package br.mackenzie.chorameliga.catalogo;

// Lançada quando a fonte externa não responde ou responde com erro.
public class FonteIndisponivelException extends RuntimeException {

    public FonteIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public FonteIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}