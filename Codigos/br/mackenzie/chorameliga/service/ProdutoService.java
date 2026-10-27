package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.OfertaDetalhe;
import br.mackenzie.chorameliga.dto.ProdutoResumo;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ProdutoService {

    private final ProdutoRepository repositorio;
    private final ImportacaoCatalogoService importacao; // pode ser null (só busca no repositório)

    public ProdutoService(ProdutoRepository repositorio) {
        this(repositorio, null);
    }

    public ProdutoService(ProdutoRepository repositorio, ImportacaoCatalogoService importacao) {
        if (repositorio == null) {
            throw new IllegalArgumentException("O repositório é obrigatório.");
        }
        this.repositorio = repositorio;
        this.importacao = importacao;
    }

    // UC01 - Pesquisar produto manualmente
    public synchronized List<Produto> pesquisarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }
        String termo = nome.trim();

        List<Produto> importados = List.of();
        if (importacao != null) {
            try {
                importados = importacao.importar(termo);
            } catch (RuntimeException e) {
                // fonte fora do ar ou resposta inesperada: segue com o que já temos
            }
        }

        // primeiro o que já estava salvo, depois os importados que ainda não apareceram
        List<Produto> resultado = new ArrayList<>(repositorio.buscarPorNome(termo));
        for (Produto p : importados) {
            if (!resultado.contains(p)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    // UC01 para a API: a mesma pesquisa, já convertida em dados prontos para a tela
    public synchronized List<ProdutoResumo> pesquisarResumos(String nome) {
        return pesquisarPorNome(nome).stream()
                .map(this::paraResumo)
                .sorted(Comparator.comparing(resumo -> resumo.quantidadeOfertas() == 0))
                .toList();
    }
    // UC03 - Acessar detalhes do produto
    public synchronized DetalhesProduto buscarDetalhes(long id) {
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

    private ProdutoResumo paraResumo(Produto p) {
        Double menorPreco = p.getOfertaMaisBarata()
                .map(Oferta::getPreco)
                .orElse(null);
        return new ProdutoResumo(p.getId(), p.getNome(), p.getCategoria(),
                p.getQuantidadeOfertas(), menorPreco);
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