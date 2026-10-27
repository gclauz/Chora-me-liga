package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.dto.ProdutoResumo;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Cenário: pesquisar "notebook" traz notebooks e também uma fonte de notebook (carregador).
class FiltroPorDominioTest {

    private ProdutoRepository repositorio;
    private FonteCatalogoFake fonte;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
        fonte = new FonteCatalogoFake();
        fonte.adicionar(new ProdutoExterno("MLB1", "Notebook Dell", "MLB-NOTEBOOKS"),
                List.of(new OfertaExterna("Loja", 3000.00, 0.00)));
        fonte.adicionar(new ProdutoExterno("MLB2", "Fonte Notebook 19V", "MLB-LAPTOP_CHARGERS"),
                List.of(new OfertaExterna("Loja", 50.00, 0.00)));
        fonte.adicionar(new ProdutoExterno("MLB3", "Notebook Samsung", "MLB-NOTEBOOKS"), List.of());
    }

    private ProdutoService criarService(FonteCatalogo origem) {
        return new ProdutoService(repositorio, new ImportacaoCatalogoService(repositorio, origem));
    }

    @Test
    void testImportacaoComDominio() {
        ImportacaoCatalogoService importacao = new ImportacaoCatalogoService(repositorio, fonte);

        List<Produto> importados = importacao.importar("notebook", "MLB-NOTEBOOKS");

        // Caso 1: só os dois notebooks entram
        assertEquals(2, importados.size());

        // Caso 2: o carregador nem foi salvo
        assertTrue(repositorio.buscarPorIdExterno("MLB2").isEmpty());
    }

    @Test
    void testImportacaoSemDominioTrazTudo() {
        ImportacaoCatalogoService importacao = new ImportacaoCatalogoService(repositorio, fonte);

        // Caso 1: sem domínio, os três aparecem
        assertEquals(3, importacao.importar("notebook").size());
    }

    @Test
    void testFonteQueIgnoraODominioContinuaFiltrada() {
        FonteCatalogo ignoraODominio = new FonteCatalogo() {
            @Override
            public List<ProdutoExterno> pesquisarProdutos(String nome) {
                return fonte.pesquisarProdutos(nome);
            }

            @Override
            public List<OfertaExterna> buscarOfertas(String idExterno) {
                return fonte.buscarOfertas(idExterno);
            }
        };
        ImportacaoCatalogoService importacao = new ImportacaoCatalogoService(repositorio, ignoraODominio);

        List<Produto> importados = importacao.importar("notebook", "MLB-NOTEBOOKS");

        // Caso 1: mesmo com uma fonte que devolve tudo, o carregador é descartado
        assertEquals(2, importados.size());
        assertTrue(repositorio.buscarPorIdExterno("MLB2").isEmpty());
    }

    @Test
    void testResumosFiltramTambemOQueJaEstavaSalvo() {
        repositorio.salvar(new Produto("Fonte notebook salva antes", "MLB-LAPTOP_CHARGERS", "desc", "MLB9"));
        ProdutoService service = criarService(fonte);

        List<ProdutoResumo> resumos = service.pesquisarResumos("notebook", "MLB-NOTEBOOKS");

        // Caso 1: só os dois notebooks, e o carregador que já estava salvo fica de fora
        assertEquals(2, resumos.size());
        assertTrue(resumos.stream().allMatch(r -> "MLB-NOTEBOOKS".equals(r.categoria())));

        // Caso 2: sem domínio, aparece tudo: o salvo antes e os 3 importados
        assertEquals(4, service.pesquisarResumos("notebook").size());
    }

    @Test
    void testDominioEmBrancoEquivaleASemFiltro() {
        ProdutoService service = criarService(fonte);

        // Caso 1: domínio só com espaços não filtra
        assertEquals(3, service.pesquisarResumos("notebook", "  ").size());
    }

    @Test
    void testDominioInvalidoLancaErro() {
        ProdutoService service = criarService(fonte);

        // Caso 1: espaço no meio
        assertThrows(IllegalArgumentException.class,
                () -> service.pesquisarResumos("notebook", "MLB NOTEBOOKS"));

        // Caso 2: tentativa de acrescentar outro parâmetro no endereço
        assertThrows(IllegalArgumentException.class,
                () -> service.pesquisarResumos("notebook", "MLB-NOTEBOOKS&limit=1"));

        // Caso 3: valor grande demais
        assertThrows(IllegalArgumentException.class,
                () -> service.pesquisarResumos("notebook", "A".repeat(81)));

        // Caso 4: nada foi importado
        assertTrue(repositorio.listarTodos().isEmpty());
    }
}