package br.mackenzie.chorameliga.fonte;

import java.util.ArrayList;
import java.util.List;

/** Fonte simulada, com dados fixos. Serve para desenvolver e testar sem depender de API. */
public class FontePrecosFake implements FontePrecos {

    private final List<PrecoExterno> precos = new ArrayList<>();

    public static FontePrecosFake comDadosDeExemplo() {
        FontePrecosFake fonte = new FontePrecosFake();
        String galaxy = "Smartphone Samsung Galaxy S23 128GB";
        fonte.adicionar(new PrecoExterno("Amazon Brasil", galaxy, 2899.00, 0.00, "https://www.amazon.com.br"));
        fonte.adicionar(new PrecoExterno("Magazine Luiza", galaxy, 2949.00, 15.00, "https://www.magazineluiza.com.br"));
        fonte.adicionar(new PrecoExterno("Casas Bahia", galaxy, 3099.00, 0.00, "https://www.casasbahia.com.br"));
        fonte.adicionar(new PrecoExterno("Fast Shop", galaxy, 3199.00, 22.00, "https://www.fastshop.com.br"));
        return fonte;
    }

    // Se a loja já tem preço para esse produto, o novo substitui o antigo (simula a loja mudando o preço)
    public void adicionar(PrecoExterno novo) {
        precos.removeIf(p -> p.loja().equalsIgnoreCase(novo.loja())
                && p.nomeProduto().equalsIgnoreCase(novo.nomeProduto()));
        precos.add(novo);
    }

    @Override
    public List<PrecoExterno> buscarPorNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return List.of();
        }
        String busca = nome.toLowerCase();
        List<PrecoExterno> resultado = new ArrayList<>();
        for (PrecoExterno p : precos) {
            if (p.nomeProduto().toLowerCase().contains(busca)) {
                resultado.add(p);
            }
        }
        return resultado;
    }
}