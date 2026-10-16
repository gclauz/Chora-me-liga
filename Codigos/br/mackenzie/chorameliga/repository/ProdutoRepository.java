package br.mackenzie.chorameliga.repository;

import br.mackenzie.chorameliga.model.Produto;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;


// Guarda os produtos e permite pesquisar por nome (UC01).

public class ProdutoRepository {

    private final List<Produto> produtos = new ArrayList<>();
    private long proximoId = 1;

    public void salvar(Produto produto) {
        if (produto == null) {
            throw new IllegalArgumentException("O produto é obrigatório.");
        }
        if (produto.getId() == null) {
            produto.setId(proximoId++);
        }
        produtos.add(produto);
    }

    public Optional<Produto> buscarPorId(long id) {
        for (Produto p : produtos) {
            if (p.getId() != null && p.getId() == id) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    // Busca ignorando maiúsculas e acentos; todas as palavras digitadas precisam aparecer no nome
    public List<Produto> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }
        String[] termos = normalizar(nome).trim().split("\\s+");

        List<Produto> encontrados = new ArrayList<>();
        for (Produto p : produtos) {
            String alvo = normalizar(p.getNome());
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

    public List<Produto> listarTodos() {
        return Collections.unmodifiableList(produtos);
    }

    private String normalizar(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
    }
}