package br.mackenzie.chorameliga.reconhecimento;

import br.mackenzie.chorameliga.catalogo.ClienteHttp;
import br.mackenzie.chorameliga.catalogo.FonteIndisponivelException;
import br.mackenzie.chorameliga.catalogo.RespostaHttpException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.LongConsumer;
import java.util.regex.Pattern;

/**
 * Reconhece o produto da foto com o Gemini (API do Google).
 * A chave NUNCA fica no código: vem da variável de ambiente GEMINI_API_KEY (ver AppConfig).
 * Ela vai num cabeçalho, e não na URL, para não aparecer em logs de endereço.
 */
public class GeminiReconhecedor implements ReconhecedorFoto {

    private static final Logger log = LoggerFactory.getLogger(GeminiReconhecedor.class);

    // No plano gratuito (11/2026), o 3.5-flash-lite permite 500 pedidos por dia e o 3.8-flash só 20.
    // Por isso o lite é o principal e o 3.8 fica de reserva, para quando o lite estiver sobrecarregado,
    // sem cota ou for aposentado. Versões fixas de propósito: um "-latest" muda de modelo sem aviso.
    // Para trocar sem mexer no código: GEMINI_MODELO e GEMINI_MODELO_RESERVA.
    public static final String MODELO_PADRAO = "gemini-3.5-flash-lite";
    public static final String MODELO_RESERVA_PADRAO = "gemini-3.8-flash";
    private static final String URL_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final Pattern FORMATO_MODELO = Pattern.compile("[A-Za-z0-9._-]{1,80}");
    private static final int TAMANHO_MAXIMO_NOME = 100;
    private static final String SEM_CATEGORIA = "OUTRO";

    // Categorias que o site conhece (domínios do Mercado Livre), com uma descrição para a IA escolher.
    private static final Map<String, String> CATEGORIAS = new LinkedHashMap<>();

    static {
        CATEGORIAS.put("MLB-CELLPHONES", "celulares e smartphones");
        CATEGORIAS.put("MLB-TELEVISIONS", "televisores e smart TVs");
        CATEGORIAS.put("MLB-NOTEBOOKS", "notebooks e laptops");
        CATEGORIAS.put("MLB-REFRIGERATORS", "geladeiras e refrigeradores");
        CATEGORIAS.put("MLB-GAME_CONSOLES", "consoles de videogame");
    }

    // Quando o Google responde na hora que está sobrecarregado (5xx), vale tentar de novo logo depois.
    private static final int TENTATIVAS_POR_MODELO = 2;
    private static final long PAUSA_MS = 1500;

    private final ClienteHttp http;
    private final String chave;
    private final List<String> modelos; // o principal e, se houver, a reserva
    private final LongConsumer esperar; // existe para os testes não esperarem de verdade
    private final ObjectMapper mapper = new ObjectMapper();

    // Modelo principal informado (ou o padrão) e a reserva padrão.
    public GeminiReconhecedor(ClienteHttp http, String chave, String modelo) {
        this(http, chave, modelo, MODELO_RESERVA_PADRAO);
    }

    // reserva null ou igual ao principal = sem reserva
    public GeminiReconhecedor(ClienteHttp http, String chave, String modelo, String reserva) {
        this(http, chave, modelo, reserva, GeminiReconhecedor::dormir);
    }

    GeminiReconhecedor(ClienteHttp http, String chave, String modelo, String reserva, LongConsumer esperar) {
        if (http == null) {
            throw new IllegalArgumentException("O cliente HTTP é obrigatório.");
        }
        if (chave == null || chave.isBlank()) {
            throw new IllegalArgumentException("A chave do Gemini é obrigatória.");
        }
        String principal = validarModelo((modelo == null || modelo.isBlank()) ? MODELO_PADRAO : modelo.trim());
        List<String> lista = new ArrayList<>(List.of(principal));
        if (reserva != null && !reserva.isBlank() && !reserva.trim().equals(principal)) {
            lista.add(validarModelo(reserva.trim()));
        }
        this.http = http;
        this.chave = chave.trim();
        this.modelos = List.copyOf(lista);
        this.esperar = esperar;
    }

