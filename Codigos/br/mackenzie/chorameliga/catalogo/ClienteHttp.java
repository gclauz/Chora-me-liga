package br.mackenzie.chorameliga.catalogo;

import java.util.Map;

// Faz as chamadas HTTP. Existe como interface para os testes não dependerem da internet.
public interface ClienteHttp {

    // GET com token (se o token for null, não envia o cabeçalho de autorização)
    String get(String url, String token);

    // POST com corpo no formato de formulário (application/x-www-form-urlencoded)
    String postForm(String url, String corpo);

    // POST com corpo JSON e cabeçalhos extras (ex.: a chave de uma API).
    // Tem implementação padrão para quem não precisa dele não ser obrigado a implementar.
    default String postJson(String url, String corpoJson, Map<String, String> cabecalhos) {
        throw new UnsupportedOperationException("Este cliente HTTP não envia JSON.");
    }
}