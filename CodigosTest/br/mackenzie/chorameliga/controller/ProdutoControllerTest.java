package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.FonteIndisponivelException;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import br.mackenzie.chorameliga.service.ImportacaoCatalogoService;
import br.mackenzie.chorameliga.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProdutoControllerTest {

    private MockMvc mockMvc;

    // Monta o controller com o service de verdade e uma fonte de dados à escolha
    private MockMvc montar(FonteCatalogo fonte) {
        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService service = new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, fonte));
        return MockMvcBuilders.standaloneSetup(new ProdutoController(service))
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @BeforeEach
    void setUp() {
        mockMvc = montar(FonteCatalogoFake.comDadosDeExemplo());
    }

    @Test
    void testPesquisarDevolveLista() throws Exception {
        // Caso 1: pesquisa com resultado responde 200 com os dois produtos de exemplo
        mockMvc.perform(get("/api/produtos").param("nome", "galaxy s23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Caso 2: o primeiro traz nome, quantidade de ofertas e menor preço
                .andExpect(jsonPath("$[0].nome").value("Smartphone Samsung Galaxy S23 256gb Preto"))
                .andExpect(jsonPath("$[0].quantidadeOfertas").value(3))
                .andExpect(jsonPath("$[0].menorPreco").value(3499.0));
    }

    @Test
    void testPesquisarSemResultado() throws Exception {
        // Caso 1: pesquisa sem correspondência é 200 com lista vazia (e não erro)
        mockMvc.perform(get("/api/produtos").param("nome", "geladeira"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void testPesquisarSemParametro() throws Exception {
        // Caso 1: faltou o ?nome= devolve 400 com mensagem
        mockMvc.perform(get("/api/produtos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void testPesquisarNomeEmBranco() throws Exception {
        // Caso 1: nome só com espaços devolve 400 com a mensagem do sistema
        mockMvc.perform(get("/api/produtos").param("nome", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Informe o nome do produto."));
    }

    @Test
    void testDetalhes() throws Exception {
        // a pesquisa importa os produtos; o primeiro salvo recebe o id 1
        mockMvc.perform(get("/api/produtos").param("nome", "galaxy s23"));

        // Caso 1: detalhes do produto 1 com as 3 ofertas
        mockMvc.perform(get("/api/produtos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Smartphone Samsung Galaxy S23 256gb Preto"))
                .andExpect(jsonPath("$.ofertas.length()").value(3))
                // Caso 2: ofertas da mais barata para a mais cara
                .andExpect(jsonPath("$.ofertas[0].preco").value(3499.0))
                .andExpect(jsonPath("$.ofertas[2].preco").value(4499.0))
                .andExpect(jsonPath("$.menorPrecoAtual").value(3499.0));
    }

    @Test
    void testDetalhesDeIdInexistente() throws Exception {
        // Caso 1: id que não existe devolve 404 com mensagem
        mockMvc.perform(get("/api/produtos/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void testDetalhesComIdInvalido() throws Exception {
        // Caso 1: id que não é número devolve 400
        mockMvc.perform(get("/api/produtos/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").exists());
    }

    @Test
    void testFonteForaDoArNaoQuebraAPesquisa() throws Exception {
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

        // Caso 1: com a fonte fora do ar a API responde 200 com lista vazia
        montar(foraDoAr).perform(get("/api/produtos").param("nome", "galaxy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}