package br.mackenzie.chorameliga.dto;

/** Uma linha da lista "Ofertas disponíveis" da tela de detalhes. */
public record OfertaDetalhe(
        String loja,
        double preco,
        double frete,
        double precoTotal,
        boolean freteGratis,
        double parcela10x,
        String url
) {
}