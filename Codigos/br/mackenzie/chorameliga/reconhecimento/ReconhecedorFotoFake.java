package br.mackenzie.chorameliga.reconhecimento;

import java.util.Optional;

// Reconhecedor simulado, com resposta fixa. Serve para desenvolver e testar sem depender de IA.
public class ReconhecedorFotoFake implements ReconhecedorFoto {

    private final ProdutoReconhecido resposta;

    private ReconhecedorFotoFake(ProdutoReconhecido resposta) {
        this.resposta = resposta;
    }

    // Qualquer foto é reconhecida como o produto informado
    public static ReconhecedorFotoFake sempreReconhece(ProdutoReconhecido produto) {
        if (produto == null) {
            throw new IllegalArgumentException("Informe o produto que o fake deve devolver.");
        }
        return new ReconhecedorFotoFake(produto);
    }

    // Nenhuma foto é reconhecida (simula o caso do REQ 5)
    public static ReconhecedorFotoFake nuncaReconhece() {
        return new ReconhecedorFotoFake(null);
    }

    @Override
    public Optional<ProdutoReconhecido> reconhecer(byte[] imagem) {
        if (imagem == null || imagem.length == 0) {
            throw new IllegalArgumentException("A foto enviada está vazia.");
        }
        return Optional.ofNullable(resposta);
    }
}
