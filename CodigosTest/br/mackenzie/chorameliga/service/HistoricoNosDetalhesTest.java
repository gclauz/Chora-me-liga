package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.HistoricoLoja;
import br.mackenzie.chorameliga.dto.PontoPreco;
import br.mackenzie.chorameliga.historico.HistoricoPrecosMemoria;
import br.mackenzie.chorameliga.historico.RegistroPreco;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// UC06 + UC09: a pesquisa grava os preços e os detalhes mostram o histórico, mesmo depois de reiniciar.
class HistoricoNosDetalhesTest {

    private final HistoricoPrecosMemoria banco = new HistoricoPrecosMemoria();
    private final HistoricoPrecoService historico = new HistoricoPrecoService(banco);

    // um servidor novo: repositório vazio, mas o mesmo banco de histórico
    private ProdutoService servidor(FonteCatalogoFake fonte) {
        ProdutoRepository repositorio = new ProdutoRepository();
        return new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, fonte, historico), historico);
    }

    private static FonteCatalogoFake fonteComPreco(double preco) {
        FonteCatalogoFake fonte = new FonteCatalogoFake();
        fonte.adicionar(new ProdutoExterno("MLB1", "Galaxy S23", "MLB-CELLPHONES"),
                List.of(new OfertaExterna("Loja A", preco, 0), new OfertaExterna("Loja B", 3500.00, 0)));
        return fonte;
    }

    @Test
    void testPesquisaGravaEDetalhesMostram() {
        ProdutoService service = servidor(fonteComPreco(3000.00));
        Produto produto = service.pesquisarPorNome("galaxy").get(0);

        DetalhesProduto detalhes = service.buscarDetalhes(produto.getId());

        // uma série por loja, cada uma com o preço gravado na pesquisa
        assertEquals(2, detalhes.historico().size());
        assertEquals("Loja A", detalhes.historico().get(0).loja());
        assertEquals(3000.00, detalhes.historico().get(0).pontos().get(0).preco(), 0.001);
        assertEquals("Loja B", detalhes.historico().get(1).loja());
    }

    @Test
    void testHistoricoSobreviveAoReinicio() {
        servidor(fonteComPreco(3000.00)).pesquisarPorNome("galaxy");

        // o servidor reinicia e o preço da Loja A caiu
        ProdutoService depois = servidor(fonteComPreco(2799.00));
        Produto produto = depois.pesquisarPorNome("galaxy").get(0);
        DetalhesProduto detalhes = depois.buscarDetalhes(produto.getId());

        // Caso 1: a Loja A tem os dois preços, do mais antigo para o mais recente
        List<PontoPreco> lojaA = detalhes.historico().get(0).pontos();
        assertEquals(2, lojaA.size());
        assertEquals(3000.00, lojaA.get(0).preco(), 0.001);
        assertEquals(2799.00, lojaA.get(1).preco(), 0.001);
        // Caso 2: a Loja B não mudou, então continua com um ponto só
        assertEquals(1, detalhes.historico().get(1).pontos().size());
    }

    @Test
    void testMenorPrecoHistoricoUsaOBanco() {
        // um preço antigo, de antes de o servidor reiniciar, menor que todos os atuais
        banco.registrar("MLB1", new RegistroPreco("Loja C", 2500.00, Instant.parse("2026-10-01T00:00:00Z")));
        ProdutoService service = servidor(fonteComPreco(3000.00));
        Produto produto = service.pesquisarPorNome("galaxy").get(0);

        DetalhesProduto detalhes = service.buscarDetalhes(produto.getId());

        assertEquals(3000.00, detalhes.menorPrecoAtual(), 0.001);
        assertEquals(2500.00, detalhes.menorPrecoHistorico(), 0.001);
        // a loja que não tem mais oferta continua no histórico
        assertEquals(List.of("Loja C", "Loja A", "Loja B"),
                detalhes.historico().stream().map(HistoricoLoja::loja).toList());
    }

    @Test
    void testSemHistoricoConfigurado() {
        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService service = new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, fonteComPreco(3000.00)));
        Produto produto = service.pesquisarPorNome("galaxy").get(0);

        assertTrue(service.buscarDetalhes(produto.getId()).historico().isEmpty());
    }
}
