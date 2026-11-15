package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.historico.HistoricoPrecos;
import br.mackenzie.chorameliga.historico.RegistroPreco;
import br.mackenzie.chorameliga.model.Oferta;
import br.mackenzie.chorameliga.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * UC06/UC09: grava os preços das ofertas com a data e devolve o histórico de um produto.
 * Só grava quando o preço de uma loja é diferente do último gravado, então pesquisar o mesmo
 * produto várias vezes (ou reiniciar o servidor) não repete pontos.
 * Se o banco falhar, a pesquisa continua funcionando: o erro só vai para o log.
 */
public class HistoricoPrecoService {

    private static final Logger log = LoggerFactory.getLogger(HistoricoPrecoService.class);

    private final HistoricoPrecos historico;
    private final Clock relogio;

    public HistoricoPrecoService(HistoricoPrecos historico) {
        this(historico, Clock.systemUTC());
    }

    public HistoricoPrecoService(HistoricoPrecos historico, Clock relogio) {
        if (historico == null || relogio == null) {
            throw new IllegalArgumentException("Histórico e relógio são obrigatórios.");
        }
        this.historico = historico;
        this.relogio = relogio;
    }

    public void registrar(Produto produto) {
        if (produto == null || produto.getIdExterno() == null || produto.getOfertas().isEmpty()) {
            return; // sem id externo não dá para achar o produto de novo depois
        }
        String id = produto.getIdExterno();
        try {
            Map<String, Double> ultimos = ultimoPrecoPorLoja(historico.listar(id));
            Instant agora = Instant.now(relogio);
            for (Oferta oferta : produto.getOfertas()) {
                Double ultimo = ultimos.get(chave(oferta.getLoja()));
                if (ultimo == null || mudou(ultimo, oferta.getPreco())) {
                    historico.registrar(id, new RegistroPreco(oferta.getLoja(), oferta.getPreco(), agora));
                }
            }
        } catch (RuntimeException e) {
            log.warn("Não foi possível gravar o histórico de preços de {}: {}", id, e.getMessage());
        }
    }

    public List<RegistroPreco> listar(Produto produto) {
        if (produto == null || produto.getIdExterno() == null) {
            return List.of();
        }
        try {
            return historico.listar(produto.getIdExterno());
        } catch (RuntimeException e) {
            log.warn("Não foi possível ler o histórico de preços de {}: {}", produto.getIdExterno(), e.getMessage());
            return List.of();
        }
    }

    private static Map<String, Double> ultimoPrecoPorLoja(List<RegistroPreco> registros) {
        Map<String, Double> ultimos = new HashMap<>();
        for (RegistroPreco r : registros) { // vêm do mais antigo para o mais recente
            ultimos.put(chave(r.loja()), r.preco());
        }
        return ultimos;
    }

    // mesma regra de Produto.aplicarPreco: o nome da loja não diferencia maiúsculas
    private static String chave(String loja) {
        return loja.toLowerCase(Locale.ROOT);
    }

    // o banco guarda centavos; diferenças menores que meio centavo não são mudança de preço
    private static boolean mudou(double antes, double agora) {
        return Math.abs(antes - agora) >= 0.005;
    }
}
