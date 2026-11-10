package br.mackenzie.chorameliga.catalogo;

// A API respondeu, mas com um status de erro (ex.: 404, 403, 500).
public class RespostaHttpException extends FonteIndisponivelException {

    private final int status;
    private final String corpo; // o que a API explicou sobre o erro (pode ser null)

    public RespostaHttpException(int status) {
        this(status, null);
    }

    public RespostaHttpException(int status, String corpo) {
        super("A API respondeu com status " + status);
        this.status = status;
        this.corpo = corpo;
    }

    public int getStatus() {
        return status;
    }

    public String getCorpo() {
        return corpo;
    }
}