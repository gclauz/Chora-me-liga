package br.mackenzie.chorameliga.reconhecimento;

// O que a IA identificou na foto: o nome para pesquisar e, se souber, o tipo de produto (domínio).
public record ProdutoReconhecido(String nome, String dominio) {

    public ProdutoReconhecido {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do produto reconhecido é obrigatório.");
        }
        nome = nome.trim();
        dominio = (dominio == null || dominio.isBlank()) ? null : dominio.trim();
    }
}