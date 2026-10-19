package br.mackenzie.chorameliga.catalogo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

// Cliente HTTP de mentira: devolve respostas prontas e registra o que foi chamado.
public class ClienteHttpFake implements ClienteHttp {

    public final List<String> chamadas = new ArrayList<>();
    public final Deque<String> respostas = new ArrayDeque<>();
    public String ultimoCorpo;

    @Override
    public String get(String url, String token) {
        chamadas.add("GET " + url);
        return proxima();
    }

    @Override
    public String postForm(String url, String corpo) {
        chamadas.add("POST " + url);
        ultimoCorpo = corpo;
        return proxima();
    }

    private String proxima() {
        if (respostas.isEmpty()) {
            throw new FonteIndisponivelException("Sem resposta simulada.");
        }
        return respostas.poll();
    }
}