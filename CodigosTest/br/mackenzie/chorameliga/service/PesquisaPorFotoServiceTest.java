package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.dto.ResultadoPesquisaFoto;
import br.mackenzie.chorameliga.reconhecimento.FormatoImagem;
import br.mackenzie.chorameliga.reconhecimento.ProdutoReconhecido;
import br.mackenzie.chorameliga.reconhecimento.ReconhecedorFoto;
import br.mackenzie.chorameliga.reconhecimento.ReconhecedorFotoFake;
import br.mackenzie.chorameliga.reconhecimento.ReconhecimentoIndisponivelException;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PesquisaPorFotoServiceTest {

    private static final byte[] FOTO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private PesquisaPorFotoService montar(ReconhecedorFoto reconhecedor) {
        FonteCatalogoFake fonte = FonteCatalogoFake.comDadosDeExemplo();
        fonte.adicionar(new ProdutoExterno("MLB1", "Capinha para Galaxy S23", "MLB-CELLPHONE_COVERS"),
                List.of(new OfertaExterna("Loja Capas", 29.90, 0.00)));
        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService produtos = new ProdutoService(repositorio, new ImportacaoCatalogoService(repositorio, fonte));
        return new PesquisaPorFotoService(reconhecedor, produtos);
    }

    private PesquisaPorFotoService reconhecendo(String nome, String dominio) {
        return montar(ReconhecedorFotoFake.sempreReconhece(new ProdutoReconhecido(nome, dominio)));
    }

    @Test
    void testFotoReconhecidaPesquisaNaCategoria() {
        // Caso 1: reconheceu "galaxy s23" como celular: só os celulares, sem a capinha
        ResultadoPesquisaFoto r = reconhecendo("galaxy s23", "MLB-CELLPHONES").pesquisar(FOTO_JPEG);

        assertEquals("galaxy s23", r.nomeReconhecido());
        assertEquals("MLB-CELLPHONES", r.dominioReconhecido());
        assertEquals(2, r.produtos().size());
        assertTrue(r.produtos().stream().allMatch(p -> "MLB-CELLPHONES".equals(p.categoria())));
    }

    @Test
    void testFotoSemCategoriaPesquisaSoPeloNome() {
        // Caso 1: a IA não soube a categoria: aparecem celulares e capinha
        ResultadoPesquisaFoto r = reconhecendo("galaxy s23", null).pesquisar(FOTO_JPEG);

        assertNull(r.dominioReconhecido());
        assertEquals(3, r.produtos().size());
    }

    @Test
    void testCategoriaErradaCaiParaPesquisaPeloNome() {
        // Caso 1: categoria sem nenhum produto com esse nome: pesquisa de novo sem a categoria
        assertEquals(3, reconhecendo("galaxy s23", "MLB-NOTEBOOKS").pesquisar(FOTO_JPEG).produtos().size());

        // Caso 2: categoria em formato inválido não vira erro para o usuário
        assertEquals(3, reconhecendo("galaxy s23", "isso não é domínio!").pesquisar(FOTO_JPEG).produtos().size());
    }

    @Test
    void testReconhecidoSemProdutos() {
        // Caso 1: reconheceu, mas não há nada com esse nome: lista vazia, sem erro
        ResultadoPesquisaFoto r = reconhecendo("geladeira", null).pesquisar(FOTO_JPEG);

        assertEquals("geladeira", r.nomeReconhecido());
        assertTrue(r.produtos().isEmpty());
    }

    @Test
    void testFotoNaoReconhecida() {
        // Caso 1: REQ 5 - a IA não identificou o produto
        PesquisaPorFotoService service = montar(ReconhecedorFotoFake.nuncaReconhece());

        FotoNaoReconhecida erro = assertThrows(FotoNaoReconhecida.class, () -> service.pesquisar(FOTO_JPEG));
        assertTrue(erro.getMessage().contains("Não conseguimos identificar"));
    }

    @Test
    void testReconhecimentoIndisponivel() {
        // Caso 1: o serviço de IA caiu: o erro sobe para a API responder 503
        PesquisaPorFotoService service = montar(imagem -> {
            throw new ReconhecimentoIndisponivelException("fora do ar", null);
        });

        assertThrows(ReconhecimentoIndisponivelException.class, () -> service.pesquisar(FOTO_JPEG));
    }

    @Test
    void testFotoInvalida() {
        PesquisaPorFotoService service = reconhecendo("galaxy s23", null);

        // Caso 1: foto nula ou vazia
        assertThrows(IllegalArgumentException.class, () -> service.pesquisar(null));
        assertThrows(IllegalArgumentException.class, () -> service.pesquisar(new byte[0]));

        // Caso 2: arquivo que não é imagem
        assertThrows(IllegalArgumentException.class, () -> service.pesquisar("texto".getBytes()));

        // Caso 3: imagem acima de 5 MB
        byte[] grande = Arrays.copyOf(FOTO_JPEG, FormatoImagem.TAMANHO_MAXIMO + 1);
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class, () -> service.pesquisar(grande));
        assertTrue(erro.getMessage().contains("5 MB"));
    }

    @Test
    void testDependenciasObrigatorias() {
        // Caso 1: sem reconhecedor ou sem serviço de produtos não dá para montar
        ProdutoService produtos = new ProdutoService(new ProdutoRepository());
        assertThrows(IllegalArgumentException.class, () -> new PesquisaPorFotoService(null, produtos));
        assertThrows(IllegalArgumentException.class,
                () -> new PesquisaPorFotoService(ReconhecedorFotoFake.nuncaReconhece(), null));
    }
}
