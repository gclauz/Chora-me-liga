package br.mackenzie.chorameliga.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoTest {

    private Produto galaxy;

    @BeforeEach
    void setUp() {
        galaxy = new Produto("Smartphone Samsung Galaxy S23 128GB", "Smartphones",
                "Tela 6.1 AMOLED, Snapdragon 8 Gen 2");
    }

    @Test
    void testConstrutorComNomeInvalido() {
        // Caso 1: nome vazio
        assertThrows(IllegalArgumentException.class, () -> new Produto("", "Smartphones", "desc"));
        // Caso 2: nome nulo
        assertThrows(IllegalArgumentException.class, () -> new Produto(null, "Smartphones", "desc"));
    }

    @Test
    void testAdicionarOferta() {
        // Caso 1: produto novo não tem ofertas
        assertEquals(0, galaxy.getQuantidadeOfertas());

        // Caso 2: adiciona duas ofertas
        galaxy.adicionarOferta(new Oferta("Amazon Brasil", 2899.00, 0.00));
        galaxy.adicionarOferta(new Oferta("Magazine Luiza", 2949.00, 15.00));
        assertEquals(2, galaxy.getQuantidadeOfertas());

        // Caso 3: oferta nula é rejeitada
        assertThrows(IllegalArgumentException.class, () -> galaxy.adicionarOferta(null));
        assertEquals(2, galaxy.getQuantidadeOfertas());
    }

    @Test
    void testGetOfertaMaisBarata() {
        // Caso 1: sem ofertas
        assertTrue(galaxy.getOfertaMaisBarata().isEmpty());

        // Caso 2: a Amazon (total 2899) vence a Magalu (total 2964)
        galaxy.adicionarOferta(new Oferta("Magazine Luiza", 2949.00, 15.00));
        galaxy.adicionarOferta(new Oferta("Amazon Brasil", 2899.00, 0.00));
        assertEquals("Amazon Brasil", galaxy.getOfertaMaisBarata().get().getLoja());

        // Caso 3: preço menor, mas o frete deixa a Fast Shop mais cara (total 2912)
        galaxy.adicionarOferta(new Oferta("Fast Shop", 2890.00, 22.00));
        assertEquals("Amazon Brasil", galaxy.getOfertaMaisBarata().get().getLoja());
    }

    @Test
    void testListaDeOfertasImutavel() {
        Oferta amazon = new Oferta("Amazon Brasil", 2899.00, 0.00);
        // Caso 1: tentar adicionar direto na lista devolvida
        assertThrows(UnsupportedOperationException.class, () -> galaxy.getOfertas().add(amazon));
    }

    @Test   
    void testAplicarPrecoLojaNova() {
        // Caso 1: loja que ainda não tem oferta cria uma nova
        galaxy.aplicarPreco("Amazon Brasil", 2899.00, 0.00, "https://amazon.com.br/galaxy");

        // Caso 2: a oferta criada tem o preço informado
        assertEquals(1, galaxy.getQuantidadeOfertas());
        assertEquals(2899.00, galaxy.getOfertas().get(0).getPreco(), 0.01);
    }

    @Test
    void testAplicarPrecoLojaExistente() {
        galaxy.aplicarPreco("Amazon Brasil", 2899.00, 0.00, null);

        // Caso 1: preço diferente atualiza e não duplica a oferta
        galaxy.aplicarPreco("amazon brasil", 2799.00, 0.00, null);
        assertEquals(1, galaxy.getQuantidadeOfertas());
        assertEquals(2799.00, galaxy.getOfertas().get(0).getPreco(), 0.01);
        assertEquals(1, galaxy.getOfertas().get(0).getHistoricoPrecos().size());

        // Caso 2: mesmo preço não suja o histórico
        galaxy.aplicarPreco("Amazon Brasil", 2799.00, 0.00, null);
        assertEquals(1, galaxy.getOfertas().get(0).getHistoricoPrecos().size());
    }

    @Test
    void testAplicarPrecoRetorno() {
        // Caso 1: loja nova, criou a oferta
        assertTrue(galaxy.aplicarPreco("Amazon Brasil", 2899.00, 0.00, null));
        // Caso 2: preço diferente, mudou
        assertTrue(galaxy.aplicarPreco("Amazon Brasil", 2799.00, 0.00, null));
        // Caso 3: mesmo preço, nada mudou
        assertFalse(galaxy.aplicarPreco("Amazon Brasil", 2799.00, 0.00, null));
    }

    @Test
    void testIdExterno() {
        // Caso 1: construtor antigo não tem id externo
        assertNull(galaxy.getIdExterno());

        // Caso 2: construtor novo guarda o id externo
        Produto comId = new Produto("Smartphone Samsung Galaxy S23 256gb Preto", "Smartphones", "desc", "MLB39957733");
        assertEquals("MLB39957733", comId.getIdExterno());

        // Caso 3: id externo em branco é tratado como ausente
        Produto semId = new Produto("Smart TV 50 4K", "Smart TVs", "desc", "  ");
        assertNull(semId.getIdExterno());
    }

    @Test
    void testUrlCatalogo() {
        // Caso 1: produto sem id externo não tem link
        assertNull(galaxy.getUrlCatalogo());

        // Caso 2: com id externo, o link é montado a partir dele
        Produto comId = new Produto("Smartphone Samsung Galaxy S23 256gb Preto", "Smartphones", "desc", "MLB39957733");
        assertEquals("https://www.mercadolivre.com.br/p/MLB39957733", comId.getUrlCatalogo());
    }

}