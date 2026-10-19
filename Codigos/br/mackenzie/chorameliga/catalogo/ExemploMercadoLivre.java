package br.mackenzie.chorameliga.catalogo;

import java.util.List;

// Execução manual para conferir a integração real com o Mercado Livre.
// Precisa das variáveis de ambiente ML_CLIENT_ID e ML_CLIENT_SECRET.
public class ExemploMercadoLivre {

    public static void main(String[] args) {
        ClienteHttp http = new ClienteHttpJava();
        MercadoLivreAuth auth = MercadoLivreAuth.doAmbiente(http);
        MercadoLivreFonte fonte = new MercadoLivreFonte(http, auth, "MLB-CELLPHONES");

        String busca = args.length > 0 ? String.join(" ", args) : "galaxy s23";
        System.out.println("Pesquisando: " + busca);

        List<ProdutoExterno> produtos = fonte.pesquisarProdutos(busca);
        for (ProdutoExterno p : produtos.stream().limit(2).toList()) {
            System.out.println(p.idExterno() + " | " + p.nome());

            List<OfertaExterna> ofertas = fonte.buscarOfertas(p.idExterno());
            if (ofertas.isEmpty()) {
                System.out.println("   (sem ofertas)");
            }
            for (OfertaExterna o : ofertas) {
                System.out.printf("   %s: R$ %.2f (frete R$ %.2f)%n", o.loja(), o.preco(), o.frete());
            }
        }
    }
}