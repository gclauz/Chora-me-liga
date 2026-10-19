package br.mackenzie.chorameliga;

import br.mackenzie.chorameliga.catalogo.ClienteHttp;
import br.mackenzie.chorameliga.catalogo.ClienteHttpJava;
import br.mackenzie.chorameliga.catalogo.FonteCatalogo;
import br.mackenzie.chorameliga.catalogo.MercadoLivreAuth;
import br.mackenzie.chorameliga.catalogo.MercadoLivreFonte;
import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.OfertaDetalhe;
import br.mackenzie.chorameliga.model.Produto;
import br.mackenzie.chorameliga.repository.ProdutoRepository;
import br.mackenzie.chorameliga.service.ImportacaoCatalogoService;
import br.mackenzie.chorameliga.service.ProdutoService;

import java.util.List;

// Execução manual: UC01 (pesquisar) e UC03 (detalhes) com dados reais do Mercado Livre.
// Precisa das variáveis de ambiente ML_CLIENT_ID e ML_CLIENT_SECRET.
public class ExemploPesquisaCompleta {

    public static void main(String[] args) {
        ClienteHttp http = new ClienteHttpJava();
        MercadoLivreAuth auth = MercadoLivreAuth.doAmbiente(http);
        FonteCatalogo fonte = new MercadoLivreFonte(http, auth, "MLB-CELLPHONES");

        ProdutoRepository repositorio = new ProdutoRepository();
        ProdutoService service = new ProdutoService(repositorio,
                new ImportacaoCatalogoService(repositorio, fonte));

        String busca = args.length > 0 ? String.join(" ", args) : "galaxy s23";
        System.out.println("UC01 - Pesquisando: " + busca);
        List<Produto> produtos = service.pesquisarPorNome(busca);

        for (Produto p : produtos) {
            System.out.println("  [" + p.getId() + "] " + p.getNome()
                    + " (" + p.getQuantidadeOfertas() + " ofertas)");
        }
        if (produtos.isEmpty()) {
            System.out.println("  Nenhum produto encontrado.");
            return;
        }

        Produto escolhido = produtos.stream()
                .filter(p -> p.getQuantidadeOfertas() > 0)
                .findFirst()
                .orElse(produtos.get(0));

        DetalhesProduto detalhes = service.buscarDetalhes(escolhido.getId());
        System.out.println();
        System.out.println("UC03 - Detalhes: " + detalhes.nome());
        for (OfertaDetalhe o : detalhes.ofertas()) {
            System.out.printf("  %s: R$ %.2f + frete R$ %.2f = R$ %.2f%n",
                    o.loja(), o.preco(), o.frete(), o.precoTotal());
        }
        if (detalhes.menorPrecoAtual() != null) {
            System.out.printf("  Menor preço atual: R$ %.2f%n", detalhes.menorPrecoAtual());
        }
    }
}