package br.mackenzie.chorameliga.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa a oferta de um produto em uma loja.
 * Guarda o preço atual, o frete e os preços antigos (histórico de preços).
 */
public class Oferta {

    private final String loja;
    private double preco;
    private double frete;
    private final List<Double> historicoPrecos = new ArrayList<>();

    public Oferta(String loja, double preco, double frete) {
        if (loja == null || loja.isBlank()) {
            throw new IllegalArgumentException("A loja é obrigatória.");
        }
        if (preco <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        if (frete < 0) {
            throw new IllegalArgumentException("O frete não pode ser negativo.");
        }
        this.loja = loja;
        this.preco = preco;
        this.frete = frete;
    }

    // UC06 - Atualizar preços: guarda o preço antigo antes de trocar
    public void atualizarPreco(double novoPreco) {
        if (novoPreco <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        historicoPrecos.add(preco);
        preco = novoPreco;
    }

    // UC09 - Consultar histórico de preços: menor valor já registrado
    public double getMenorPrecoHistorico() {
        double menor = preco;
        for (double p : historicoPrecos) {
            if (p < menor) {
                menor = p;
            }
        }
        return menor;
    }

    public double calcularPrecoTotal() {
        return preco + frete;
    }

    public boolean temFreteGratis() {
        return frete == 0;
    }

    public double calcularParcela(int parcelas) {
        if (parcelas < 1 || parcelas > 12) {
            throw new IllegalArgumentException("O número de parcelas deve ser entre 1 e 12.");
        }
        return preco / parcelas;
    }

    // Compara considerando o frete, que é o que o cliente paga de verdade
    public boolean isMaisBarataQue(Oferta outra) {
        return calcularPrecoTotal() < outra.calcularPrecoTotal();
    }

    public String getLoja() {
        return loja;
    }

    public double getPreco() {
        return preco;
    }

    public double getFrete() {
        return frete;
    }

    public List<Double> getHistoricoPrecos() {
        return Collections.unmodifiableList(historicoPrecos);
    }
}
