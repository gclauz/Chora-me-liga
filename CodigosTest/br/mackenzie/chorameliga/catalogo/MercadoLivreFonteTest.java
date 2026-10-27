package br.mackenzie.chorameliga.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MercadoLivreFonteTest {

    private static final String TOKEN_JSON = "{\"access_token\":\"TOKEN-TESTE\",\"expires_in\":21600}";

    private ClienteHttpFake http;
    private MercadoLivreAuth auth;
    private MercadoLivreFonte fonte;

    @BeforeEach
    void setUp() {
        http = new ClienteHttpFake();
        http.responder("/oauth/token", TOKEN_JSON);
        auth = new MercadoLivreAuth(http, "id-teste", "segredo-teste");
        fonte = new MercadoLivreFonte(http, auth);
    }

    private String chamadaCom(String trecho) {
        return http.chamadas.stream().filter(c -> c.contains(trecho)).findFirst().orElse("");
    }

    private long contarChamadas(String trecho) {
        return http.chamadas.stream().filter(c -> c.contains(trecho)).count();
    }

    @Test
    void testPesquisarProdutos() {
        http.responder("/products/search", """
                {"results":[
                  {"id":"MLB39957733","domain_id":"MLB-CELLPHONES","name":"Smartphone Samsung Galaxy S23 256gb Preto"},
                  {"id":"MLB47311354","domain_id":"MLB-CELLPHONES","name":"Samsung Galaxy S23 256gb Violeta Cor Lavanda"}]}
                """);

        List<ProdutoExterno> produtos = fonte.pesquisarProdutos("galaxy s23");

        // Caso 1: converte os produtos da resposta
        assertEquals(2, produtos.size());
        assertEquals("MLB39957733", produtos.get(0).idExterno());
        assertEquals("Smartphone Samsung Galaxy S23 256gb Preto", produtos.get(0).nome());
        assertEquals("MLB-CELLPHONES", produtos.get(0).categoria());

        // Caso 2: o texto vai codificado e sem filtro de domínio
        String chamada = chamadaCom("/products/search");
        assertTrue(chamada.contains("q=galaxy%20s23"));
        assertTrue(chamada.contains("status=active"));
        assertTrue(chamada.contains("site_id=MLB"));
        assertFalse(chamada.contains("domain_id="));

        // Caso 3: a chamada leva o token
        assertEquals("TOKEN-TESTE", http.ultimoToken);
    }

    @Test
    void testPesquisaComDominio() {
        http.responder("/products/search", "{\"results\":[]}");
        MercadoLivreFonte comDominio = new MercadoLivreFonte(http, auth, "MLB-CELLPHONES");

        // Caso 1: o filtro de domínio vai na pesquisa
        comDominio.pesquisarProdutos("galaxy");
        assertTrue(chamadaCom("/products/search").contains("domain_id=MLB-CELLPHONES"));
    }

    @Test
    void testPesquisaIgnoraResultadosIncompletos() {
        http.responder("/products/search", """
                {"results":[
                  {"id":"","name":"Sem id"},
                  {"id":"MLB5","name":""},
                  {"name":"Sem campo id"},
                  {"id":"MLB6","domain_id":"MLB-CELLPHONES","name":"Produto Completo"}]}
                """);

        List<ProdutoExterno> produtos = fonte.pesquisarProdutos("produto");

        // Caso 1: só o resultado com id e nome entra
        assertEquals(1, produtos.size());
        assertEquals("MLB6", produtos.get(0).idExterno());
    }

    @Test
    void testPesquisaVazia() {
        // Caso 1: texto vazio ou nulo não chama a API
        assertTrue(fonte.pesquisarProdutos("  ").isEmpty());
        assertTrue(fonte.pesquisarProdutos(null).isEmpty());
        assertTrue(http.chamadas.isEmpty());
    }

    @Test
    void testBuscarOfertas() {
        http.responder("/products/MLB39957733/items", """
                {"results":[
                  {"item_id":"MLB1","seller_id":111,"price":4299,"shipping":{"free_shipping":true,"cost":0}},
                  {"item_id":"MLB2","seller_id":222,"price":3499,"shipping":{"free_shipping":true,"cost":0}},
                  {"item_id":"MLB3","seller_id":333,"price":3399.9,"shipping":{"free_shipping":false,"cost":25.5}},
                  {"item_id":"MLB4","seller_id":222,"price":3600,"shipping":{"free_shipping":true,"cost":0}}]}
                """);
        http.responder("/users/111", "{\"nickname\":\"LOJA_A\"}");
        http.responder("/users/222", "{\"nickname\":\"LOJA_B\"}");
        http.responder("/users/333", "{\"nickname\":\"LOJA_C\"}");

        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB39957733");

        // Caso 1: um vendedor repetido fica só com a oferta mais barata (3 ofertas, não 4)
        assertEquals(3, ofertas.size());

        // Caso 2: ordenadas pelo preço total (preço + frete): C (3425,40), B (3499) e A (4299)
        assertEquals("LOJA_C", ofertas.get(0).loja());
        assertEquals(3399.90, ofertas.get(0).preco(), 0.01);
        assertEquals(25.50, ofertas.get(0).frete(), 0.01);
        assertEquals("LOJA_B", ofertas.get(1).loja());
        assertEquals(3499.00, ofertas.get(1).preco(), 0.01);
        assertEquals("LOJA_A", ofertas.get(2).loja());
    }

    @Test
    void testLimitaAs5OfertasMaisBaratas() {
        StringBuilder json = new StringBuilder("{\"results\":[");
        for (int i = 7; i >= 1; i--) {
            json.append("{\"seller_id\":").append(i)
                    .append(",\"price\":").append(i * 100)
                    .append(",\"shipping\":{\"free_shipping\":true,\"cost\":0}}");
            if (i > 1) {
                json.append(",");
            }
        }
        json.append("]}");
        http.responder("/products/MLB9/items", json.toString());
        http.responder("/users/", "{\"nickname\":\"LOJA\"}");

        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB9");

        // Caso 1: de 7 ofertas ficam as 5 mais baratas, em ordem
        assertEquals(5, ofertas.size());
        assertEquals(100.0, ofertas.get(0).preco(), 0.01);
        assertEquals(500.0, ofertas.get(4).preco(), 0.01);

        // Caso 2: só consultou o nome dos 5 vendedores que ficaram
        assertEquals(5, contarChamadas("/users/"));
    }

    @Test
    void testIgnoraPrecoInvalido() {
        http.responder("/products/MLB9/items", """
                {"results":[
                  {"seller_id":111,"price":0,"shipping":{"cost":0}},
                  {"seller_id":222,"shipping":{"cost":0}},
                  {"price":500,"shipping":{"cost":0}},
                  {"seller_id":333,"price":900,"shipping":{"cost":0}}]}
                """);
        http.responder("/users/333", "{\"nickname\":\"LOJA_C\"}");

        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB9");

        // Caso 1: sem preço, com preço zero ou sem vendedor, a oferta é descartada
        assertEquals(1, ofertas.size());
        assertEquals("LOJA_C", ofertas.get(0).loja());
        assertEquals(900.0, ofertas.get(0).preco(), 0.01);
    }

    @Test
    void testFreteGratisZeraCusto() {
        http.responder("/products/MLB9/items", """
                {"results":[
                  {"seller_id":111,"price":100,"shipping":{"free_shipping":true,"cost":10}},
                  {"seller_id":222,"price":200,"shipping":{"free_shipping":false,"cost":10}},
                  {"seller_id":333,"price":300}]}
                """);
        http.responder("/users/", "{\"nickname\":\"LOJA\"}");

        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB9");

        // Caso 1: frete grátis vale 0, mesmo que o campo de custo traga outro valor
        assertEquals(0.0, ofertas.get(0).frete(), 0.01);

        // Caso 2: sem frete grátis, usa o custo informado
        assertEquals(10.0, ofertas.get(1).frete(), 0.01);

        // Caso 3: sem informação de envio, o frete é 0
        assertEquals(0.0, ofertas.get(2).frete(), 0.01);
    }

    @Test
    void testReaproveitaNomeDoVendedor() {
        http.responder("/products/MLB9/items",
                "{\"results\":[{\"seller_id\":111,\"price\":100,\"shipping\":{\"cost\":0}}]}");
        http.responder("/users/111", "{\"nickname\":\"LOJA_A\"}");

        // Caso 1: a segunda busca usa o nome guardado, sem chamar /users de novo
        assertEquals("LOJA_A", fonte.buscarOfertas("MLB9").get(0).loja());
        assertEquals("LOJA_A", fonte.buscarOfertas("MLB9").get(0).loja());
        assertEquals(1, contarChamadas("/users/"));
    }

    @Test
    void testVendedorIndisponivel() {
        http.responder("/products/MLB9/items",
                "{\"results\":[{\"seller_id\":111,\"price\":100,\"shipping\":{\"cost\":0}}]}");
        http.falharEm("/users/111");

        // Caso 1: sem o nome, usa um nome genérico
        assertEquals("Vendedor 111", fonte.buscarOfertas("MLB9").get(0).loja());

        // Caso 2: o nome genérico não é guardado, então tenta de novo na próxima vez
        fonte.buscarOfertas("MLB9");
        assertEquals(2, contarChamadas("/users/"));
    }

    @Test
    void testRespostaInvalida() {
        http.responder("/products/search", "isso não é json");
        http.responder("/products/MLB9/items", "{{");

        // Caso 1: pesquisa com resposta que não é JSON
        assertThrows(FonteIndisponivelException.class, () -> fonte.pesquisarProdutos("galaxy"));

        // Caso 2: ofertas com resposta que não é JSON
        assertThrows(FonteIndisponivelException.class, () -> fonte.buscarOfertas("MLB9"));
    }

    @Test
    void testFonteForaDoAr() {
        http.falharEm("/products/search");
        http.falharEm("/products/MLB9/items");

        // Caso 1: pesquisa com a API fora do ar
        assertThrows(FonteIndisponivelException.class, () -> fonte.pesquisarProdutos("galaxy"));

        // Caso 2: ofertas com a API fora do ar
        assertThrows(FonteIndisponivelException.class, () -> fonte.buscarOfertas("MLB9"));
    }

    @Test
    void testOfertasComIdInvalido() {
        // Caso 1: id nulo ou em branco não chama a API
        assertTrue(fonte.buscarOfertas(null).isEmpty());
        assertTrue(fonte.buscarOfertas("  ").isEmpty());
        assertTrue(http.chamadas.isEmpty());
    }

    @Test
    void testConstrutorInvalido() {
        // Caso 1: cliente HTTP nulo
        assertThrows(IllegalArgumentException.class, () -> new MercadoLivreFonte(null, auth));

        // Caso 2: autenticação nula
        assertThrows(IllegalArgumentException.class, () -> new MercadoLivreFonte(http, null));
    }

    @Test
    void testProdutoSemOfertasDevolveListaVazia() {
        http.responderComStatus("/products/MLB9/items", 404);

        // Caso 1: 404 significa produto sem ofertas, e não é falha da fonte
        assertTrue(fonte.buscarOfertas("MLB9").isEmpty());
    }

    @Test
    void testErroDeServidorContinuaSendoFalha() {
        http.responderComStatus("/products/MLB9/items", 500);

        // Caso 1: outros status de erro continuam sendo falha da fonte
        assertThrows(FonteIndisponivelException.class, () -> fonte.buscarOfertas("MLB9"));
    }

    @Test
    void testLinkDaOferta() {
        http.responder("/products/MLB9/items", """
                {"results":[
                  {"item_id":"MLB7329366394","seller_id":111,"price":100,"shipping":{"cost":0}},
                  {"item_id":"MLBU123","seller_id":222,"price":200,"shipping":{"cost":0}},
                  {"seller_id":333,"price":300,"shipping":{"cost":0}}]}
                """);
        http.responder("/users/", "{\"nickname\":\"LOJA\"}");

        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB9");

        // Caso 1: item_id no formato MLB + números vira o link da oferta
        assertEquals("https://produto.mercadolivre.com.br/MLB-7329366394", ofertas.get(0).url());

        // Caso 2: id em outro formato não vira link
        assertNull(ofertas.get(1).url());

        // Caso 3: sem item_id, sem link
        assertNull(ofertas.get(2).url());
    }

}