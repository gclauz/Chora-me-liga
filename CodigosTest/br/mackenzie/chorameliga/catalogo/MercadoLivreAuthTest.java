package br.mackenzie.chorameliga.catalogo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MercadoLivreAuthTest {

    private static final String RESPOSTA_TOKEN =
            "{\"access_token\":\"TOKEN-1\",\"token_type\":\"Bearer\",\"expires_in\":21600}";

    private ClienteHttpFake http;
    private Instant[] agora;
    private MercadoLivreAuth auth;

    @BeforeEach
    void setUp() {
        http = new ClienteHttpFake();
        agora = new Instant[] { Instant.parse("2026-10-19T12:00:00Z") };
        auth = new MercadoLivreAuth(http, "id-teste", "segredo-teste", () -> agora[0]);
    }

    @Test
    void testObtemToken() {
        http.respostas.add(RESPOSTA_TOKEN);

        // Caso 1: devolve o token da resposta
        assertEquals("TOKEN-1", auth.getToken());

        // Caso 2: fez uma única chamada, um POST no endereço do token
        assertEquals(1, http.chamadas.size());
        assertEquals("POST https://api.mercadolibre.com/oauth/token", http.chamadas.get(0));

        // Caso 3: pediu o token da aplicação, com o client_id
        assertTrue(http.ultimoCorpo.contains("grant_type=client_credentials"));
        assertTrue(http.ultimoCorpo.contains("client_id=id-teste"));
    }

    @Test
    void testReaproveitaTokenValido() {
        http.respostas.add(RESPOSTA_TOKEN);
        auth.getToken();

        // Caso 1: uma hora depois o token ainda vale, então não chama a API de novo
        agora[0] = agora[0].plusSeconds(3600);
        assertEquals("TOKEN-1", auth.getToken());
        assertEquals(1, http.chamadas.size());
    }

    @Test
    void testRenovaQuandoExpira() {
        http.respostas.add(RESPOSTA_TOKEN);
        http.respostas.add("{\"access_token\":\"TOKEN-2\",\"expires_in\":21600}");
        auth.getToken();

        // Caso 1: em 21540 s (21600 menos a margem de 60 s) pede outro token
        agora[0] = agora[0].plusSeconds(21540);
        assertEquals("TOKEN-2", auth.getToken());
        assertEquals(2, http.chamadas.size());
    }

    @Test
    void testRespostaInvalida() {
        // Caso 1: JSON sem access_token
        http.respostas.add("{\"expires_in\":21600}");
        assertThrows(IllegalStateException.class, () -> auth.getToken());

        // Caso 2: texto que não é JSON
        http.respostas.add("isto não é json");
        assertThrows(IllegalStateException.class, () -> auth.getToken());
    }

    @Test
    void testCredenciaisInvalidas() {
        // Caso 1: client_id vazio
        assertThrows(IllegalArgumentException.class, () -> new MercadoLivreAuth(http, "", "segredo"));

        // Caso 2: client_secret nulo
        assertThrows(IllegalArgumentException.class, () -> new MercadoLivreAuth(http, "id", null));

        // Caso 3: cliente HTTP nulo
        assertThrows(IllegalArgumentException.class, () -> new MercadoLivreAuth(null, "id", "segredo"));
    }

    @Test
    void testDoAmbiente() {
        Map<String, String> ambiente = Map.of("ML_CLIENT_ID", "id-env", "ML_CLIENT_SECRET", "segredo-env");
        http.respostas.add(RESPOSTA_TOKEN);

        // Caso 1: lê as credenciais das variáveis de ambiente
        MercadoLivreAuth doEnv = MercadoLivreAuth.doAmbiente(http, ambiente::get);
        assertEquals("TOKEN-1", doEnv.getToken());
        assertTrue(http.ultimoCorpo.contains("client_id=id-env"));
    }

    @Test
    void testDoAmbienteSemVariaveis() {
        // Caso 1: sem as variáveis definidas, falha com mensagem clara
        assertThrows(IllegalStateException.class,
                () -> MercadoLivreAuth.doAmbiente(http, nome -> null));
    }
}