    private static String validarModelo(String modelo) {
        if (!FORMATO_MODELO.matcher(modelo).matches()) {
            throw new IllegalArgumentException("Nome de modelo do Gemini inválido: " + modelo);
        }
        return modelo;
    }

    public String getModelo() {
        return modelos.get(0);
    }

    // null quando não há reserva
    public String getModeloReserva() {
        return modelos.size() > 1 ? modelos.get(1) : null;
    }

    @Override
    public Optional<ProdutoReconhecido> reconhecer(byte[] imagem) {
        if (imagem == null || imagem.length == 0) {
            throw new IllegalArgumentException("A foto enviada está vazia.");
        }
        FormatoImagem formato = FormatoImagem.detectar(imagem)
                .orElseThrow(() -> new IllegalArgumentException("Formato de imagem não aceito. Use JPG, PNG ou WEBP."));

        String pedido = montarPedido(imagem, formato);
        for (int i = 0; i < modelos.size(); i++) {
            String modelo = modelos.get(i);
            boolean temOutroModelo = i < modelos.size() - 1;

            for (int tentativa = 1; tentativa <= TENTATIVAS_POR_MODELO; tentativa++) {
                try {
                    String resposta = http.postJson(URL_BASE + modelo + ":generateContent",
                            pedido, Map.of("x-goog-api-key", chave));
                    return interpretar(resposta);
                } catch (RespostaHttpException e) {
                    // 400/403: chave inválida; 404: modelo aposentado; 429: limite de uso do modelo;
                    // 500/503: o Google está sobrecarregado, o que costuma passar em segundos
                    int status = e.getStatus();
                    log.warn("O Gemini ({}) respondeu com status {} (tentativa {} de {}): {}",
                            modelo, status, tentativa, TENTATIVAS_POR_MODELO, explicacaoDoErro(e.getCorpo()));
                    if (temporario(status) && tentativa < TENTATIVAS_POR_MODELO) {
                        esperar.accept(PAUSA_MS);
                        continue;
                    }
                    if (temOutroModelo && valeTrocarDeModelo(status)) {
                        log.info("Tentando o modelo reserva {}.", modelos.get(i + 1));
                        break;
                    }
                    throw new ReconhecimentoIndisponivelException("O Gemini respondeu com status " + status, e);
                } catch (FonteIndisponivelException e) {
                    // sem resposta (rede ou tempo esgotado): não repete, senão o usuário esperaria o prazo várias vezes
                    log.warn("Falha ao chamar o Gemini ({}): {}", modelo, e.getMessage());
                    throw new ReconhecimentoIndisponivelException("Falha ao chamar o Gemini.", e);
                }
            }
        }
        // só chega aqui se o último modelo pediu para trocar, o que não acontece; fica por segurança
        throw new ReconhecimentoIndisponivelException("Nenhum modelo do Gemini respondeu.", null);
    }

    // Outro modelo ajuda quando o problema é deste modelo (sobrecarga, cota ou aposentadoria),
    // mas não quando é da chave (400/401/403), que é a mesma para os dois.
    private static boolean valeTrocarDeModelo(int status) {
        return temporario(status) || status == 404 || status == 429;
    }

    private static boolean temporario(int status) {
        return status == 500 || status == 502 || status == 503 || status == 504;
    }

