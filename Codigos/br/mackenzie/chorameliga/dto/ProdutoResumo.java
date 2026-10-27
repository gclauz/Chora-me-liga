package br.mackenzie.chorameliga.dto;

// Um item da lista de resultados da pesquisa (a tela de resultados do wireframe).
public record ProdutoResumo(
        Long id,
        String nome,
        String categoria,
        int quantidadeOfertas,
        Double menorPreco // null se o produto não tem ofertas
) {
}