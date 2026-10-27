package br.mackenzie.chorameliga.config;

import br.mackenzie.chorameliga.catalogo.ClienteHttp;
import br.mackenzie.chorameliga.catalogo.ClienteHttpJava;
import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.FonteCatalogoFake;
import br.mackenzie.chorameliga.catalogo.MercadoLivreAuth;
import br.mackenzie.chorameliga.catalogo.MercadoLivreFonte;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import br.mackenzie.chorameliga.service.ImportacaoCatalogoService;
import br.mackenzie.chorameliga.service.ProdutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    @Bean
    public ProdutoRepository produtoRepository() {
        return new ProdutoRepository();
    }

    // Com as credenciais no ambiente usa o Mercado Livre; sem elas, usa dados de exemplo.
    // Assim o projeto sobe (e os testes rodam) em qualquer máquina, sem segredo nenhum.
    @Bean
    public FonteCatalogo fonteCatalogo() {
        String id = System.getenv("ML_CLIENT_ID");
        String segredo = System.getenv("ML_CLIENT_SECRET");
        if (vazio(id) || vazio(segredo)) {
            log.warn("ML_CLIENT_ID e ML_CLIENT_SECRET não definidas: usando dados de exemplo.");
            return FonteCatalogoFake.comDadosDeExemplo();
        }
        String dominio = System.getenv("ML_DOMINIO"); // opcional, ex.: MLB-CELLPHONES
        ClienteHttp http = new ClienteHttpJava();
        log.info("Usando a API do Mercado Livre.");
        return new MercadoLivreFonte(http, new MercadoLivreAuth(http, id, segredo), dominio);
    }

    @Bean
    public ImportacaoCatalogoService importacaoCatalogoService(ProdutoRepository repositorio, FonteCatalogo fonte) {
        return new ImportacaoCatalogoService(repositorio, fonte);
    }

    @Bean
    public ProdutoService produtoService(ProdutoRepository repositorio, ImportacaoCatalogoService importacao) {
        return new ProdutoService(repositorio, importacao);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}