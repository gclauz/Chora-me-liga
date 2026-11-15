package br.mackenzie.chorameliga.dto;

import java.time.Instant;

// Um ponto do gráfico de histórico: a partir desta data a loja passou a cobrar este preço.
public record PontoPreco(Instant data, double preco) {
}
