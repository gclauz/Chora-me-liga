package br.mackenzie.chorameliga.catalogo;

import java.util.List;

// De onde vêm os produtos e as ofertas. Hoje é simulada; depois será o Mercado Livre.
public interface FonteCatalogo {

    // Pesquisa produtos pelo nome
    List<ProdutoExterno> pesquisarProdutos(String nome);

    // Lista as ofertas (loja, preço e frete) de um produto, pelo id externo
    List<OfertaExterna> buscarOfertas(String idExterno);
}