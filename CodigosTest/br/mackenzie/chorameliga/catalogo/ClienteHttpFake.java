package br.mackenzie.chorameliga.catalogo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Cliente HTTP de mentira: devolve respostas prontas e registra o que foi chamado.
public class ClienteHttpFake implements ClienteHttp {

    public final List<String> chamadas = new ArrayList<>();
    public final Deque<String> respostas = new ArrayDeque<>();
    private final Map<String, String> regras = new LinkedHashMap<>();
    private final Set<String> falhas = new HashSet<>();
    public String ultimoCorpo;
    public String ultimoToken;

    // Quando a URL contiver o trecho, devolve esta resposta
    public void responder(String trechoDaUrl, String resposta) {
        regras.put(trechoDaUrl, resposta);
    }

    // Quando a URL contiver o trecho, simula a API fora do ar
    public void falharEm(String trechoDaUrl) {
        falhas.add(trechoDaUrl);
    }

    @Override
    public String get(String url, String token) {
        chamadas.add("GET " + url);
        ultimoToken = token;
        return resposta(url);
    }

    @Override
    public String postForm(String url, String corpo) {
        chamadas.add("POST " + url);
        ultimoCorpo = corpo;
        return resposta(url);
    }

    private String resposta(String url) {
        for (String trecho : falhas) {
            if (url.contains(trecho)) {
                throw new FonteIndisponivelException("Falha simulada em " + trecho);
            }
        }

        for (Map.Entry<String, Integer> regra : status.entrySet()) {
            if (url.contains(regra.getKey())) {
                throw new RespostaHttpException(regra.getValue());
            }
        }

        for (Map.Entry<String, String> regra : regras.entrySet()) {
            if (url.contains(regra.getKey())) {
                return regra.getValue();
            }
        }
        if (respostas.isEmpty()) {
            throw new FonteIndisponivelException("Sem resposta simulada.");
        }
        return respostas.poll();
    }

    private final Map<String, Integer> status = new LinkedHashMap<>();

    // Quando a URL contiver o trecho, simula a API respondendo com este status de erro
    public void responderComStatus(String trechoDaUrl, int codigo) {
        status.put(trechoDaUrl, codigo);
    }

}