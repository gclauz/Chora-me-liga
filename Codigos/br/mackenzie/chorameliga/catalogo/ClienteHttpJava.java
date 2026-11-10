package br.mackenzie.chorameliga.catalogo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

// Implementação real, com o HttpClient que já vem no Java.
public class ClienteHttpJava implements ClienteHttp {

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String get(String url, String token) {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET();
        if (token != null) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return enviar(requisicao.build());
    }

    @Override
    public String postForm(String url, String corpo) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        return enviar(requisicao);
    }

    // Prazo maior que o das outras chamadas (enviar uma foto e a IA analisar demora mais),
    // mas curto o bastante para o usuário não ficar preso na tela esperando.
    @Override
    public String postJson(String url, String corpoJson, Map<String, String> cabecalhos) {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(corpoJson, StandardCharsets.UTF_8));
        if (cabecalhos != null) {
            cabecalhos.forEach(requisicao::header);
        }
        return enviar(requisicao.build());
    }

    private String enviar(HttpRequest requisicao) {
        try {
            // lê a resposta como UTF-8, o que evita os acentos quebrados que vimos no PowerShell
            HttpResponse<String> resposta = cliente.send(requisicao,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resposta.statusCode() < 200 || resposta.statusCode() >= 300) {
                throw new RespostaHttpException(resposta.statusCode(), resposta.body());
            }
            return resposta.body();
        } catch (IOException e) {
            throw new FonteIndisponivelException("Falha de rede ao chamar a API.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FonteIndisponivelException("Chamada à API interrompida.", e);
        }
    }
}