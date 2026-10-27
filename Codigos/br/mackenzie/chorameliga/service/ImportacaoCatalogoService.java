package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.OfertaExterna;
import br.mackenzie.chorameliga.catalogo.ProdutoExterno;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Importa produtos e ofertas de uma fonte externa para o nosso repositório.
 * Não sabe se a fonte é simulada ou o Mercado Livre: só usa o contrato FonteCatalogo.
 */
public class ImportacaoCatalogoService {

    private final ProdutoRepository repositorio;
    private final FonteCatalogo fonte;

    public ImportacaoCatalogoService(ProdutoRepository repositorio, FonteCatalogo fonte) {
        if (repositorio == null || fonte == null) {
            throw new IllegalArgumentException("Repositório e fonte são obrigatórios.");
        }
        this.repositorio = repositorio;
        this.fonte = fonte;
    }

    public List<Produto> importar(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }

        List<Produto> importados = new ArrayList<>();
        for (ProdutoExterno externo : fonte.pesquisarProdutos(nome.trim())) {
            // sem id externo não dá para reconhecer o produto depois, então ignora
            if (externo.idExterno() == null || externo.idExterno().isBlank()) {
                continue;
            }
            Produto produto = repositorio.buscarPorIdExterno(externo.idExterno())
                    .orElseGet(() -> salvarNovo(externo));
            aplicarOfertas(produto);
            importados.add(produto);
        }
        return importados;
    }

    private Produto salvarNovo(ProdutoExterno externo) {
        Produto produto = new Produto(externo.nome(), externo.categoria(), null, externo.idExterno());
        repositorio.salvar(produto);
        return produto;
    }

    private void aplicarOfertas(Produto produto) {
        List<OfertaExterna> ofertas;
        try {
            ofertas = fonte.buscarOfertas(produto.getIdExterno());
        } catch (RuntimeException e) {
            return; // fonte fora do ar para este produto: mantém o que já temos
        }
        if (ofertas == null) {
            return;
        }
        for (OfertaExterna oferta : ofertas) {
            // link da própria oferta; se a fonte não informou, usa a página do produto
            String link = (oferta.url() != null && !oferta.url().isBlank())
                    ? oferta.url()
                    : produto.getUrlCatalogo();
            try {
                produto.aplicarPreco(oferta.loja(), oferta.preco(), oferta.frete(), link);
            } catch (IllegalArgumentException e) {
                // dado inválido vindo de fora (ex.: preço zero): ignora só esta oferta
            }
        }
    }
}