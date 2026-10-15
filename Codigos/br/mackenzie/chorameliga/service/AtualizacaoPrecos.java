package br.mackenzie.chorameliga.service;

import br.mackenzie.chorameliga.fonte.FontePrecos;
import br.mackenzie.chorameliga.fonte.PrecoExterno;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;

import java.util.List;

/**
 * UC06 - Atualizar preços.
 * Para cada produto cadastrado, pede os preços à fonte e aplica nas ofertas
 * (o histórico de preços é guardado pela própria Oferta).
 */
public class AtualizacaoPrecos {

    private final ProdutoRepository repositorio;
    private final FontePrecos fonte;

    public AtualizacaoPrecos(ProdutoRepository repositorio, FontePrecos fonte) {
        if (repositorio == null || fonte == null) {
            throw new IllegalArgumentException("Repositório e fonte de preços são obrigatórios.");
        }
        this.repositorio = repositorio;
        this.fonte = fonte;
    }

    public ResultadoAtualizacao atualizarPrecos() {
        int alteradas = 0;
        int falhas = 0;

        for (Produto produto : repositorio.listarTodos()) {
            List<PrecoExterno> precos;
            try {
                precos = fonte.buscarPorNome(produto.getNome());
            } catch (RuntimeException e) {
                falhas++;      // fonte fora do ar: segue para o próximo produto
                continue;
            }
            if (precos == null) {
                continue;
            }

            for (PrecoExterno p : precos) {
                // a busca pode trazer produtos parecidos; só aplica o que é exatamente este produto
                if (!p.nomeProduto().equalsIgnoreCase(produto.getNome())) {
                    continue;
                }
                try {
                    if (produto.aplicarPreco(p.loja(), p.preco(), p.frete(), p.url())) {
                        alteradas++;
                    }
                } catch (IllegalArgumentException e) {
                    falhas++;  // dado inválido vindo de fora (ex.: preço zero)
                }
            }
        }
        return new ResultadoAtualizacao(alteradas, falhas);
    }
}