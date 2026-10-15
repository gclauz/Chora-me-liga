package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.fonte.FontePrecos;
import br.mackenzie.chorameliga.fonte.FontePrecosFake;
import br.mackenzie.chorameliga.fonte.PrecoExterno;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AtualizacaoPrecosTest {

    private static final String GALAXY = "Smartphone Samsung Galaxy S23 128GB";

    private ProdutoRepository repositorio;
    private Produto galaxy;
    private FontePrecosFake fonte;
    private AtualizacaoPrecos service;

    @BeforeEach
    void setUp() {
        repositorio = new ProdutoRepository();
        galaxy = new Produto(GALAXY, "Smartphones", "desc");
        repositorio.salvar(galaxy);
        fonte = new FontePrecosFake();
        service = new AtualizacaoPrecos(repositorio, fonte);
    }

    @Test
    @DisplayName("Cria as ofertas quando o produto ainda não tem nenhuma")
    void testCriaOfertasNovas() {
        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY, 2899.00, 0.00, null));
        fonte.adicionar(new PrecoExterno("Magazine Luiza", GALAXY, 2949.00, 15.00, null));

        ResultadoAtualizacao resultado = service.atualizarPrecos();

        assertEquals(2, galaxy.getQuantidadeOfertas());
        assertEquals(2, resultado.ofertasAlteradas());
        assertEquals(0, resultado.falhas());
    }

    @Test
    @DisplayName("Atualiza o preço e guarda o valor antigo no histórico")
    void testAtualizaEGuardaHistorico() {
        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY, 2899.00, 0.00, null));
        service.atualizarPrecos();

        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY, 2799.00, 0.00, null));
        ResultadoAtualizacao resultado = service.atualizarPrecos();

        assertEquals(1, resultado.ofertasAlteradas());
        assertEquals(1, galaxy.getQuantidadeOfertas());
        assertEquals(2799.00, galaxy.getOfertas().get(0).getPreco(), 0.01);
        assertEquals(1, galaxy.getOfertas().get(0).getHistoricoPrecos().size());
        assertEquals(2899.00, galaxy.getOfertas().get(0).getHistoricoPrecos().get(0), 0.01);
    }

    @Test
    @DisplayName("Preço igual não altera nada nem suja o histórico")
    void testPrecoIgualNaoAltera() {
        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY, 2899.00, 0.00, null));
        service.atualizarPrecos();

        ResultadoAtualizacao resultado = service.atualizarPrecos();

        assertEquals(0, resultado.ofertasAlteradas());
        assertTrue(galaxy.getOfertas().get(0).getHistoricoPrecos().isEmpty());
    }

    @Test
    @DisplayName("Ignora preços de produtos parecidos, mas diferentes")
    void testIgnoraOutroProduto() {
        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY + " Edição Especial", 3500.00, 0.00, null));

        ResultadoAtualizacao resultado = service.atualizarPrecos();

        assertEquals(0, galaxy.getQuantidadeOfertas());
        assertEquals(0, resultado.ofertasAlteradas());
    }

    @Test
    @DisplayName("Dado inválido conta como falha e não interrompe os demais")
    void testDadoInvalidoNaoInterrompe() {
        fonte.adicionar(new PrecoExterno("Fast Shop", GALAXY, 0.00, 0.00, null)); // inválido
        fonte.adicionar(new PrecoExterno("Amazon Brasil", GALAXY, 2899.00, 0.00, null));

        ResultadoAtualizacao resultado = service.atualizarPrecos();

        assertEquals(1, resultado.ofertasAlteradas());
        assertEquals(1, resultado.falhas());
        assertEquals(1, galaxy.getQuantidadeOfertas());
    }

    @Test
    @DisplayName("Fonte fora do ar para um produto não impede os outros")
    void testFonteComErro() {
        Produto tv = new Produto("Smart TV 50 4K", "Smart TVs", "desc");
        repositorio.salvar(tv);

        FontePrecos fonteComErro = nome -> {
            if (nome.equals(GALAXY)) {
                throw new IllegalStateException("fora do ar");
            }
            return List.of(new PrecoExterno("Fast Shop", "Smart TV 50 4K", 2199.00, 0.00, null));
        };
        AtualizacaoPrecos serviceComErro = new AtualizacaoPrecos(repositorio, fonteComErro);

        ResultadoAtualizacao resultado = serviceComErro.atualizarPrecos();

        assertEquals(1, resultado.ofertasAlteradas());
        assertEquals(1, resultado.falhas());
        assertEquals(0, galaxy.getQuantidadeOfertas());
        assertEquals(1, tv.getQuantidadeOfertas());
    }
}