package br.mackenzie.chorameliga.dto;

import java.util.List;

// Histórico de preços de uma loja para um produto, do ponto mais antigo para o mais recente.
public record HistoricoLoja(String loja, List<PontoPreco> pontos) {
}
