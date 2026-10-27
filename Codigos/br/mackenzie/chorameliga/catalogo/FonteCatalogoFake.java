package br.mackenzie.chorameliga.catalogo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Fonte simulada, com dados fixos. Serve para desenvolver e testar sem depender de API.
public class FonteCatalogoFake implements FonteCatalogo {

    private final Map<String, ProdutoExterno> produtos = new LinkedHashMap<>();
    private final Map<String, List<OfertaExterna>> ofertas = new HashMap<>();

    public static FonteCatalogoFake comDadosDeExemplo() {
        FonteCatalogoFake fonte = new FonteCatalogoFake();
        fonte.adicionar(
                new ProdutoExterno("MLB39957733", "Smartphone Samsung Galaxy S23 256gb Preto", "MLB-CELLPHONES"),
                List.of(new OfertaExterna("Loja Exemplo 1", 3499.00, 0.00),
                        new OfertaExterna("Loja Exemplo 2", 4299.00, 0.00),
                        new OfertaExterna("Loja Exemplo 3", 4499.00, 0.00)));
        fonte.adicionar(
                new ProdutoExterno("MLB47311354", "Samsung Galaxy S23 256gb Violeta Cor Lavanda", "MLB-CELLPHONES"),
                List.of(new OfertaExterna("Loja Exemplo 1", 3599.00, 15.00)));
        return fonte;
    }

    public void adicionar(ProdutoExterno produto, List<OfertaExterna> ofertasDoProduto) {
        produtos.put(produto.idExterno(), produto);
        ofertas.put(produto.idExterno(), new ArrayList<>(ofertasDoProduto));
    }

    // Todas as palavras digitadas precisam aparecer no nome, em qualquer ordem
    @Override
    public List<ProdutoExterno> pesquisarProdutos(String nome) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }
        String[] termos = nome.toLowerCase().trim().split("\\s+");
        List<ProdutoExterno> encontrados = new ArrayList<>();
        for (ProdutoExterno p : produtos.values()) {
            String alvo = p.nome().toLowerCase();
            boolean contemTodos = true;
            for (String termo : termos) {
                if (!alvo.contains(termo)) {
                    contemTodos = false;
                    break;
                }
            }
            if (contemTodos) {
                encontrados.add(p);
            }
        }
        return encontrados;
    }

    // Mesma pesquisa, mas só com os produtos do domínio pedido (a "categoria" do produto externo)
    @Override
    public List<ProdutoExterno> pesquisarProdutos(String nome, String dominio) {
        List<ProdutoExterno> todos = pesquisarProdutos(nome);
        if (dominio == null || dominio.isBlank()) {
            return todos;
        }
        List<ProdutoExterno> doDominio = new ArrayList<>();
        for (ProdutoExterno p : todos) {
            if (dominio.trim().equalsIgnoreCase(p.categoria())) {
                doDominio.add(p);
            }
        }
        return doDominio;
    }

    @Override
    public List<OfertaExterna> buscarOfertas(String idExterno) {
        if (idExterno == null) {
            return List.of();
        }
        return ofertas.getOrDefault(idExterno, List.of());
    }
}