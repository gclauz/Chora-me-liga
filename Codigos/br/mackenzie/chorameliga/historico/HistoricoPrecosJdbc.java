package br.mackenzie.chorameliga.historico;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

// Histórico guardado no banco de dados (tabela historico_preco, criada pelo schema.sql).
// O mesmo SQL funciona no H2 (computador e testes) e no PostgreSQL (nuvem).
public class HistoricoPrecosJdbc implements HistoricoPrecos {

    private final JdbcTemplate jdbc;

    public HistoricoPrecosJdbc(JdbcTemplate jdbc) {
        if (jdbc == null) {
            throw new IllegalArgumentException("O acesso ao banco é obrigatório.");
        }
        this.jdbc = jdbc;
    }

    @Override
    public void registrar(String idExternoProduto, RegistroPreco registro) {
        jdbc.update("INSERT INTO historico_preco (id_externo_produto, loja, preco, registrado_em) VALUES (?, ?, ?, ?)",
                idExternoProduto,
                registro.loja(),
                BigDecimal.valueOf(registro.preco()).setScale(2, RoundingMode.HALF_UP),
                registro.data().atOffset(ZoneOffset.UTC));
    }

    @Override
    public List<RegistroPreco> listar(String idExternoProduto) {
        return jdbc.query(
                "SELECT loja, preco, registrado_em FROM historico_preco WHERE id_externo_produto = ? ORDER BY registrado_em, id",
                (linha, numero) -> new RegistroPreco(
                        linha.getString("loja"),
                        linha.getBigDecimal("preco").doubleValue(),
                        linha.getObject("registrado_em", OffsetDateTime.class).toInstant()),
                idExternoProduto);
    }
}
