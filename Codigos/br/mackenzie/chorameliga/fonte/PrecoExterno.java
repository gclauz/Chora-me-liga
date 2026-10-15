package br.mackenzie.chorameliga.fonte;

/** Um preço devolvido por uma fonte externa (loja, API, etc.). */
public record PrecoExterno(String loja, String nomeProduto, double preco, double frete, String url) {
}