package br.mackenzie.chorameliga.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Construtor deve rejeitar nome inválido")
    void testConstrutorComNomeInvalido() {
        assertThrows(IllegalArgumentException.class,
                () -> new Produto("", "Smartphones", "desc"));
        assertThrows(IllegalArgumentException.class,
                () -> new Produto(null, "Smartphones", "desc"));
    }

    @Test
    @DisplayName("Adicionar oferta aumenta a quantidade de ofertas")
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
    @DisplayName("Oferta mais barata considera o frete")
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
    @DisplayName("A lista de ofertas não pode ser alterada por fora")
    void testListaDeOfertasImutavel() {
        Oferta amazon = new Oferta("Amazon Brasil", 2899.00, 0.00);
        assertThrows(UnsupportedOperationException.class,
                () -> galaxy.getOfertas().add(amazon));
    }
}