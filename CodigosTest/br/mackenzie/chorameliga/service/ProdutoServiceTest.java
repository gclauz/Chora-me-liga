package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.OfertaDetalhe;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProdutoServiceTest {

    private ProdutoRepository repositorio;
    private ProdutoService service;
    private Produto galaxy;
    private Oferta amazon;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
        service = new ProdutoService(repositorio);

        galaxy = new Produto("Smartphone Samsung Galaxy S23 128GB", "Smartphones",
                "Tela 6.1 AMOLED, Snapdragon 8 Gen 2");
        amazon = new Oferta("Amazon Brasil", 2899.00, 0.00, "https://www.amazon.com.br");
        galaxy.adicionarOferta(new Oferta("Magazine Luiza", 2949.00, 15.00)); // total 2964
        galaxy.adicionarOferta(amazon);                                       // total 2899
        galaxy.adicionarOferta(new Oferta("Fast Shop", 2890.00, 22.00));      // total 2912
        repositorio.salvar(galaxy);
    }

    @Test
    @DisplayName("Ofertas vêm ordenadas do menor para o maior preço total")
    void testOfertasOrdenadas() {
        DetalhesProduto detalhes = service.buscarDetalhes(galaxy.getId());

        assertEquals("Smartphone Samsung Galaxy S23 128GB", detalhes.nome());
        assertEquals(3, detalhes.ofertas().size());
        assertEquals("Amazon Brasil", detalhes.ofertas().get(0).loja());
        assertEquals("Fast Shop", detalhes.ofertas().get(1).loja());
        assertEquals("Magazine Luiza", detalhes.ofertas().get(2).loja());
    }

    @Test
    @DisplayName("Cada oferta traz total, frete grátis, parcela e URL")
    void testCamposDaOferta() {
        OfertaDetalhe primeira = service.buscarDetalhes(galaxy.getId()).ofertas().get(0);

        assertEquals(2899.00, primeira.preco(), 0.01);
        assertEquals(2899.00, primeira.precoTotal(), 0.01);
        assertTrue(primeira.freteGratis());
        assertEquals(289.90, primeira.parcela10x(), 0.01);
        assertEquals("https://www.amazon.com.br", primeira.url());
    }

    @Test
    @DisplayName("Menor preço atual e menor preço histórico")
    void testMenoresPrecos() {
        // Caso 1: sem atualizações, os dois são o menor preço da oferta mais barata
        DetalhesProduto antes = service.buscarDetalhes(galaxy.getId());
        assertEquals(2899.00, antes.menorPrecoAtual(), 0.01);
        assertEquals(2890.00, antes.menorPrecoHistorico(), 0.01);

        // Caso 2: a Amazon já foi a 2799 e subiu para 3050; o histórico guarda o menor
        amazon.atualizarPreco(2799.00);
        amazon.atualizarPreco(3050.00);
        DetalhesProduto depois = service.buscarDetalhes(galaxy.getId());
        assertEquals(2890.00, depois.menorPrecoAtual(), 0.01);   // Fast Shop (total 2912) virou a mais barata
        assertEquals(2799.00, depois.menorPrecoHistorico(), 0.01);
    }

    @Test
    @DisplayName("Produto sem ofertas retorna lista vazia e preços nulos")
    void testProdutoSemOfertas() {
        Produto tv = new Produto("Smart TV 50 4K", "Smart TVs", "desc");
        repositorio.salvar(tv);

        DetalhesProduto detalhes = service.buscarDetalhes(tv.getId());

        assertTrue(detalhes.ofertas().isEmpty());
        assertNull(detalhes.menorPrecoAtual());
        assertNull(detalhes.menorPrecoHistorico());
    }

    @Test
    @DisplayName("Id inexistente lança ProdutoNaoEncontrado")
    void testIdInexistente() {
        assertThrows(ProdutoNaoEncontrado.class, () -> service.buscarDetalhes(999));
    }

    @Test
    @DisplayName("Construtor rejeita repositório nulo")
    void testConstrutorInvalido() {
        assertThrows(IllegalArgumentException.class, () -> new ProdutoService(null));
    }
}