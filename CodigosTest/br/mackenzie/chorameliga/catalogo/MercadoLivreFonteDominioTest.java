package br.mackenzie.chorameliga.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// Confere que o domínio pedido chega na chamada à API do Mercado Livre.
class MercadoLivreFonteDominioTest {

    private ClienteHttpFake http;
    private MercadoLivreAuth auth;

    @BeforeEach
    void setUp() {
        http = new ClienteHttpFake();
        http.responder("/oauth/token", "{\"access_token\":\"TOKEN-TESTE\",\"expires_in\":21600}");
        http.responder("/products/search", "{\"results\":[]}");
        auth = new MercadoLivreAuth(http, "id-teste", "segredo-teste");
    }

    private String chamadaDePesquisa() {
        return http.chamadas.stream().filter(c -> c.contains("/products/search")).findFirst().orElse("");
    }

    @Test
    void testDominioPedidoVaiNaPesquisa() {
        MercadoLivreFonte fonte = new MercadoLivreFonte(http, auth);

        fonte.pesquisarProdutos("notebook", "MLB-NOTEBOOKS");

        // Caso 1: o domínio pedido vai como domain_id
        assertTrue(chamadaDePesquisa().contains("domain_id=MLB-NOTEBOOKS"));
        assertTrue(chamadaDePesquisa().contains("q=notebook"));
    }

    @Test
    void testDominioPedidoSubstituiODaConfiguracao() {
        MercadoLivreFonte fonte = new MercadoLivreFonte(http, auth, "MLB-CELLPHONES");

        fonte.pesquisarProdutos("notebook", "MLB-NOTEBOOKS");

        // Caso 1: vale o domínio do pedido, e não o da configuração
        assertTrue(chamadaDePesquisa().contains("domain_id=MLB-NOTEBOOKS"));
        assertFalse(chamadaDePesquisa().contains("MLB-CELLPHONES"));
    }

    @Test
    void testSemDominioPedidoUsaODaConfiguracao() {
        MercadoLivreFonte fonte = new MercadoLivreFonte(http, auth, "MLB-CELLPHONES");

        // Caso 1: sem domínio no pedido, usa o da configuração
        fonte.pesquisarProdutos("galaxy", null);
        assertTrue(chamadaDePesquisa().contains("domain_id=MLB-CELLPHONES"));
    }

    @Test
    void testSemNenhumDominioNaoFiltra() {
        MercadoLivreFonte fonte = new MercadoLivreFonte(http, auth);

        // Caso 1: nem no pedido nem na configuração: a chamada não leva domain_id
        fonte.pesquisarProdutos("galaxy", "  ");
        assertFalse(chamadaDePesquisa().contains("domain_id="));
    }
}