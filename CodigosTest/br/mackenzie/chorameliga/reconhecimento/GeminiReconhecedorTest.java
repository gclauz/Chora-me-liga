package br.mackenzie.chorameliga.reconhecimento;

import br.mackenzie.chorameliga.catalogo.ClienteHttp;
import br.mackenzie.chorameliga.catalogo.ClienteHttpFake;
import br.mackenzie.chorameliga.catalogo.RespostaHttpException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GeminiReconhecedorTest {

    private static final byte[] FOTO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    private static final String URL = "generativelanguage.googleapis.com";

    private ClienteHttpFake http;
    private GeminiReconhecedor gemini;
    private List<Long> esperas; // pausas entre tentativas, registradas em vez de esperadas de verdade

    @BeforeEach
    void setUp() {
        http = new ClienteHttpFake();
        esperas = new ArrayList<>();
        gemini = new GeminiReconhecedor(http, "CHAVE-TESTE", null, null, esperas::add);
    }

    // Cliente HTTP que devolve, em ordem, um status de erro (número) ou uma resposta (texto)
    private static ClienteHttp emSequencia(List<Object> passos, List<String> chamadas) {
        Deque<Object> fila = new ArrayDeque<>(passos);
        return new ClienteHttp() {
            @Override
            public String get(String url, String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String postForm(String url, String corpo) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String postJson(String url, String corpoJson, Map<String, String> cabecalhos) {
                chamadas.add(url);
                Object passo = fila.poll();
                if (passo instanceof Integer status) {
                    throw new RespostaHttpException(status, "{\"error\":{\"message\":\"simulado\"}}");
                }
                return (String) passo;
            }
        };
    }

    // Monta uma resposta do Gemini com o JSON do produto dentro do texto, como a API real faz
    private static String respostaGemini(String jsonProduto) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var raiz = mapper.createObjectNode();
        raiz.putArray("candidates").addObject()
                .putObject("content").putArray("parts").addObject().put("text", jsonProduto);
        return mapper.writeValueAsString(raiz);
    }

    @Test
    void testReconheceProdutoComCategoria() throws Exception {
        http.responder(URL, respostaGemini(
                "{\"reconhecido\":true,\"nome\":\"Galaxy S23\",\"dominio\":\"MLB-CELLPHONES\"}"));

        Optional<ProdutoReconhecido> resultado = gemini.reconhecer(FOTO_JPEG);

        // Caso 1: devolve o nome e a categoria
        assertTrue(resultado.isPresent());
        assertEquals("Galaxy S23", resultado.get().nome());
        assertEquals("MLB-CELLPHONES", resultado.get().dominio());
    }

    @Test
    void testPedidoEnviado() throws Exception {
        http.responder(URL, respostaGemini("{\"reconhecido\":false,\"nome\":\"\",\"dominio\":\"OUTRO\"}"));
        gemini.reconhecer(FOTO_JPEG);

        // Caso 1: chama o generateContent do modelo padrão
        assertEquals("POST https://generativelanguage.googleapis.com/v1beta/models/"
                + GeminiReconhecedor.MODELO_PADRAO + ":generateContent", http.chamadas.get(0));

        // Caso 2: a chave vai no cabeçalho e nunca na URL
        assertEquals("CHAVE-TESTE", http.ultimosCabecalhos.get("x-goog-api-key"));
        assertFalse(http.chamadas.get(0).contains("CHAVE-TESTE"));

        // Caso 3: a foto vai em base64 com o tipo detectado, e a resposta é pedida em JSON
        JsonNode pedido = new ObjectMapper().readTree(http.ultimoCorpo);
        JsonNode foto = pedido.path("contents").path(0).path("parts").path(0).path("inlineData");
        assertEquals("image/jpeg", foto.path("mimeType").asText());
        assertEquals(Base64.getEncoder().encodeToString(FOTO_JPEG), foto.path("data").asText());
        assertEquals("application/json", pedido.path("generationConfig").path("responseMimeType").asText());
    }

    @Test
    void testModeloConfiguravel() throws Exception {
        GeminiReconhecedor outro = new GeminiReconhecedor(http, "CHAVE-TESTE", " gemini-outro-modelo ");
        http.responder(URL, respostaGemini("{\"reconhecido\":false,\"nome\":\"\",\"dominio\":\"OUTRO\"}"));
        outro.reconhecer(FOTO_JPEG);

        // Caso 1: usa o modelo informado, sem espaços
        assertEquals("gemini-outro-modelo", outro.getModelo());
        assertTrue(http.chamadas.get(0).contains("/models/gemini-outro-modelo:generateContent"));
    }

    @Test
    void testSemCategoriaConhecida() throws Exception {
        // Caso 1: "OUTRO" vira categoria nula
        http.responder(URL, respostaGemini("{\"reconhecido\":true,\"nome\":\"Air Fryer Mondial\",\"dominio\":\"OUTRO\"}"));
        assertNull(gemini.reconhecer(FOTO_JPEG).orElseThrow().dominio());

        // Caso 2: categoria fora da lista também vira nula
        assertNull(gemini.interpretar(respostaGemini(
                "{\"reconhecido\":true,\"nome\":\"Air Fryer\",\"dominio\":\"MLB-INVENTADO\"}")).orElseThrow().dominio());
    }

    @Test
    void testNaoReconhecido() throws Exception {
        // Caso 1: o Gemini diz que não há produto (REQ 5)
        assertTrue(gemini.interpretar(respostaGemini(
                "{\"reconhecido\":false,\"nome\":\"\",\"dominio\":\"OUTRO\"}")).isEmpty());

        // Caso 2: diz que reconheceu, mas sem nome
        assertTrue(gemini.interpretar(respostaGemini(
                "{\"reconhecido\":true,\"nome\":\"  \",\"dominio\":\"MLB-CELLPHONES\"}")).isEmpty());

        // Caso 3: a imagem foi bloqueada e não veio candidato
        assertTrue(gemini.interpretar("{\"promptFeedback\":{\"blockReason\":\"SAFETY\"}}").isEmpty());
    }

    @Test
    void testNomeMuitoLongoECortado() throws Exception {
        String longo = "Produto ".repeat(30);
        ProdutoReconhecido p = gemini.interpretar(respostaGemini(
                "{\"reconhecido\":true,\"nome\":\"" + longo + "\",\"dominio\":\"OUTRO\"}")).orElseThrow();

        // Caso 1: o nome usado na pesquisa tem no máximo 100 caracteres
        assertTrue(p.nome().length() <= 100);
    }

    @Test
    void testRespostaForaDoFormato() throws Exception {
        // Caso 1: a resposta não é JSON
        assertThrows(ReconhecimentoIndisponivelException.class, () -> gemini.interpretar("<html>erro</html>"));

        // Caso 2: o texto do candidato não é JSON
        assertThrows(ReconhecimentoIndisponivelException.class,
                () -> gemini.interpretar(respostaGemini("Isto é um Galaxy S23")));
    }

    @Test
    void testGeminiIndisponivel() {
        // Caso 1: chave inválida, limite de uso ou erro do Google (status de erro)
        http.responderComStatus(URL, 429);
        assertThrows(ReconhecimentoIndisponivelException.class, () -> gemini.reconhecer(FOTO_JPEG));

        // Caso 2: 429 (limite de uso) não adianta repetir: uma chamada só
        assertEquals(1, http.chamadas.size());
        assertTrue(esperas.isEmpty());

        // Caso 3: falha de rede ou tempo esgotado desiste na hora, sem somar esperas
        ClienteHttpFake semRede = new ClienteHttpFake();
        semRede.falharEm(URL);
        GeminiReconhecedor semConexao = new GeminiReconhecedor(semRede, "CHAVE-TESTE", null, null, esperas::add);
        assertThrows(ReconhecimentoIndisponivelException.class, () -> semConexao.reconhecer(FOTO_JPEG));
        assertEquals(1, semRede.chamadas.size());
        assertTrue(esperas.isEmpty());
    }

    @Test
    void testSobrecargaTemporariaTentaDeNovo() throws Exception {
        // Caso 1: 503 e depois sucesso: reconhece na segunda tentativa, após uma pausa
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor comSobrecarga = new GeminiReconhecedor(emSequencia(List.of(503, respostaGemini(
                "{\"reconhecido\":true,\"nome\":\"PlayStation 5\",\"dominio\":\"MLB-GAME_CONSOLES\"}")), chamadas),
                "CHAVE-TESTE", null, null, esperas::add);

        assertEquals("PlayStation 5", comSobrecarga.reconhecer(FOTO_JPEG).orElseThrow().nome());
        assertEquals(2, chamadas.size());
        assertEquals(List.of(1500L), esperas);
    }

    @Test
    void testSobrecargaQueNaoPassaDesiste() {
        // Caso 1: sem reserva, 503 nas 2 tentativas: desiste, com uma pausa de 1,5 s entre elas
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor sempreSobrecarregado = new GeminiReconhecedor(
                emSequencia(List.of(503, 503), chamadas), "CHAVE-TESTE", null, null, esperas::add);

        assertThrows(ReconhecimentoIndisponivelException.class, () -> sempreSobrecarregado.reconhecer(FOTO_JPEG));
        assertEquals(2, chamadas.size());
        assertEquals(List.of(1500L), esperas);
    }

    @Test
    void testModeloInexistenteSemReservaDesiste() {
        // Caso 1: 404 (modelo aposentado) sem reserva: desiste na hora
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor modeloVelho = new GeminiReconhecedor(
                emSequencia(List.of(404), chamadas), "CHAVE-TESTE", "modelo-velho", null, esperas::add);

        assertThrows(ReconhecimentoIndisponivelException.class, () -> modeloVelho.reconhecer(FOTO_JPEG));
        assertEquals(1, chamadas.size());
    }

    private static final String PS5 = "{\"reconhecido\":true,\"nome\":\"PlayStation 5\",\"dominio\":\"MLB-GAME_CONSOLES\"}";

    @Test
    void testReservaQuandoPrincipalSobrecarregado() throws Exception {
        // Caso 1: o principal dá 503 duas vezes; a reserva responde
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor comReserva = new GeminiReconhecedor(
                emSequencia(List.of(503, 503, respostaGemini(PS5)), chamadas),
                "CHAVE-TESTE", "principal", "reserva", esperas::add);

        assertEquals("PlayStation 5", comReserva.reconhecer(FOTO_JPEG).orElseThrow().nome());
        assertEquals(3, chamadas.size());
        assertTrue(chamadas.get(0).contains("/models/principal:"));
        assertTrue(chamadas.get(2).contains("/models/reserva:"));
        // Caso 2: só pausa entre as tentativas do mesmo modelo
        assertEquals(List.of(1500L), esperas);
    }

    @Test
    void testReservaQuandoPrincipalAposentadoOuSemCota() throws Exception {
        // Caso 1: 404 no principal vai direto para a reserva, sem pausa
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor aposentado = new GeminiReconhecedor(
                emSequencia(List.of(404, respostaGemini(PS5)), chamadas),
                "CHAVE-TESTE", "principal", "reserva", esperas::add);
        assertTrue(aposentado.reconhecer(FOTO_JPEG).isPresent());
        assertEquals(2, chamadas.size());

        // Caso 2: 429 (cota do modelo) também tenta a reserva
        List<String> outras = new ArrayList<>();
        GeminiReconhecedor semCota = new GeminiReconhecedor(
                emSequencia(List.of(429, respostaGemini(PS5)), outras),
                "CHAVE-TESTE", "principal", "reserva", esperas::add);
        assertTrue(semCota.reconhecer(FOTO_JPEG).isPresent());
        assertTrue(esperas.isEmpty());
    }

    @Test
    void testChaveInvalidaNaoTentaReserva() {
        // Caso 1: 403 é problema da chave, que é a mesma para a reserva: desiste na hora
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor chaveRuim = new GeminiReconhecedor(
                emSequencia(List.of(403), chamadas), "CHAVE-TESTE", "principal", "reserva", esperas::add);

        assertThrows(ReconhecimentoIndisponivelException.class, () -> chaveRuim.reconhecer(FOTO_JPEG));
        assertEquals(1, chamadas.size());
    }

    @Test
    void testReservaTambemSobrecarregada() {
        // Caso 1: os dois modelos dão 503 nas duas tentativas: 4 chamadas e desiste
        List<String> chamadas = new ArrayList<>();
        GeminiReconhecedor tudoCheio = new GeminiReconhecedor(
                emSequencia(List.of(503, 503, 503, 503), chamadas), "CHAVE-TESTE", "principal", "reserva", esperas::add);

        assertThrows(ReconhecimentoIndisponivelException.class, () -> tudoCheio.reconhecer(FOTO_JPEG));
        assertEquals(4, chamadas.size());
        assertEquals(List.of(1500L, 1500L), esperas);
    }

    @Test
    void testConfiguracaoDaReserva() {
        // Caso 1: o construtor público usa a reserva padrão
        assertEquals(GeminiReconhecedor.MODELO_RESERVA_PADRAO,
                new GeminiReconhecedor(http, "CHAVE", null).getModeloReserva());

        // Caso 2: reserva nula, em branco ou igual ao principal = sem reserva
        assertNull(new GeminiReconhecedor(http, "CHAVE", "m1", (String) null).getModeloReserva());
        assertNull(new GeminiReconhecedor(http, "CHAVE", "m1", " ").getModeloReserva());
        assertNull(new GeminiReconhecedor(http, "CHAVE", "m1", "m1").getModeloReserva());

        // Caso 3: nome de reserva que mudaria a URL é recusado
        assertThrows(IllegalArgumentException.class,
                () -> new GeminiReconhecedor(http, "CHAVE", "m1", "../outro"));
    }

    @Test
    void testFotoInvalidaNemChamaAApi() {
        // Caso 1: foto vazia ou que não é imagem é recusada antes de gastar uma chamada
        assertThrows(IllegalArgumentException.class, () -> gemini.reconhecer(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> gemini.reconhecer("texto".getBytes()));
        assertTrue(http.chamadas.isEmpty());
    }

    @Test
    void testConfiguracaoInvalida() {
        // Caso 1: sem chave
        assertThrows(IllegalArgumentException.class, () -> new GeminiReconhecedor(http, " ", null));
        assertThrows(IllegalArgumentException.class, () -> new GeminiReconhecedor(http, null, null));

        // Caso 2: sem cliente HTTP
        assertThrows(IllegalArgumentException.class, () -> new GeminiReconhecedor(null, "CHAVE", null));

        // Caso 3: nome de modelo que mudaria a URL
        assertThrows(IllegalArgumentException.class,
                () -> new GeminiReconhecedor(http, "CHAVE", "modelo/../outro?x=1"));
    }
}
