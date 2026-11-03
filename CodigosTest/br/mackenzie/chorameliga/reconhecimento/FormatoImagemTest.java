package br.mackenzie.chorameliga.reconhecimento;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FormatoImagemTest {

    static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};
    static final byte[] WEBP = "RIFF\0\0\0\0WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    @Test
    void testDetectaFormatosAceitos() {
        // Caso 1: JPEG
        assertEquals(Optional.of(FormatoImagem.JPEG), FormatoImagem.detectar(JPEG));

        // Caso 2: PNG
        assertEquals(Optional.of(FormatoImagem.PNG), FormatoImagem.detectar(PNG));

        // Caso 3: WEBP
        assertEquals(Optional.of(FormatoImagem.WEBP), FormatoImagem.detectar(WEBP));
    }

    @Test
    void testRecusaOutrosArquivos() {
        // Caso 1: texto qualquer
        assertTrue(FormatoImagem.detectar("ola mundo".getBytes(StandardCharsets.UTF_8)).isEmpty());

        // Caso 2: PDF
        assertTrue(FormatoImagem.detectar("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)).isEmpty());

        // Caso 3: arquivo curto demais, vazio ou nulo
        assertTrue(FormatoImagem.detectar(new byte[]{(byte) 0xFF}).isEmpty());
        assertTrue(FormatoImagem.detectar(new byte[0]).isEmpty());
        assertTrue(FormatoImagem.detectar(null).isEmpty());

        // Caso 4: RIFF que não é WEBP (ex.: áudio WAV)
        assertTrue(FormatoImagem.detectar("RIFF\0\0\0\0WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).isEmpty());
    }
}
