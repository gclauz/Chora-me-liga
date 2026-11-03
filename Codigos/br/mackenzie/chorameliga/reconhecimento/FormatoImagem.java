package br.mackenzie.chorameliga.reconhecimento;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

// Descobre o formato da foto pelos primeiros bytes do arquivo (não confia no nome nem no tipo informado).
public enum FormatoImagem {
    JPEG("image/jpeg"),
    PNG("image/png"),
    WEBP("image/webp");

    public static final int TAMANHO_MAXIMO = 5 * 1024 * 1024; // 5 MB

    private static final byte[] ASSINATURA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] ASSINATURA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private final String tipoMime;

    FormatoImagem(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public String tipoMime() {
        return tipoMime;
    }

    public static Optional<FormatoImagem> detectar(byte[] imagem) {
        if (imagem == null) {
            return Optional.empty();
        }
        if (comecaCom(imagem, ASSINATURA_JPEG)) {
            return Optional.of(JPEG);
        }
        if (comecaCom(imagem, ASSINATURA_PNG)) {
            return Optional.of(PNG);
        }
        // WEBP: "RIFF" + 4 bytes de tamanho + "WEBP"
        if (imagem.length >= 12
                && "RIFF".equals(new String(imagem, 0, 4, StandardCharsets.US_ASCII))
                && "WEBP".equals(new String(imagem, 8, 4, StandardCharsets.US_ASCII))) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean comecaCom(byte[] dados, byte[] prefixo) {
        return dados.length >= prefixo.length
                && Arrays.equals(Arrays.copyOf(dados, prefixo.length), prefixo);
    }
}