    private static void dormir(long milissegundos) {
        try {
            Thread.sleep(milissegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReconhecimentoIndisponivelException("Reconhecimento interrompido.", e);
        }
    }

    // Pede a resposta já em JSON, num formato fixo (responseSchema), para não depender de texto livre.
    private String montarPedido(byte[] imagem, FormatoImagem formato) {
        ObjectNode pedido = mapper.createObjectNode();

        ArrayNode partes = pedido.putArray("contents").addObject().putArray("parts");
        ObjectNode foto = partes.addObject().putObject("inlineData");
        foto.put("mimeType", formato.tipoMime());
        foto.put("data", Base64.getEncoder().encodeToString(imagem));
        partes.addObject().put("text", instrucoes());

        ObjectNode configuracao = pedido.putObject("generationConfig");
        configuracao.put("temperature", 0);
        configuracao.put("responseMimeType", "application/json");
        ObjectNode esquema = configuracao.putObject("responseSchema");
        esquema.put("type", "OBJECT");
        ObjectNode propriedades = esquema.putObject("properties");
        propriedades.putObject("reconhecido").put("type", "BOOLEAN");
        propriedades.putObject("nome").put("type", "STRING");
        ObjectNode dominio = propriedades.putObject("dominio");
        dominio.put("type", "STRING");
        ArrayNode opcoes = dominio.putArray("enum");
        CATEGORIAS.keySet().forEach(opcoes::add);
        opcoes.add(SEM_CATEGORIA);
        esquema.putArray("required").add("reconhecido").add("nome").add("dominio");

        try {
            return mapper.writeValueAsString(pedido);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível montar o pedido ao Gemini.", e);
        }
    }

    private String instrucoes() {
        StringBuilder texto = new StringBuilder()
                .append("Você ajuda um comparador de preços brasileiro a identificar o produto à venda nesta foto.\n")
                .append("- \"reconhecido\": true só se houver um produto comercial que dê para identificar; ")
                .append("false para paisagens, pessoas, animais, fotos borradas ou sem produto.\n")
                .append("- \"nome\": um termo curto para pesquisar o produto numa loja, com marca e modelo quando ")
                .append("possível (ex.: \"Galaxy S23\", \"PlayStation 5\", \"MacBook Air M2\"), sem cor, ")
                .append("sem capacidade e sem frases. Vazio se não reconheceu.\n")
                .append("- \"dominio\": a categoria do produto, escolhida desta lista:\n");
        CATEGORIAS.forEach((codigo, descricao) ->
                texto.append("  ").append(codigo).append(" = ").append(descricao).append('\n'));
        texto.append("  ").append(SEM_CATEGORIA).append(" = nenhuma das anteriores ou não reconhecido.");
        return texto.toString();
    }

    // Lê a resposta do Gemini. Sem produto, bloqueada ou vazia = não reconhecido (REQ 5);
    // fora do formato combinado = reconhecimento indisponível.
    Optional<ProdutoReconhecido> interpretar(String resposta) {
        JsonNode raiz = lerJson(resposta);

        JsonNode texto = raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (!texto.isTextual()) {
            // o Gemini às vezes recusa a imagem (ex.: filtro de segurança) e não manda candidato
            log.info("O Gemini não devolveu resposta para a foto (motivo: {}).",
                    raiz.path("promptFeedback").path("blockReason").asText("não informado"));
            return Optional.empty();
        }

        JsonNode produto = lerJson(texto.asText());
        if (!produto.path("reconhecido").asBoolean(false)) {
            return Optional.empty();
        }
        String nome = produto.path("nome").asText("").trim();
        if (nome.isEmpty()) {
            return Optional.empty();
        }
        if (nome.length() > TAMANHO_MAXIMO_NOME) {
            nome = nome.substring(0, TAMANHO_MAXIMO_NOME).trim();
        }

        // só aceita categorias da lista; qualquer outra coisa vira "sem categoria"
        String dominio = produto.path("dominio").asText("").trim();
        return Optional.of(new ProdutoReconhecido(nome, CATEGORIAS.containsKey(dominio) ? dominio : null));
    }

    private JsonNode lerJson(String json) {
        try {
            JsonNode no = mapper.readTree(json == null ? "" : json);
            if (no != null && no.isObject()) {
                return no;
            }
        } catch (JsonProcessingException e) {
            // cai no aviso abaixo
        }
        log.warn("Resposta do Gemini fora do formato esperado: {}", resumir(json));
        throw new ReconhecimentoIndisponivelException("Resposta do Gemini fora do formato esperado.", null);
    }

    // O Google explica o erro em {"error":{"message":...}}; a resposta de erro não contém a chave.
    private String explicacaoDoErro(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return "(sem detalhes)";
        }
        try {
            String mensagem = mapper.readTree(corpo).path("error").path("message").asText("");
            if (!mensagem.isBlank()) {
                return resumir(mensagem);
            }
        } catch (JsonProcessingException e) {
            // não era JSON: mostra o começo do texto
        }
        return resumir(corpo);
    }

    private static String resumir(String texto) {
        if (texto == null) {
            return "(vazio)";
        }
        String umaLinha = texto.replaceAll("\\s+", " ").trim();
        return umaLinha.length() > 300 ? umaLinha.substring(0, 300) + "..." : umaLinha;
    }
}
