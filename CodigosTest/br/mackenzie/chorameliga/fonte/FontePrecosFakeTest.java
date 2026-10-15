package br.mackenzie.chorameliga.fonte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FontePrecosFakeTest {

    @Test
    @DisplayName("Dados de exemplo trazem as 4 lojas do Galaxy S23")
    void testDadosDeExemplo() {
        FontePrecosFake fonte = FontePrecosFake.comDadosDeExemplo();
        assertEquals(4, fonte.buscarPorNome("galaxy s23").size());
    }

    @Test
    @DisplayName("Busca ignora maiúsculas e minúsculas")
    void testBuscaIgnoraMaiusculas() {
        FontePrecosFake fonte = FontePrecosFake.comDadosDeExemplo();
        assertEquals(4, fonte.buscarPorNome("GALAXY").size());
    }

    @Test
    @DisplayName("Adicionar preço da mesma loja substitui o anterior")
    void testAdicionarSubstitui() {
        FontePrecosFake fonte = new FontePrecosFake();
        fonte.adicionar(new PrecoExterno("Amazon Brasil", "Galaxy S23", 2899.00, 0.00, null));
        fonte.adicionar(new PrecoExterno("amazon brasil", "Galaxy S23", 2799.00, 0.00, null));

        List<PrecoExterno> resultado = fonte.buscarPorNome("Galaxy");
        assertEquals(1, resultado.size());
        assertEquals(2799.00, resultado.get(0).preco(), 0.01);
    }

    @Test
    @DisplayName("Busca vazia ou sem resultado retorna lista vazia")
    void testBuscaVazia() {
        FontePrecosFake fonte = FontePrecosFake.comDadosDeExemplo();
        assertTrue(fonte.buscarPorNome("  ").isEmpty());
        assertTrue(fonte.buscarPorNome(null).isEmpty());
        assertTrue(fonte.buscarPorNome("geladeira").isEmpty());
    }
}