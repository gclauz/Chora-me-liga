package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.dto.ProdutoResumo;
import br.mackenzie.chorameliga.dto.ResultadoPesquisaFoto;
import br.mackenzie.chorameliga.reconhecimento.FormatoImagem;
import br.mackenzie.chorameliga.reconhecimento.ProdutoReconhecido;
import br.mackenzie.chorameliga.reconhecimento.ReconhecedorFoto;

import java.util.List;

// UC02 - Pesquisar produto por foto: reconhece o produto e faz a mesma pesquisa do UC01.
public class PesquisaPorFotoService {

    private final ReconhecedorFoto reconhecedor;
    private final ProdutoService produtoService;

    public PesquisaPorFotoService(ReconhecedorFoto reconhecedor, ProdutoService produtoService) {
        if (reconhecedor == null || produtoService == null) {
            throw new IllegalArgumentException("O reconhecedor e o serviço de produtos são obrigatórios.");
        }
        this.reconhecedor = reconhecedor;
        this.produtoService = produtoService;
    }

    public ResultadoPesquisaFoto pesquisar(byte[] imagem) {
        validar(imagem);

        ProdutoReconhecido reconhecido = reconhecedor.reconhecer(imagem)
                .orElseThrow(FotoNaoReconhecida::new); // REQ 5

        return new ResultadoPesquisaFoto(reconhecido.nome(), reconhecido.dominio(),
                pesquisarProdutos(reconhecido));
    }

    // A categoria vem de um palpite da IA: se ela não servir (formato inválido ou nenhum
    // resultado nela), pesquisa só pelo nome para não perder produtos por causa do palpite.
    private List<ProdutoResumo> pesquisarProdutos(ProdutoReconhecido reconhecido) {
        if (reconhecido.dominio() != null) {
            try {
                List<ProdutoResumo> naCategoria =
                        produtoService.pesquisarResumos(reconhecido.nome(), reconhecido.dominio());
                if (!naCategoria.isEmpty()) {
                    return naCategoria;
                }
            } catch (IllegalArgumentException e) {
                // categoria em formato inválido: ignora e segue só com o nome
            }
        }
        return produtoService.pesquisarResumos(reconhecido.nome());
    }

    private void validar(byte[] imagem) {
        if (imagem == null || imagem.length == 0) {
            throw new IllegalArgumentException("Envie uma foto do produto.");
        }
        if (imagem.length > FormatoImagem.TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("A foto deve ter no máximo 5 MB.");
        }
        if (FormatoImagem.detectar(imagem).isEmpty()) {
            throw new IllegalArgumentException("Formato de imagem não aceito. Use JPG, PNG ou WEBP.");
        }
    }
}
