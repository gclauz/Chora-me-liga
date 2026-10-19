package br.mackenzie.chorameliga.catalogo;

// Faz as chamadas HTTP. Existe como interface para os testes não dependerem da internet.
public interface ClienteHttp {

    // GET com token (se o token for null, não envia o cabeçalho de autorização)
    String get(String url, String token);

    // POST com corpo no formato de formulário (application/x-www-form-urlencoded)
    String postForm(String url, String corpo);
}