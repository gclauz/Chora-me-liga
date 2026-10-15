package br.mackenzie.chorameliga.service;

/** Resumo de uma rodada de atualização de preços. */
public record ResultadoAtualizacao(int ofertasAlteradas, int falhas) {
}