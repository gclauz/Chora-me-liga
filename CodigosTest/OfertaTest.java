package br.mackenzie.chorameliga.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OfertaTest {

    private Oferta amazon;
    private Oferta magalu;

    @BeforeEach
    void setUp() {
        amazon = new Oferta("Amazon Brasil", 2899.00, 0.00);
        magalu = new Oferta("Magazine Luiza", 2949.00, 15.00);
    }

    @Test
    @DisplayName("Construtor deve rejeitar dados inválidos")
    void testConstrutorComDadosInvalidos() {
        // Caso 1: loja vazia
        assertThrows(IllegalArgumentException.class,
                () -> new Oferta("", 100.00, 0.00));

        // Caso 2: preço zero
        assertThrows(IllegalArgumentException.class,
                () -> new Oferta("Fast Shop", 0.00, 0.00));

        // Caso 3: frete negativo
        assertThrows(IllegalArgumentException.class,
                () -> new Oferta("Casas Bahia", 100.00, -5.00));
    }

    @Test
    @DisplayName("Atualizar preço deve guardar o valor antigo no histórico")
    void testAtualizarPreco() {
        // Caso 1: uma atualização
        amazon.atualizarPreco(2799.00);
        assertEquals(2799.00, amazon.getPreco(), 0.01);
        assertEquals(1, amazon.getHistoricoPrecos().size());
        assertEquals(2899.00, amazon.getHistoricoPrecos().get(0), 0.01);

        // Caso 2: segunda atualização mantém a ordem do histórico
        amazon.atualizarPreco(3050.00);
        assertEquals(3050.00, amazon.getPreco(), 0.01);
        assertEquals(2, amazon.getHistoricoPrecos().size());
        assertEquals(2799.00, amazon.getHistoricoPrecos().get(1), 0.01);

        // Caso 3: preço inválido lança erro e não altera nada
        assertThrows(IllegalArgumentException.class, () -> amazon.atualizarPreco(-10.00));
        assertEquals(3050.00, amazon.getPreco(), 0.01);
        assertEquals(2, amazon.getHistoricoPrecos().size());
    }

    @Test
    @DisplayName("Menor preço do histórico")
    void testMenorPrecoHistorico() {
        // Caso 1: sem atualizações, o menor é o preço atual
        assertEquals(2899.00, amazon.getMenorPrecoHistorico(), 0.01);

        // Caso 2: o menor preço ficou no histórico
        amazon.atualizarPreco(2799.00);
        amazon.atualizarPreco(3050.00);
        assertEquals(2799.00, amazon.getMenorPrecoHistorico(), 0.01);

        // Caso 3: o preço atual é o menor de todos
        magalu.atualizarPreco(2500.00);
        assertEquals(2500.00, magalu.getMenorPrecoHistorico(), 0.01);
    }

    @Test
    @DisplayName("Preço total deve somar o frete")
    void testCalcularPrecoTotal() {
        // Caso 1: frete grátis
        assertEquals(2899.00, amazon.calcularPrecoTotal(), 0.01);
        assertTrue(amazon.temFreteGratis());

        // Caso 2: com frete
        assertEquals(2964.00, magalu.calcularPrecoTotal(), 0.01);
        assertFalse(magalu.temFreteGratis());
    }

    @Test
    @DisplayName("Cálculo do valor da parcela")
    void testCalcularParcela() {
        // Caso 1: 10 parcelas
        assertEquals(289.90, amazon.calcularParcela(10), 0.01);

        // Caso 2: à vista (1 parcela)
        assertEquals(2899.00, amazon.calcularParcela(1), 0.01);

        // Caso 3: quantidade de parcelas inválida
        assertThrows(IllegalArgumentException.class, () -> amazon.calcularParcela(0));
        assertThrows(IllegalArgumentException.class, () -> amazon.calcularParcela(13));
    }

    @Test
    @DisplayName("Comparação entre ofertas considerando o frete")
    void testIsMaisBarataQue() {
        // Caso 1: Amazon é mais barata que Magalu
        assertTrue(amazon.isMaisBarataQue(magalu));
        assertFalse(magalu.isMaisBarataQue(amazon));

        // Caso 2: preço menor, mas o frete deixa a oferta mais cara
        Oferta fastShop = new Oferta("Fast Shop", 2890.00, 22.00); // total 2912
        assertFalse(fastShop.isMaisBarataQue(amazon));
    }
}
