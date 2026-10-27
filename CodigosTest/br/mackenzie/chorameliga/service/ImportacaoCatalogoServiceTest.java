package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImportacaoCatalogoServiceTest {

    private ProdutoRepository repositorio;
    private FonteCatalogoFake fonte;
    private ImportacaoCatalogoService service;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
        fonte = FonteCatalogoFake.comDadosDeExemplo();
        service = new ImportacaoCatalogoService(repositorio, fonte);
    }

    @Test
    void testImportaProdutosEOfertas() {
        List<Produto> importados = service.importar("galaxy s23");

        // Caso 1: os dois produtos da fonte foram importados e salvos
        assertEquals(2, importados.size());
        assertEquals(2, repositorio.listarTodos().size());

        // Caso 2: cada produto recebeu as ofertas da fonte
        Produto preto = repositorio.buscarPorIdExterno("MLB39957733").get();
        assertEquals(3, preto.getQuantidadeOfertas());
        assertEquals(3499.00, preto.getOfertaMaisBarata().get().getPreco(), 0.01);

        Produto violeta = repositorio.buscarPorIdExterno("MLB47311354").get();
        assertEquals(1, violeta.getQuantidadeOfertas());
    }

    @Test
    void testImportarDuasVezesNaoDuplica() {
        service.importar("galaxy s23");
        service.importar("galaxy s23");

        // Caso 1: os produtos não são duplicados
        assertEquals(2, repositorio.listarTodos().size());

        // Caso 2: as ofertas também não, e o histórico continua vazio (preços iguais)
        Produto preto = repositorio.buscarPorIdExterno("MLB39957733").get();
        assertEquals(3, preto.getQuantidadeOfertas());
        assertTrue(preto.getOfertas().get(0).getHistoricoPrecos().isEmpty());
    }

    @Test
    void testAtualizaPrecoNaSegundaImportacao() {
        service.importar("galaxy s23");

        // a loja baixou o preço de 3499 para 3399
        fonte.adicionar(
                new ProdutoExterno("MLB39957733", "Smartphone Samsung Galaxy S23 256gb Preto", "MLB-CELLPHONES"),
                List.of(new OfertaExterna("Loja Exemplo 1", 3399.00, 0.00),
                        new OfertaExterna("Loja Exemplo 2", 4299.00, 0.00),
                        new OfertaExterna("Loja Exemplo 3", 4499.00, 0.00)));
        service.importar("galaxy s23");

        // Caso 1: o preço novo foi aplicado, sem duplicar a oferta
        Produto preto = repositorio.buscarPorIdExterno("MLB39957733").get();
        Oferta primeira = preto.getOfertas().get(0);
        assertEquals(3, preto.getQuantidadeOfertas());
        assertEquals(3399.00, primeira.getPreco(), 0.01);

        // Caso 2: o preço antigo ficou no histórico
        assertEquals(1, primeira.getHistoricoPrecos().size());
        assertEquals(3499.00, primeira.getHistoricoPrecos().get(0), 0.01);
    }

    @Test
    void testReaproveitaProdutoJaCadastrado() {
        Produto existente = new Produto("Nome salvo antes", "Smartphones", "desc", "MLB39957733");
        repositorio.salvar(existente);

        service.importar("galaxy s23");

        // Caso 1: o produto de mesmo id externo foi reaproveitado, só o outro é novo
        assertEquals(2, repositorio.listarTodos().size());

        // Caso 2: ele recebeu as ofertas e manteve o nome
        assertEquals(3, existente.getQuantidadeOfertas());
        assertEquals("Nome salvo antes", existente.getNome());
    }

    @Test
    void testOfertasRecebemUrlDoCatalogo() {
        service.importar("preto");

        Produto preto = repositorio.buscarPorIdExterno("MLB39957733").get();
        assertEquals("https://www.mercadolivre.com.br/p/MLB39957733", preto.getOfertas().get(0).getUrl());
    }

    @Test
    void testOfertaComLinkProprioUsaOLinkDela() {
        FonteCatalogoFake fonteComLink = new FonteCatalogoFake();
        fonteComLink.adicionar(
                new ProdutoExterno("MLB1", "Produto Teste", "MLB-TESTE"),
                List.of(new OfertaExterna("Loja A", 100.00, 0.00, "https://produto.mercadolivre.com.br/MLB-111"),
                        new OfertaExterna("Loja B", 200.00, 0.00)));
        ImportacaoCatalogoService outro = new ImportacaoCatalogoService(repositorio, fonteComLink);

        outro.importar("produto");

        Produto produto = repositorio.buscarPorIdExterno("MLB1").get();

        // Caso 1: a oferta com link próprio mantém o link
        assertEquals("https://produto.mercadolivre.com.br/MLB-111", produto.getOfertas().get(0).getUrl());

        // Caso 2: a oferta sem link usa a página do produto
        assertEquals("https://www.mercadolivre.com.br/p/MLB1", produto.getOfertas().get(1).getUrl());
    }

    @Test
    void testOfertaInvalidaNaoInterrompe() {
        FonteCatalogoFake fonteComDadoRuim = new FonteCatalogoFake();
        fonteComDadoRuim.adicionar(
                new ProdutoExterno("MLB1", "Produto Teste", "MLB-TESTE"),
                List.of(new OfertaExterna("Loja Ruim", 0.00, 0.00),
                        new OfertaExterna("Loja Boa", 100.00, 0.00)));
        ImportacaoCatalogoService outro = new ImportacaoCatalogoService(repositorio, fonteComDadoRuim);

        outro.importar("produto");

        // Caso 1: a oferta com preço zero foi ignorada
        Produto produto = repositorio.buscarPorIdExterno("MLB1").get();
        assertEquals(1, produto.getQuantidadeOfertas());

        // Caso 2: a oferta válida foi aplicada
        assertEquals("Loja Boa", produto.getOfertas().get(0).getLoja());
    }

    @Test
    void testFonteComErroNasOfertas() {
        FonteCatalogo fonteComErro = new FonteCatalogo() {
            @Override
            public List<ProdutoExterno> pesquisarProdutos(String nome) {
                return List.of(new ProdutoExterno("MLB2", "Produto Sem Ofertas", "MLB-TESTE"));
            }

            @Override
            public List<OfertaExterna> buscarOfertas(String idExterno) {
                throw new IllegalStateException("fora do ar");
            }
        };
        ImportacaoCatalogoService outro = new ImportacaoCatalogoService(repositorio, fonteComErro);

        List<Produto> importados = outro.importar("produto");

        // Caso 1: o produto foi salvo mesmo sem conseguir as ofertas
        assertEquals(1, importados.size());

        // Caso 2: ele ficou sem ofertas, sem lançar erro
        assertEquals(0, importados.get(0).getQuantidadeOfertas());
    }

    @Test
    void testProdutosSemOfertasTambemSaoDevolvidos() {
        FonteCatalogoFake fonteMista = new FonteCatalogoFake();
        fonteMista.adicionar(new ProdutoExterno("MLB1", "Console A sem oferta", "MLB-TESTE"), List.of());
        fonteMista.adicionar(new ProdutoExterno("MLB2", "Console B", "MLB-TESTE"),
                List.of(new OfertaExterna("Loja", 100.00, 0.00)));
        fonteMista.adicionar(new ProdutoExterno("MLB3", "Console C", "MLB-TESTE"),
                List.of(new OfertaExterna("Loja", 200.00, 0.00)));
        ImportacaoCatalogoService outro = new ImportacaoCatalogoService(repositorio, fonteMista);

        List<Produto> importados = outro.importar("console");

        // Caso 1: os três aparecem, inclusive o sem oferta
        assertEquals(3, importados.size());

        // Caso 2: o produto sem oferta foi salvo, sem ofertas
        assertEquals(0, repositorio.buscarPorIdExterno("MLB1").get().getQuantidadeOfertas());

        // Caso 3: os outros dois têm uma oferta cada
        assertEquals(1, repositorio.buscarPorIdExterno("MLB2").get().getQuantidadeOfertas());
        assertEquals(1, repositorio.buscarPorIdExterno("MLB3").get().getQuantidadeOfertas());
    }

    @Test
    void testParaDeConsultarAoReunirOsProdutosComOferta() {
        FonteCatalogoFake base = new FonteCatalogoFake();
        for (int i = 1; i <= 8; i++) {
            base.adicionar(new ProdutoExterno("MLB" + i, "Console " + i, "MLB-TESTE"),
                    List.of(new OfertaExterna("Loja", 100.00 + i, 0.00)));
        }
        int[] consultas = {0};
        FonteCatalogo contador = new FonteCatalogo() {
            @Override
            public List<ProdutoExterno> pesquisarProdutos(String nome) {
                return base.pesquisarProdutos(nome);
            }

            @Override
            public List<OfertaExterna> buscarOfertas(String idExterno) {
                consultas[0]++;
                return base.buscarOfertas(idExterno);
            }
        };
        ImportacaoCatalogoService outro = new ImportacaoCatalogoService(repositorio, contador);

        List<Produto> importados = outro.importar("console");

        // Caso 1: de 8 candidatos com oferta, entram só 5
        assertEquals(5, importados.size());

        // Caso 2: e só 5 consultas de ofertas foram feitas (não gasta as outras 3)
        assertEquals(5, consultas[0]);
    }

    @Test
    void testNomeInvalido() {
        // Caso 1: nome vazio
        assertThrows(IllegalArgumentException.class, () -> service.importar("  "));

        // Caso 2: nome nulo
        assertThrows(IllegalArgumentException.class, () -> service.importar(null));

        // Caso 3: nada foi salvo
        assertTrue(repositorio.listarTodos().isEmpty());
    }

    @Test
    void testSemResultados() {
        // Caso 1: pesquisa sem correspondência devolve lista vazia
        assertTrue(service.importar("geladeira").isEmpty());

        // Caso 2: o repositório continua vazio
        assertTrue(repositorio.listarTodos().isEmpty());
    }

    @Test
    void testConstrutorInvalido() {
        // Caso 1: repositório nulo
        assertThrows(IllegalArgumentException.class, () -> new ImportacaoCatalogoService(null, fonte));

        // Caso 2: fonte nula
        assertThrows(IllegalArgumentException.class, () -> new ImportacaoCatalogoService(repositorio, null));
    }
}