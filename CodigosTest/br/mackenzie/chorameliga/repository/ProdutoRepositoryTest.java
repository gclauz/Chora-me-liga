package br.mackenzie.chorameliga.repository;

import br.mackenzie.chorameliga.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoRepositoryTest {

    private ProdutoRepository repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
        repositorio.salvar(new Produto("Smartphone Samsung Galaxy S23 128GB", "Smartphones", "desc"));
        repositorio.salvar(new Produto("Notebook Dell Inspiron 15", "Notebooks", "desc"));
        repositorio.salvar(new Produto("Câmera Canon EOS", "Câmeras", "desc"));
    }

    @Test
    @DisplayName("Salvar adiciona o produto e rejeita nulo")
    void testSalvar() {
        // Caso 1: os 3 produtos do setUp foram salvos
        assertEquals(3, repositorio.listarTodos().size());

        // Caso 2: produto nulo é rejeitado
        assertThrows(IllegalArgumentException.class, () -> repositorio.salvar(null));
        assertEquals(3, repositorio.listarTodos().size());
    }

    @Test
    @DisplayName("Busca ignora maiúsculas e minúsculas")
    void testBuscaIgnoraMaiusculas() {
        // Caso 1: tudo minúsculo
        assertEquals(1, repositorio.buscarPorNome("galaxy").size());

        // Caso 2: tudo maiúsculo
        assertEquals(1, repositorio.buscarPorNome("NOTEBOOK").size());
    }

    @Test
    @DisplayName("Busca ignora acentos")
    void testBuscaIgnoraAcentos() {
        // Caso 1: sem acento encontra produto com acento
        assertEquals(1, repositorio.buscarPorNome("camera").size());

        // Caso 2: com acento também encontra
        assertEquals(1, repositorio.buscarPorNome("Câmera").size());
    }

    @Test
    @DisplayName("Busca com várias palavras em qualquer ordem")
    void testBuscaComVariasPalavras() {
        // Caso 1: palavras na mesma ordem do nome
        assertEquals(1, repositorio.buscarPorNome("galaxy s23").size());

        // Caso 2: palavras em outra ordem
        assertEquals(1, repositorio.buscarPorNome("s23 galaxy").size());

        // Caso 3: uma das palavras não existe, então não encontra
        assertTrue(repositorio.buscarPorNome("galaxy dell").isEmpty());
    }

    @Test
    @DisplayName("Busca sem resultado ou com texto vazio retorna lista vazia")
    void testBuscaSemResultado() {
        // Caso 1: nenhum produto com esse nome
        assertTrue(repositorio.buscarPorNome("geladeira").isEmpty());

        // Caso 2: texto vazio ou nulo não lista tudo
        assertTrue(repositorio.buscarPorNome("   ").isEmpty());
        assertTrue(repositorio.buscarPorNome(null).isEmpty());
    }

    @Test
    @DisplayName("A lista de produtos não pode ser alterada por fora")
    void testListaImutavel() {
        List<Produto> lista = repositorio.listarTodos();
        assertThrows(UnsupportedOperationException.class,
                () -> lista.add(new Produto("Outro", "Cat", "desc")));
    }
    @Test
    @DisplayName("Salvar gera um id único para cada produto")
    void testSalvarGeraId() {
        Produto a = new Produto("Teclado Mecânico", "Periféricos", "desc");
        Produto b = new Produto("Mouse Gamer", "Periféricos", "desc");
        repositorio.salvar(a);
        repositorio.salvar(b);

        assertNotNull(a.getId());
        assertNotNull(b.getId());
        assertNotEquals(a.getId(), b.getId());
    }

    @Test
    @DisplayName("Buscar por id encontra o produto ou retorna vazio")
    void testBuscarPorId() {
        Produto primeiro = repositorio.listarTodos().get(0);

        assertSame(primeiro, repositorio.buscarPorId(primeiro.getId()).get());
        assertTrue(repositorio.buscarPorId(999).isEmpty());
    }
}