package br.mackenzie.chorameliga.catalogo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Obtém e renova o token de acesso da aplicação no Mercado Livre.
 * As credenciais NUNCA ficam no código: vêm de variáveis de ambiente.
 */
public class MercadoLivreAuth {

    private static final String URL_TOKEN = "https://api.mercadolibre.com/oauth/token";
    private static final long MARGEM_SEGUNDOS = 60; // renova um pouco antes de vencer

    private final ClienteHttp http;
    private final String clientId;
    private final String clientSecret;
    private final Supplier<Instant> relogio; // existe para os testes controlarem o tempo
    private final ObjectMapper mapper = new ObjectMapper();

    private String token;
    private Instant expiraEm;

    public MercadoLivreAuth(ClienteHttp http, String clientId, String clientSecret) {
        this(http, clientId, clientSecret, Instant::now);
    }

    MercadoLivreAuth(ClienteHttp http, String clientId, String clientSecret, Supplier<Instant> relogio) {
        if (http == null || clientId == null || clientId.isBlank()
                || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Cliente HTTP, client_id e client_secret são obrigatórios.");
        }
        this.http = http;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.relogio = relogio;
    }

    // Lê ML_CLIENT_ID e ML_CLIENT_SECRET do ambiente
    public static MercadoLivreAuth doAmbiente(ClienteHttp http) {
        return doAmbiente(http, System::getenv);
    }

    static MercadoLivreAuth doAmbiente(ClienteHttp http, Function<String, String> ambiente) {
        String id = ambiente.apply("ML_CLIENT_ID");
        String segredo = ambiente.apply("ML_CLIENT_SECRET");
        if (id == null || id.isBlank() || segredo == null || segredo.isBlank()) {
            throw new IllegalStateException("Defina as variáveis de ambiente ML_CLIENT_ID e ML_CLIENT_SECRET.");
        }
        return new MercadoLivreAuth(http, id, segredo);
    }

    // Devolve um token válido, pedindo outro se ainda não existe ou está perto de vencer
    public synchronized String getToken() {
        if (token == null || !relogio.get().isBefore(expiraEm)) {
            renovar();
        }
        return token;
    }

    private void renovar() {
        String corpo = "grant_type=client_credentials"
                + "&client_id=" + codificar(clientId)
                + "&client_secret=" + codificar(clientSecret);
        String resposta = http.postForm(URL_TOKEN, corpo);

        JsonNode json;
        try {
            json = mapper.readTree(resposta);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Resposta de token inválida.");
        }

        String novoToken = json.path("access_token").asText("");
        if (novoToken.isBlank()) {
            throw new IllegalStateException("A resposta não trouxe o access_token.");
        }
        long validadeSegundos = json.path("expires_in").asLong(3600);

        token = novoToken;
        expiraEm = relogio.get().plusSeconds(Math.max(validadeSegundos - MARGEM_SEGUNDOS, 0));
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}