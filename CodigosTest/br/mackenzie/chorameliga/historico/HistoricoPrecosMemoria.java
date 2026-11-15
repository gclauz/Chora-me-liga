package br.mackenzie.chorameliga.historico;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Histórico em memória, para testar sem banco de dados. Pode ser ligado para falhar como um banco fora do ar.
public class HistoricoPrecosMemoria implements HistoricoPrecos {

    private final Map<String, List<RegistroPreco>> registros = new HashMap<>();
    private boolean foraDoAr;

    @Override
    public void registrar(String idExternoProduto, RegistroPreco registro) {
        verificar();
        registros.computeIfAbsent(idExternoProduto, k -> new ArrayList<>()).add(registro);
    }

    @Override
    public List<RegistroPreco> listar(String idExternoProduto) {
        verificar();
        return List.copyOf(registros.getOrDefault(idExternoProduto, List.of()));
    }

    public void ficarForaDoAr() {
        foraDoAr = true;
    }

    private void verificar() {
        if (foraDoAr) {
            throw new IllegalStateException("banco fora do ar");
        }
    }
}
