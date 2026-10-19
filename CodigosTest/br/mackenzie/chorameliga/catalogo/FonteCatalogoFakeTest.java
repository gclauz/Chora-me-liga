package br.mackenzie.chorameliga.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FonteCatalogoFakeTest {

    private FonteCatalogoFake fonte;

    @BeforeEach
    void setUp() {
        fonte = FonteCatalogoFake.comDadosDeExemplo();
    }

    @Test
    void testPesquisarProdutos() {
        // Caso 1: "galaxy s23" encontra os dois produtos de exemplo
        assertEquals(2, fonte.pesquisarProdutos("galaxy s23").size());

        // Caso 2: palavras em outra ordem também encontram
        assertEquals(2, fonte.pesquisarProdutos("s23 galaxy").size());

        // Caso 3: busca mais específica encontra só um
        List<ProdutoExterno> preto = fonte.pesquisarProdutos("preto");
        assertEquals(1, preto.size());
        assertEquals("MLB39957733", preto.get(0).idExterno());
    }

    @Test
    void testPesquisaIgnoraMaiusculas() {
        // Caso 1: busca em maiúsculas encontra o produto
        assertEquals(2, fonte.pesquisarProdutos("GALAXY").size());
    }

    @Test
    void testPesquisaSemResultado() {
        // Caso 1: nenhum produto com esse nome
        assertTrue(fonte.pesquisarProdutos("geladeira").isEmpty());

        // Caso 2: texto vazio ou nulo não lista tudo
        assertTrue(fonte.pesquisarProdutos("  ").isEmpty());
        assertTrue(fonte.pesquisarProdutos(null).isEmpty());
    }

    @Test
    void testBuscarOfertas() {
        // Caso 1: produto com 3 ofertas
        List<OfertaExterna> ofertas = fonte.buscarOfertas("MLB39957733");
        assertEquals(3, ofertas.size());
        assertEquals(3499.00, ofertas.get(0).preco(), 0.01);

        // Caso 2: id desconhecido devolve lista vazia
        assertTrue(fonte.buscarOfertas("MLB000").isEmpty());

        // Caso 3: id nulo devolve lista vazia
        assertTrue(fonte.buscarOfertas(null).isEmpty());
    }
}