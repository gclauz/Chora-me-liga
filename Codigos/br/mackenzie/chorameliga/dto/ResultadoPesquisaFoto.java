package br.mackenzie.chorameliga.dto;

import java.util.List;

// Resposta da pesquisa por foto: o que foi reconhecido e os produtos encontrados com esse nome.
public record ResultadoPesquisaFoto(
        String nomeReconhecido,
        String dominioReconhecido, // null se a IA não soube a categoria
        List<ProdutoResumo> produtos
) {
}
