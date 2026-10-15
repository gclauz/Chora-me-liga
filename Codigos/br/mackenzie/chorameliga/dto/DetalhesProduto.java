package br.mackenzie.chorameliga.dto;

import java.util.List;

/** Tudo que a tela de detalhes do produto precisa mostrar. */
public record DetalhesProduto(
        Long id,
        String nome,
        String categoria,
        String descricao,
        List<OfertaDetalhe> ofertas,
        Double menorPrecoAtual,      
        Double menorPrecoHistorico   
) {
}