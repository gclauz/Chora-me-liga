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
import java.util.regex.Pattern;

public class ProdutoService {

    // um domínio é só letras, números, hífen e sublinhado (ex.: MLB-NOTEBOOKS)
    private static final Pattern FORMATO_DOMINIO = Pattern.compile("[A-Za-z0-9_-]{1,80}");

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
        return pesquisarPorNome(nome, null);
    }

    // UC01 limitado a um tipo de produto (domínio); sem domínio, pesquisa em tudo
    public synchronized List<Produto> pesquisarPorNome(String nome, String dominio) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }
        String filtro = normalizarDominio(dominio);
        String termo = nome.trim();

        List<Produto> importados = List.of();
        if (importacao != null) {
            try {
                importados = importacao.importar(termo, filtro);
            } catch (RuntimeException e) {
                // fonte fora do ar ou resposta inesperada: segue com o que já temos
            }
        }

        // primeiro o que já estava salvo, depois os importados que ainda não apareceram
        List<Produto> resultado = new ArrayList<>();
        for (Produto p : repositorio.buscarPorNome(termo)) {
            if (dominioConfere(p, filtro)) {
                resultado.add(p);
            }
        }
        for (Produto p : importados) {
            if (dominioConfere(p, filtro) && !resultado.contains(p)) {
                resultado.add(p);
            }
        }
        return resultado;
    }

    // UC01 para a API: a mesma pesquisa, já convertida em dados prontos para a tela.
    // Devolve também os produtos sem oferta; a tela decide como mostrá-los.
    public synchronized List<ProdutoResumo> pesquisarResumos(String nome) {
        return pesquisarResumos(nome, null);
    }

    public synchronized List<ProdutoResumo> pesquisarResumos(String nome, String dominio) {
        return pesquisarPorNome(nome, dominio).stream()
                .map(this::paraResumo)
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

    // domínio em branco = sem filtro; fora do formato = erro (vira 400 na API)
    private String normalizarDominio(String dominio) {
        if (dominio == null || dominio.isBlank()) {
            return null;
        }
        String limpo = dominio.trim();
        if (!FORMATO_DOMINIO.matcher(limpo).matches()) {
            throw new IllegalArgumentException("Categoria inválida.");
        }
        return limpo;
    }

    // a categoria do produto guarda o domínio da fonte (ex.: MLB-NOTEBOOKS)
    private boolean dominioConfere(Produto produto, String filtro) {
        return filtro == null || filtro.equalsIgnoreCase(produto.getCategoria());
    }

    private ProdutoResumo paraResumo(Produto p) {
        Double menorPreco = p.getOfertaMaisBarata()
                .map(Oferta::getPreco)
                .orElse(null);
        return new ProdutoResumo(p.getId(), p.getNome(), p.getCategoria(),
                p.getQuantidadeOfertas(), menorPreco, p.getUrlCatalogo());
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