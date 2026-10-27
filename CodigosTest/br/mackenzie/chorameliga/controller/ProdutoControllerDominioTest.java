package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
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

// O parâmetro opcional ?dominio= da API.
class ProdutoControllerDominioTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FonteCatalogoFake fonte = new FonteCatalogoFake();
        fonte.adicionar(new ProdutoExterno("MLB1", "Notebook Dell", "MLB-NOTEBOOKS"),
                List.of(new OfertaExterna("Loja", 3000.00, 0.00)));
        fonte.adicionar(new ProdutoExterno("MLB2", "Fonte Notebook 19V", "MLB-LAPTOP_CHARGERS"),
                List.of(new OfertaExterna("Loja", 50.00, 0.00)));

        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService service = new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, fonte));
        mockMvc = MockMvcBuilders.standaloneSetup(new ProdutoController(service))
                .setControllerAdvice(new TratadorDeErros())
                .build();
    }

    @Test
    void testPesquisaComDominio() throws Exception {
        // Caso 1: com o domínio de notebooks, só o notebook aparece
        mockMvc.perform(get("/api/produtos").param("nome", "notebook").param("dominio", "MLB-NOTEBOOKS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nome").value("Notebook Dell"))
                .andExpect(jsonPath("$[0].categoria").value("MLB-NOTEBOOKS"));
    }

    @Test
    void testPesquisaSemDominio() throws Exception {
        // Caso 1: sem o parâmetro, a pesquisa continua trazendo tudo
        mockMvc.perform(get("/api/produtos").param("nome", "notebook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testDominioInvalidoDevolve400() throws Exception {
        // Caso 1: domínio fora do formato devolve 400 com a mensagem
        mockMvc.perform(get("/api/produtos").param("nome", "notebook").param("dominio", "MLB NOTEBOOKS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Categoria inválida."));
    }
}