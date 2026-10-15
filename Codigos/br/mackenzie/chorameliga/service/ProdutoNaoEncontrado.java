package br.mackenzie.chorameliga.service;

public class ProdutoNaoEncontrado extends RuntimeException {

    public ProdutoNaoEncontrado(long id) {
        super("Produto não encontrado: " + id);
    }
}