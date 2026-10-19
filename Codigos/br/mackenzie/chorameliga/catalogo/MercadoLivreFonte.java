package br.mackenzie.chorameliga.catalogo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fonte real: consulta o catálogo do Mercado Livre.
 * A busca geral (/sites/MLB/search) e o detalhe de item (/items) estão bloqueados (403)
 * para a nossa aplicação, então usamos o catálogo de produtos.
 */
public class MercadoLivreFonte implements FonteCatalogo {

    private static final String BASE = "https://api.mercadolibre.com";
    private static final int MAX_PRODUTOS = 5;
    private static final int MAX_OFERTAS = 5;

    private final ClienteHttp http;
    private final MercadoLivreAuth auth;
    private final String dominio; // ex.: MLB-CELLPHONES; null = sem filtro
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, String> cacheVendedores = new ConcurrentHashMap<>();

    public MercadoLivreFonte(ClienteHttp http, MercadoLivreAuth auth) {
        this(http, auth, null);
    }

    public MercadoLivreFonte(ClienteHttp http, MercadoLivreAuth auth, String dominio) {
        if (http == null || auth == null) {
            throw new IllegalArgumentException("Cliente HTTP e autenticação são obrigatórios.");
        }
        this.http = http;
        this.auth = auth;
        this.dominio = (dominio == null || dominio.isBlank()) ? null : dominio.trim();
    }

    @Override
    public List<ProdutoExterno> pesquisarProdutos(String nome) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }
        String url = BASE + "/products/search?status=active&site_id=MLB&limit=" + MAX_PRODUTOS
                + "&q=" + codificar(nome.trim());
        if (dominio != null) {
            url += "&domain_id=" + codificar(dominio);
        }

        String resposta;
        try {
            resposta = http.get(url, auth.getToken());
        } catch (RespostaHttpException e) {
            if (e.getStatus() == 404) {
                return List.of(); // produto do catálogo sem ofertas ativas
            }
            throw e;
        }
        JsonNode json = lerJson(resposta);
        List<ProdutoExterno> produtos = new ArrayList<>();
        for (JsonNode item : json.path("results")) {
            String id = item.path("id").asText("");
            String nomeProduto = item.path("name").asText("");
            if (id.isBlank() || nomeProduto.isBlank()) {
                continue;
            }
            produtos.add(new ProdutoExterno(id, nomeProduto, item.path("domain_id").asText(null)));
        }
        return produtos;
    }

    @Override
    public List<OfertaExterna> buscarOfertas(String idExterno) {
        if (idExterno == null || idExterno.isBlank()) {
            return List.of();
        }
        String url = BASE + "/products/" + codificar(idExterno.trim()) + "/items";

        String resposta;
        try {
            resposta = http.get(url, auth.getToken());
        } catch (RespostaHttpException e) {
            if (e.getStatus() == 404) {
                return List.of(); // produto do catálogo sem ofertas ativas
            }
            throw e;
        }
        JsonNode json = lerJson(resposta);

        List<OfertaBruta> brutas = new ArrayList<>();
        for (JsonNode item : json.path("results")) {
            double preco = item.path("price").asDouble(0);
            String vendedor = item.path("seller_id").asText("");
            if (preco <= 0 || vendedor.isBlank()) {
                continue;
            }
            JsonNode envio = item.path("shipping");
            double frete = envio.path("free_shipping").asBoolean(false)
                    ? 0.0
                    : envio.path("cost").asDouble(0.0);
            brutas.add(new OfertaBruta(vendedor, preco, frete));
        }

        // as mais baratas primeiro (considerando o frete)
        brutas.sort(Comparator.comparingDouble(o -> o.preco() + o.frete()));

        List<OfertaExterna> ofertas = new ArrayList<>();
        Set<String> vendedoresVistos = new HashSet<>();
        for (OfertaBruta o : brutas) {
            if (ofertas.size() >= MAX_OFERTAS) {
                break;
            }
            if (!vendedoresVistos.add(o.vendedor())) {
                continue; // mesmo vendedor: já ficou a oferta mais barata dele
            }
            ofertas.add(new OfertaExterna(nomeDoVendedor(o.vendedor()), o.preco(), o.frete()));
        }
        return ofertas;
    }

    private String nomeDoVendedor(String vendedorId) {
        String guardado = cacheVendedores.get(vendedorId);
        if (guardado != null) {
            return guardado;
        }
        try {
            JsonNode json = lerJson(http.get(BASE + "/users/" + codificar(vendedorId), auth.getToken()));
            String nickname = json.path("nickname").asText("");
            if (!nickname.isBlank()) {
                cacheVendedores.put(vendedorId, nickname);
                return nickname;
            }
        } catch (FonteIndisponivelException e) {
            // cai no nome genérico, sem guardar, para tentar de novo na próxima vez
        }
        return "Vendedor " + vendedorId;
    }

    private JsonNode lerJson(String texto) {
        try {
            return mapper.readTree(texto);
        } catch (JsonProcessingException e) {
            throw new FonteIndisponivelException("Resposta da API em formato inválido.", e);
        }
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record OfertaBruta(String vendedor, double preco, double frete) {
    }
}