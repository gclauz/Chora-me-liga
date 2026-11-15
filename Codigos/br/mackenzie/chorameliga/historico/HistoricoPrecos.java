package br.mackenzie.chorameliga.historico;

import java.util.List;

// Onde o histórico de preços fica guardado. O produto é identificado pelo id externo
// (Mercado Livre), porque o nosso id é gerado de novo sempre que o servidor reinicia.
public interface HistoricoPrecos {

    void registrar(String idExternoProduto, RegistroPreco registro);

    // do registro mais antigo para o mais recente
    List<RegistroPreco> listar(String idExternoProduto);
}
