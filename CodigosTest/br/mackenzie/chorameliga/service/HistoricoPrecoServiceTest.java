package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.historico.HistoricoPrecosMemoria;
import br.mackenzie.chorameliga.historico.RegistroPreco;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoricoPrecoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-15T10:00:00Z");

    private HistoricoPrecosMemoria banco;
    private HistoricoPrecoService service;
    private Produto produto;

    @BeforeEach
    void setUp() {
        banco = new HistoricoPrecosMemoria();
        service = new HistoricoPrecoService(banco, Clock.fixed(AGORA, ZoneOffset.UTC));
        produto = new Produto("Galaxy S23", "MLB-CELLPHONES", null, "MLB1");
        produto.adicionarOferta(new Oferta("Loja A", 3000.00, 0));
        produto.adicionarOferta(new Oferta("Loja B", 3100.00, 10));
    }

    @Test
    void testPrimeiraVezGravaTodasAsLojas() {
        service.registrar(produto);

        assertEquals(List.of(
                new RegistroPreco("Loja A", 3000.00, AGORA),
                new RegistroPreco("Loja B", 3100.00, AGORA)), banco.listar("MLB1"));
    }

    @Test
    void testPrecoIgualNaoRepete() {
        service.registrar(produto);
        // Caso 1: mesma pesquisa de novo, sem mudança de preço
        service.registrar(produto);
        // Caso 2: o servidor reiniciou (produto novo na memória), mesmo preço e loja escrita em maiúsculas
        Produto depoisDeReiniciar = new Produto("Galaxy S23", "MLB-CELLPHONES", null, "MLB1");
        depoisDeReiniciar.adicionarOferta(new Oferta("LOJA A", 3000.00, 0));
        service.registrar(depoisDeReiniciar);

        assertEquals(2, banco.listar("MLB1").size());
    }

    @Test
    void testSoGravaALojaQueMudou() {
        service.registrar(produto);
        produto.aplicarPreco("Loja A", 2899.00, 0, null);

        service.registrar(produto);

        List<RegistroPreco> registros = banco.listar("MLB1");
        assertEquals(3, registros.size());
        assertEquals(new RegistroPreco("Loja A", 2899.00, AGORA), registros.get(2));
    }

    @Test
    void testVoltarAoPrecoAntigoTambemGrava() {
        service.registrar(produto);
        produto.aplicarPreco("Loja A", 2899.00, 0, null);
        service.registrar(produto);
        produto.aplicarPreco("Loja A", 3000.00, 0, null);

        service.registrar(produto);

        // compara com o último preço da loja, não com qualquer preço já visto
        assertEquals(4, banco.listar("MLB1").size());
    }

    @Test
    void testProdutoSemIdExternoNaoGrava() {
        Produto local = new Produto("Produto local", null, null);
        local.adicionarOferta(new Oferta("Loja A", 10.00, 0));

        service.registrar(local);

        assertTrue(service.listar(local).isEmpty());
    }

    @Test
    void testBancoForaDoArNaoAtrapalhaAPesquisa() {
        banco.ficarForaDoAr();

        // Caso 1: gravar não lança erro
        assertDoesNotThrow(() -> service.registrar(produto));
        // Caso 2: ler devolve histórico vazio
        assertTrue(service.listar(produto).isEmpty());
    }

    @Test
    void testParametrosObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> new HistoricoPrecoService(null));
    }
}
