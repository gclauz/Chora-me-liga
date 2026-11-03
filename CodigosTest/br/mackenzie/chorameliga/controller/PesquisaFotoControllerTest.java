package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.reconhecimento.ProdutoReconhecido;
import br.mackenzie.chorameliga.reconhecimento.ReconhecedorFoto;
import br.mackenzie.chorameliga.reconhecimento.ReconhecedorFotoFake;
import br.mackenzie.chorameliga.reconhecimento.ReconhecimentoIndisponivelException;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import br.mackenzie.chorameliga.service.ImportacaoCatalogoService;
import br.mackenzie.chorameliga.service.PesquisaPorFotoService;
import br.mackenzie.chorameliga.service.ProdutoService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PesquisaFotoControllerTest {

    private static final byte[] FOTO_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};

    private MockMvc montar(ReconhecedorFoto reconhecedor) {
        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService produtos = new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, FonteCatalogoFake.comDadosDeExemplo()));
        return MockMvcBuilders.standaloneSetup(
                        new PesquisaFotoController(new PesquisaPorFotoService(reconhecedor, produtos)))
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    private MockMultipartFile foto(byte[] conteudo) {
        return new MockMultipartFile("foto", "foto.jpg", "image/jpeg", conteudo);
    }

    @Test
    void testFotoReconhecida() throws Exception {
        // Caso 1: 200 com o nome reconhecido e os produtos encontrados
        MockMvc mvc = montar(ReconhecedorFotoFake.sempreReconhece(
                new ProdutoReconhecido("Galaxy S23", "MLB-CELLPHONES")));

        mvc.perform(multipart("/api/produtos/foto").file(foto(FOTO_JPEG)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeReconhecido").value("Galaxy S23"))
                .andExpect(jsonPath("$.dominioReconhecido").value("MLB-CELLPHONES"))
                .andExpect(jsonPath("$.produtos.length()").value(2))
                .andExpect(jsonPath("$.produtos[0].menorPreco").value(3499.0));
    }

    @Test
    void testFotoNaoReconhecida() throws Exception {
        // Caso 1: REQ 5 - 422 com a mensagem para o usuário
        montar(ReconhecedorFotoFake.nuncaReconhece())
                .perform(multipart("/api/produtos/foto").file(foto(FOTO_JPEG)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void testReconhecimentoIndisponivel() throws Exception {
        // Caso 1: IA fora do ar responde 503
        montar(imagem -> { throw new ReconhecimentoIndisponivelException("fora do ar", null); })
                .perform(multipart("/api/produtos/foto").file(foto(FOTO_JPEG)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void testArquivoInvalido() throws Exception {
        MockMvc mvc = montar(ReconhecedorFotoFake.nuncaReconhece());

        // Caso 1: arquivo que não é imagem, mesmo dizendo ser image/jpeg
        mvc.perform(multipart("/api/produtos/foto").file(foto("nao sou foto".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Formato de imagem não aceito. Use JPG, PNG ou WEBP."));

        // Caso 2: arquivo vazio
        mvc.perform(multipart("/api/produtos/foto").file(foto(new byte[0])))
                .andExpect(status().isBadRequest());

        // Caso 3: requisição sem o campo "foto"
        mvc.perform(multipart("/api/produtos/foto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Envie uma foto do produto."));
    }
}
