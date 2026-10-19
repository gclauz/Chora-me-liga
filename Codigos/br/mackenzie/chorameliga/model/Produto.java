package br.mackenzie.chorameliga.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

// Representa um produto que pode ser pesquisado.

public class Produto {

    private final String nome;
    private final String categoria;
    private final String descricao;
    private Long id;
    private final String idExterno; 
    private final List<Oferta> ofertas = new ArrayList<>();

    public Produto(String nome, String categoria, String descricao) {
        this(nome, categoria, descricao, null);
    }

    public Produto(String nome, String categoria, String descricao, String idExterno) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        this.nome = nome;
        this.categoria = categoria;
        this.descricao = descricao;
        this.idExterno = (idExterno == null || idExterno.isBlank()) ? null : idExterno.trim();
    }

    public void adicionarOferta(Oferta oferta) {
        if (oferta == null) {
            throw new IllegalArgumentException("A oferta é obrigatória.");
        }
        ofertas.add(oferta);
    }

    public boolean aplicarPreco(String loja, double preco, double frete, String url) {
        for (Oferta o : ofertas) {
            if (o.getLoja().equalsIgnoreCase(loja)) {
                if (o.getPreco() == preco) {
                    return false;
                }
                o.atualizarPreco(preco);
                return true;
            }
        }
        ofertas.add(new Oferta(loja, preco, frete, url));
        return true;
    }

    // Menor preço considerando o frete 
    public Optional<Oferta> getOfertaMaisBarata() {
        Oferta maisBarata = null;
        for (Oferta o : ofertas) {
            if (maisBarata == null || o.isMaisBarataQue(maisBarata)) {
                maisBarata = o;
            }
        }
        return Optional.ofNullable(maisBarata);
    }

    public int getQuantidadeOfertas() {
        return ofertas.size();
    }

    public String getNome() {
        return nome;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescricao() {
        return descricao;
    }

    public List<Oferta> getOfertas() {
        return Collections.unmodifiableList(ofertas);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public String getIdExterno() {
        return idExterno;
    }

    public String getUrlCatalogo() {
        if (idExterno == null) {
            return null;
        }
        return "https://www.mercadolivre.com.br/p/" + idExterno;
    }
}