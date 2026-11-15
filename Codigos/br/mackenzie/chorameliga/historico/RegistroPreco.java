package br.mackenzie.chorameliga.historico;

import java.time.Instant;

// Um preço de uma loja num momento: um ponto do histórico de preços (UC09).
public record RegistroPreco(String loja, double preco, Instant data) {
}
