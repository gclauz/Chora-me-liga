package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.reconhecimento.ReconhecimentoIndisponivelException;
import br.mackenzie.chorameliga.service.FotoNaoReconhecida;
import br.mackenzie.chorameliga.service.ProdutoNaoEncontrado;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.util.Map;

// Transforma os erros do sistema em respostas JSON com o status HTTP certo.
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> requisicaoInvalida(IllegalArgumentException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> faltaParametro(MissingServletRequestParameterException e) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Parâmetro obrigatório ausente: " + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> valorInvalido(MethodArgumentTypeMismatchException e) {
        return resposta(HttpStatus.BAD_REQUEST,
                "Valor inválido para o parâmetro '" + e.getName() + "'.");
    }

    @ExceptionHandler(ProdutoNaoEncontrado.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(ProdutoNaoEncontrado e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // REQ 5: a foto chegou, mas o produto não foi identificado
    @ExceptionHandler(FotoNaoReconhecida.class)
    public ResponseEntity<Map<String, String>> fotoNaoReconhecida(FotoNaoReconhecida e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(ReconhecimentoIndisponivelException.class)
    public ResponseEntity<Map<String, String>> reconhecimentoIndisponivel(ReconhecimentoIndisponivelException e) {
        return resposta(HttpStatus.SERVICE_UNAVAILABLE,
                "O reconhecimento de fotos está indisponível agora. Tente de novo mais tarde ou pesquise pelo nome.");
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, String>> faltaArquivo(MissingServletRequestPartException e) {
        return resposta(HttpStatus.BAD_REQUEST, "Envie uma foto do produto.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> arquivoGrande(MaxUploadSizeExceededException e) {
        return resposta(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter no máximo 5 MB.");
    }

    private ResponseEntity<Map<String, String>> resposta(HttpStatus status, String mensagem) {
        String texto = (mensagem == null || mensagem.isBlank()) ? "Requisição inválida." : mensagem;
        return ResponseEntity.status(status).body(Map.of("erro", texto));
    }
}