package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.OfertaDetalhe;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;

import java.util.Comparator;
import java.util.List;

/*coordena o repository e o model */
public class ProdutoService {

    private final ProdutoRepository repositorio;

    public ProdutoService(ProdutoRepository repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        this.repositorio = repositorio;
    }

    // UC03 - Acessar detalhes do produto
    public DetalhesProduto buscarDetalhes(long id) {
        Produto produto = repositorio.buscarPorId(id)
                .orElseThrow(() -> new ProdutoNaoEncontrado(id));

        // da oferta mais barata para a mais cara, considerando o frete
        List<OfertaDetalhe> ofertas = produto.getOfertas().stream()
                .sorted(Comparator.comparingDouble(Oferta::calcularPrecoTotal))
                .map(this::paraDetalhe)
                .toList();

        Double menorPrecoAtual = produto.getOfertaMaisBarata()
                .map(Oferta::getPreco)
                .orElse(null);

        Double menorPrecoHistorico = produto.getOfertas().stream()
                .map(Oferta::getMenorPrecoHistorico)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new DetalhesProduto(
                produto.getId(),
                produto.getNome(),
                produto.getCategoria(),
                produto.getDescricao(),
                ofertas,
                menorPrecoAtual,
                menorPrecoHistorico
        );
    }

    private OfertaDetalhe paraDetalhe(Oferta o) {
        return new OfertaDetalhe(
                o.getLoja(),
                o.getPreco(),
                o.getFrete(),
                o.calcularPrecoTotal(),
                o.temFreteGratis(),
                o.calcularParcela(10),
                o.getUrl()
        );
    }
}