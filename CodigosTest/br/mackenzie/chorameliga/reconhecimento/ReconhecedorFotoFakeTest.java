package br.mackenzie.chorameliga.reconhecimento;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ReconhecedorFotoFakeTest {

    private static final byte[] FOTO = {1, 2, 3};

    @Test
    void testReconheceProduto() {
        // Caso 1: o fake devolve o produto configurado, com nome e domínio
        ReconhecedorFoto reconhecedor = ReconhecedorFotoFake.sempreReconhece(
                new ProdutoReconhecido("Galaxy S23", "MLB-CELLPHONES"));

        Optional<ProdutoReconhecido> resultado = reconhecedor.reconhecer(FOTO);

        assertTrue(resultado.isPresent());
        assertEquals("Galaxy S23", resultado.get().nome());
        assertEquals("MLB-CELLPHONES", resultado.get().dominio());
    }

    @Test
    void testNaoReconhece() {
        // Caso 1: foto não identificada devolve vazio (REQ 5)
        assertTrue(ReconhecedorFotoFake.nuncaReconhece().reconhecer(FOTO).isEmpty());
    }

    @Test
    void testFotoVazia() {
        ReconhecedorFoto reconhecedor = ReconhecedorFotoFake.nuncaReconhece();

        // Caso 1: foto nula é rejeitada
        assertThrows(IllegalArgumentException.class, () -> reconhecedor.reconhecer(null));

        // Caso 2: foto sem bytes é rejeitada
        assertThrows(IllegalArgumentException.class, () -> reconhecedor.reconhecer(new byte[0]));
    }

    @Test
    void testProdutoReconhecido() {
        // Caso 1: nome e domínio vêm sem espaços nas pontas
        ProdutoReconhecido p = new ProdutoReconhecido("  Galaxy S23 ", "  ");
        assertEquals("Galaxy S23", p.nome());

        // Caso 2: domínio em branco vira nulo
        assertNull(p.dominio());

        // Caso 3: nome obrigatório
        assertThrows(IllegalArgumentException.class, () -> new ProdutoReconhecido(" ", null));
    }
}
