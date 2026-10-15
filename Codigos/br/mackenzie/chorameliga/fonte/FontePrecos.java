package br.mackenzie.chorameliga.fonte;

import java.util.List;

/** De onde vêm os preços. Hoje é simulada; depois pode ser o Mercado Livre. */
public interface FontePrecos {
    List<PrecoExterno> buscarPorNome(String nome);
}