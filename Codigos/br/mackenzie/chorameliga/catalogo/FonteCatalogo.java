package br.mackenzie.chorameliga.catalogo;

import java.util.List;

// De onde vêm os produtos e as ofertas. Hoje é simulada ou o Mercado Livre.
public interface FonteCatalogo {

    // Pesquisa produtos pelo nome
    List<ProdutoExterno> pesquisarProdutos(String nome);

    // Pesquisa limitada a um tipo de produto (domínio), ex.: MLB-NOTEBOOKS.
    // Fontes que não sabem filtrar ignoram o domínio e devolvem tudo.
    default List<ProdutoExterno> pesquisarProdutos(String nome, String dominio) {
        return pesquisarProdutos(nome);
    }

    // Lista as ofertas (loja, preço e frete) de um produto, pelo id externo
    List<OfertaExterna> buscarOfertas(String idExterno);
}