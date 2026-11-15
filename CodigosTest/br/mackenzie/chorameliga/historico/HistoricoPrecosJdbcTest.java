package br.mackenzie.chorameliga.historico;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Usa um H2 em memória com o mesmo schema.sql da aplicação.
class HistoricoPrecosJdbcTest {

    private EmbeddedDatabase banco;
    private HistoricoPrecosJdbc historico;

    @BeforeEach
    void setUp() {
        banco = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();
        historico = new HistoricoPrecosJdbc(new JdbcTemplate(banco));
    }

    @AfterEach
    void tearDown() {
        banco.shutdown();
    }

    @Test
    void testGravaELeEmOrdemDeData() {
        Instant ontem = Instant.parse("2026-11-14T12:00:00Z");
        Instant hoje = Instant.parse("2026-11-15T12:00:00Z");
        historico.registrar("MLB1", new RegistroPreco("Loja A", 2999.90, hoje));
        historico.registrar("MLB1", new RegistroPreco("Loja A", 3199.00, ontem));
        historico.registrar("MLB2", new RegistroPreco("Loja B", 10.00, hoje));

        List<RegistroPreco> registros = historico.listar("MLB1");

        // Caso 1: só os do produto pedido, do mais antigo para o mais recente
        assertEquals(2, registros.size());
        assertEquals(new RegistroPreco("Loja A", 3199.00, ontem), registros.get(0));
        // Caso 2: preço e data voltam iguais ao que foi gravado
        assertEquals(new RegistroPreco("Loja A", 2999.90, hoje), registros.get(1));
    }

    @Test
    void testProdutoSemHistorico() {
        assertTrue(historico.listar("MLB-NADA").isEmpty());
    }

    @Test
    void testSchemaPodeRodarDeNovo() throws Exception {
        historico.registrar("MLB1", new RegistroPreco("Loja A", 10.00, Instant.parse("2026-11-15T12:00:00Z")));

        // o schema.sql roda toda vez que o servidor sobe: não pode falhar nem apagar o que já foi gravado
        try (var conexao = banco.getConnection()) {
            ScriptUtils.executeSqlScript(conexao, new ClassPathResource("schema.sql"));
        }

        assertEquals(1, historico.listar("MLB1").size());
    }
}
