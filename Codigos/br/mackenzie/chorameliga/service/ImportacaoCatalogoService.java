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
 * Para de consultar a fonte quando já juntou produtos com oferta suficientes;
 * os produtos sem oferta que foram examinados no caminho também são devolvidos.
 * Não sabe se a fonte é simulada ou o Mercado Livre: só usa o contrato FonteCatalogo.
 */
public class ImportacaoCatalogoService {

    private static final int MAX_COM_OFERTAS = 5;

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
        return importar(nome, null);
    }

    // Com domínio, só importa produtos desse tipo (ex.: MLB-NOTEBOOKS)
    public List<Produto> importar(String nome, String dominio) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Informe o nome do produto.");
        }
        String filtro = (dominio == null || dominio.isBlank()) ? null : dominio.trim();

        List<Produto> importados = new ArrayList<>();
        int comOfertas = 0;
        for (ProdutoExterno externo : fonte.pesquisarProdutos(nome.trim(), filtro)) {
            if (comOfertas >= MAX_COM_OFERTAS) {
                break; // já juntou o suficiente: não gasta mais consultas na fonte
            }
            // sem id externo não dá para reconhecer o produto depois, então ignora
            if (externo.idExterno() == null || externo.idExterno().isBlank()) {
                continue;
            }
            // proteção: uma fonte que ignora o filtro não pode trazer outros tipos
            if (filtro != null && !filtro.equalsIgnoreCase(externo.categoria())) {
                continue;
            }
            Produto produto = repositorio.buscarPorIdExterno(externo.idExterno())
                    .orElseGet(() -> salvarNovo(externo));
            aplicarOfertas(produto);
            if (produto.getQuantidadeOfertas() > 0) {
                comOfertas++;
            }
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