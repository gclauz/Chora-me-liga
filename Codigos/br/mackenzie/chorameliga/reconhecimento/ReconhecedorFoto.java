package br.mackenzie.chorameliga.reconhecimento;

import java.util.Optional;

// Quem olha a foto e diz qual produto é. Hoje é simulado; depois, um serviço de IA.
public interface ReconhecedorFoto {

    // Devolve o produto identificado, ou vazio se não conseguir reconhecer (REQ 5).
    Optional<ProdutoReconhecido> reconhecer(byte[] imagem);
}
