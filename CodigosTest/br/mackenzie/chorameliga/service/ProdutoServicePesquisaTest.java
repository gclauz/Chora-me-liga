package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.FonteIndisponivelException;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoServicePesquisaTest {

    private ProdutoRepository repositorio;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
    }

    private ProdutoService serviceComFonteDeExemplo() {
        ImportacaoCatalogoService importacao =
                new ImportacaoCatalogoService(repositorio, FonteCatalogoFake.comDadosDeExemplo());
        return new ProdutoService(repositorio, importacao);
    }

    @Test
    void testSemFonteBuscaSoNoRepositorio() {
        repositorio.salvar(new Produto("Notebook Dell Inspiron 15", "Notebooks", "desc"));
        ProdutoService service = new ProdutoService(repositorio);

        // Caso 1: encontra o produto já salvo
        assertEquals(1, service.pesquisarPorNome("dell").size());

        // Caso 2: nome sem correspondência devolve lista vazia
        assertTrue(service.pesquisarPorNome("geladeira").isEmpty());
    }

    @Test
    void testPesquisaImportaDaFonte() {
        ProdutoService service = serviceComFonteDeExemplo();

        List<Produto> resultado = service.pesquisarPorNome("galaxy s23");

        // Caso 1: os dois produtos da fonte aparecem no resultado
        assertEquals(2, resultado.size());

        // Caso 2: foram salvos no repositório, já com as ofertas
        assertEquals(2, repositorio.listarTodos().size());
        assertEquals(3, repositorio.buscarPorIdExterno("MLB39957733").get().getQuantidadeOfertas());

        // Caso 3: dá para abrir os detalhes pelo id (UC03)
        assertEquals(3, service.buscarDetalhes(resultado.get(0).getId()).ofertas().size());
    }

    @Test
    void testPesquisaRepetidaNaoDuplica() {
        ProdutoService service = serviceComFonteDeExemplo();
        service.pesquisarPorNome("galaxy s23");

        List<Produto> segunda = service.pesquisarPorNome("galaxy s23");

        // Caso 1: a segunda pesquisa devolve os mesmos 2 produtos, sem repetir
        assertEquals(2, segunda.size());
        assertEquals(2, repositorio.listarTodos().size());
    }

    @Test
    void testProdutoCadastradoEImportadosJuntos() {
        Produto manual = new Produto("Galaxy S23 cadastrado pelo grupo", "Smartphones", "desc");
        repositorio.salvar(manual);
        ProdutoService service = serviceComFonteDeExemplo();

        List<Produto> resultado = service.pesquisarPorNome("galaxy s23");

        // Caso 1: 1 cadastrado + 2 importados
        assertEquals(3, resultado.size());

        // Caso 2: o que já estava salvo vem primeiro
        assertSame(manual, resultado.get(0));
    }

    @Test
    void testFonteForaDoArUsaOQueJaTem() {
        repositorio.salvar(new Produto("Galaxy S23 antigo", "Smartphones", "desc"));
        FonteCatalogo foraDoAr = new FonteCatalogo() {
            @Override
            public List<ProdutoExterno> pesquisarProdutos(String nome) {
                throw new FonteIndisponivelException("fora do ar");
            }

            @Override
            public List<OfertaExterna> buscarOfertas(String idExterno) {
                throw new FonteIndisponivelException("fora do ar");
            }
        };
        ProdutoService service =
                new ProdutoService(repositorio, new ImportacaoCatalogoService(repositorio, foraDoAr));

        // Caso 1: não lança erro e devolve o que já estava salvo
        assertEquals(1, service.pesquisarPorNome("galaxy").size());
    }

    @Test
    void testNomeInvalido() {
        ProdutoService service = serviceComFonteDeExemplo();

        // Caso 1: nome vazio
        assertThrows(IllegalArgumentException.class, () -> service.pesquisarPorNome("  "));

        // Caso 2: nome nulo
        assertThrows(IllegalArgumentException.class, () -> service.pesquisarPorNome(null));

        // Caso 3: nada foi importado
        assertTrue(repositorio.listarTodos().isEmpty());
    }
}