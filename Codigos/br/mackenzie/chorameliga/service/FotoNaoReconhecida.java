package br.mackenzie.chorameliga.service;

// REQ 5: a foto foi analisada, mas não deu para identificar o produto.
public class FotoNaoReconhecida extends RuntimeException {

    public FotoNaoReconhecida() {
        super("Não conseguimos identificar o produto nessa foto. "
                + "Tente outra foto, com o produto bem visível, ou pesquise pelo nome.");
    }
}
