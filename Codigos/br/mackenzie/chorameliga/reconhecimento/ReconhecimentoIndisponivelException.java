package br.mackenzie.chorameliga.reconhecimento;

// O serviço de reconhecimento (IA) está fora do ar ou respondeu algo inesperado.
// Diferente de "não reconheci a foto": aqui nem deu para tentar.
public class ReconhecimentoIndisponivelException extends RuntimeException {

    public ReconhecimentoIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
