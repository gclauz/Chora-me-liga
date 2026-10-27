package br.mackenzie.chorameliga.catalogo;

// Uma oferta de um produto em uma loja, como vem de uma fonte externa.
// A url é o link da própria oferta; pode ser null quando a fonte não informa.
public record OfertaExterna(String loja, double preco, double frete, String url) {

    // Para quem não tem link da oferta (ex.: os dados de exemplo)
    public OfertaExterna(String loja, double preco, double frete) {
        this(loja, preco, frete, null);
    }
}