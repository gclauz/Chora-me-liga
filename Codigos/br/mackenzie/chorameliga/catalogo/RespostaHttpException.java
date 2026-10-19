package br.mackenzie.chorameliga.catalogo;

// A API respondeu, mas com um status de erro (ex.: 404, 403, 500).
public class RespostaHttpException extends FonteIndisponivelException {

    private final int status;

    public RespostaHttpException(int status) {
        super("A API respondeu com status " + status);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}