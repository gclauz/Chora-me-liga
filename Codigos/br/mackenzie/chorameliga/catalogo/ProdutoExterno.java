package br.mackenzie.chorameliga.catalogo;

// Um produto encontrado no catálogo de uma fonte externa (ex.: Mercado Livre).
public record ProdutoExterno(String idExterno, String nome, String categoria) {

}