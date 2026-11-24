# Casos de teste: Chora-me-liga

Versão para leitura no GitHub. O documento completo, com o catálogo dos 155 testes, está em [`casos-de-teste.docx`](casos-de-teste.docx).

**Execução de 24/11/2026:** 155 testes automatizados, 155 passaram, 0 falharam (21 classes de teste em [`CodigosTest/`](../CodigosTest)).

Os testes rodam com `mvn clean test` e na esteira do GitHub Actions (`.github/workflows/ci.yml`) a cada push e pull request. Não dependem de internet nem de chaves: o Mercado Livre e o Gemini são simulados, e o histórico usa um banco H2 em memória.

## Casos de teste automatizados

### REQ-01 / UC01: Pesquisar produto pelo nome

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-01 | Pesquisa com resultado | GET /api/produtos?nome=galaxy s23 | Status 200 com 2 produtos; o primeiro traz o nome, 3 ofertas e menor preço R$ 3.499,00. | `ProdutoControllerTest.testPesquisarDevolveLista`<br>`ProdutoServicePesquisaTest.testResumosTrazemQuantidadeEMenorPreco` | Passou |
| CT-02 | Pesquisa sem resultado | GET /api/produtos?nome=geladeira | Status 200 com lista vazia (não é tratado como erro). | `ProdutoControllerTest.testPesquisarSemResultado`<br>`ImportacaoCatalogoServiceTest.testSemResultados` | Passou |
| CT-03 | Nome ausente ou em branco | GET /api/produtos sem o parâmetro nome; e com nome contendo só espaços. | Status 400 com a mensagem "Informe o nome do produto."; nada é importado. | `ProdutoControllerTest.testPesquisarSemParametro`<br>`ProdutoControllerTest.testPesquisarNomeEmBranco`<br>`ProdutoServicePesquisaTest.testNomeInvalido` | Passou |
| CT-04 | Busca tolerante a maiúsculas, acentos e ordem das palavras | Pesquisar em maiúsculas, sem acento e com as palavras em outra ordem. | O produto é encontrado nos três casos; uma palavra que não existe no nome faz a busca não encontrar. | `ProdutoRepositoryTest.testBuscaIgnoraMaiusculas`<br>`ProdutoRepositoryTest.testBuscaIgnoraAcentos`<br>`ProdutoRepositoryTest.testBuscaComVariasPalavras` | Passou |
| CT-05 | Pesquisa importa do Mercado Livre sem duplicar | Fazer a mesma pesquisa duas vezes. | Os produtos são salvos com as ofertas e podem ser abertos pelo id; a segunda pesquisa devolve os mesmos produtos, sem repetir. | `ProdutoServicePesquisaTest.testPesquisaImportaDaFonte`<br>`ProdutoServicePesquisaTest.testPesquisaRepetidaNaoDuplica`<br>`ImportacaoCatalogoServiceTest.testImportarDuasVezesNaoDuplica` | Passou |
| CT-06 | Mercado Livre fora do ar | Pesquisar um produto. | Status 200; o sistema devolve o que já tinha salvo (ou lista vazia), sem erro para o usuário. | `ProdutoControllerTest.testFonteForaDoArNaoQuebraAPesquisa`<br>`ProdutoServicePesquisaTest.testFonteForaDoArUsaOQueJaTem` | Passou |
| CT-07 | Pesquisas ao mesmo tempo | 40 pesquisas iguais em paralelo. | Todas devolvem os 2 produtos, sem erro e sem duplicar produtos. | `ProdutoServicePesquisaTest.testPesquisasSimultaneasNaoQuebram` | Passou |
| CT-08 | Produto sem ofertas também aparece | GET /api/produtos?nome=console | Os dois aparecem; o sem ofertas traz 0 ofertas e o link do catálogo do Mercado Livre. | `ProdutoControllerTest.testPesquisarIncluiProdutoSemOfertas`<br>`ImportacaoCatalogoServiceTest.testProdutosSemOfertasTambemSaoDevolvidos` | Passou |

### Integração com a API do Mercado Livre

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-09 | Token de acesso (OAuth) | Pedir o token, usá-lo de novo dentro da validade e depois de expirar. | Faz um único POST no endereço do token; reaproveita o token válido; pede outro 60 s antes de expirar. | `MercadoLivreAuthTest.testObtemToken`<br>`MercadoLivreAuthTest.testReaproveitaTokenValido`<br>`MercadoLivreAuthTest.testRenovaQuandoExpira` | Passou |
| CT-10 | Credenciais ausentes ou inválidas | Montar a autenticação. | Falha com mensagem clara, sem chamar a API. | `MercadoLivreAuthTest.testCredenciaisInvalidas`<br>`MercadoLivreAuthTest.testDoAmbienteSemVariaveis` | Passou |
| CT-11 | Seleção das ofertas | Buscar as ofertas do produto. | Fica uma oferta por vendedor (a mais barata) e só as 5 mais baratas, ordenadas por preço + frete. | `MercadoLivreFonteTest.testBuscarOfertas`<br>`MercadoLivreFonteTest.testLimitaAs5OfertasMaisBaratas` | Passou |
| CT-12 | Frete grátis e ofertas inválidas | Buscar as ofertas. | Frete grátis vale R$ 0; ofertas inválidas são descartadas. | `MercadoLivreFonteTest.testFreteGratisZeraCusto`<br>`MercadoLivreFonteTest.testIgnoraPrecoInvalido` | Passou |
| CT-13 | Respostas inválidas e erros da API | Pesquisar e buscar ofertas. | Resposta inválida ou erro vira "fonte indisponível"; 404 significa produto sem ofertas (lista vazia). | `MercadoLivreFonteTest.testRespostaInvalida`<br>`MercadoLivreFonteTest.testFonteForaDoAr`<br>`MercadoLivreFonteTest.testProdutoSemOfertasDevolveListaVazia`<br>`MercadoLivreFonteTest.testErroDeServidorContinuaSendoFalha` | Passou |

### REQ-03 / UC03: Ver detalhes e preços do produto

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-14 | Detalhes com ofertas ordenadas | GET /api/produtos/1 | Status 200 com as 3 ofertas, da mais barata (R$ 3.499,00) para a mais cara (R$ 4.499,00); menor preço atual R$ 3.499,00. | `ProdutoControllerTest.testDetalhes`<br>`ProdutoServiceTest.testOfertasOrdenadas` | Passou |
| CT-15 | Dados de cada oferta | Abrir os detalhes. | Cada oferta traz preço, preço total com frete, frete grátis, parcela em 10x e o link da loja. | `ProdutoServiceTest.testCamposDaOferta`<br>`MercadoLivreFonteTest.testLinkDaOferta`<br>`ImportacaoCatalogoServiceTest.testOfertaComLinkProprioUsaOLinkDela` | Passou |
| CT-16 | Produto inexistente ou id inválido | GET /api/produtos/999 e GET /api/produtos/abc | Status 404 com mensagem para o id que não existe; status 400 para o id que não é número. | `ProdutoControllerTest.testDetalhesDeIdInexistente`<br>`ProdutoControllerTest.testDetalhesComIdInvalido`<br>`ProdutoServiceTest.testIdInexistente` | Passou |
| CT-17 | Cálculo de preço total, parcela e oferta mais barata | Calcular preço total, parcelas e comparar ofertas. | Preço total = preço + frete; parcela = total / parcelas; a oferta mais barata considera o frete. | `OfertaTest.testCalcularPrecoTotal`<br>`OfertaTest.testCalcularParcela`<br>`OfertaTest.testIsMaisBarataQue`<br>`ProdutoTest.testGetOfertaMaisBarata` | Passou |
| CT-18 | Validação dos dados | Criar oferta com loja vazia, preço zero ou frete negativo; produto com nome vazio. | O sistema recusa os dados inválidos com erro. | `OfertaTest.testConstrutorComDadosInvalidos`<br>`ProdutoTest.testConstrutorComNomeInvalido` | Passou |

### REQ-02 / UC02: Reconhecer o produto por foto

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-19 | Foto reconhecida | POST /api/produtos/foto com uma imagem JPG. | Status 200 com o nome e a categoria reconhecidos e os produtos da pesquisa (só celulares, sem a capinha). | `PesquisaFotoControllerTest.testFotoReconhecida`<br>`PesquisaPorFotoServiceTest.testFotoReconhecidaPesquisaNaCategoria` | Passou |
| CT-20 | Categoria desconhecida ou errada | Enviar a foto. | A pesquisa é feita só pelo nome, sem erro para o usuário. | `PesquisaPorFotoServiceTest.testFotoSemCategoriaPesquisaSoPeloNome`<br>`PesquisaPorFotoServiceTest.testCategoriaErradaCaiParaPesquisaPeloNome`<br>`GeminiReconhecedorTest.testSemCategoriaConhecida` | Passou |
| CT-21 | Formato e tamanho da imagem | Enviar JPG, PNG, WEBP; e também texto, PDF, WAV, arquivo vazio, acima de 5 MB ou sem o campo foto. | JPG, PNG e WEBP são aceitos; os demais recebem status 400 ("Formato de imagem não aceito. Use JPG, PNG ou WEBP." ou "Envie uma foto do produto."), sem chamar a IA. | `FormatoImagemTest.testDetectaFormatosAceitos`<br>`FormatoImagemTest.testRecusaOutrosArquivos`<br>`PesquisaFotoControllerTest.testArquivoInvalido`<br>`PesquisaPorFotoServiceTest.testFotoInvalida`<br>`GeminiReconhecedorTest.testFotoInvalidaNemChamaAApi` | Passou |
| CT-22 | Pedido enviado ao Gemini | Reconhecer uma foto. | A chave vai no cabeçalho (nunca na URL); a foto vai em base64 com o tipo correto; a resposta é pedida em JSON; nome com até 100 caracteres. | `GeminiReconhecedorTest.testPedidoEnviado`<br>`GeminiReconhecedorTest.testReconheceProdutoComCategoria`<br>`GeminiReconhecedorTest.testNomeMuitoLongoECortado`<br>`GeminiReconhecedorTest.testModeloConfiguravel` | Passou |
| CT-23 | Gemini sobrecarregado: nova tentativa e modelo reserva | Reconhecer uma foto. | Tenta de novo após uma pausa (503); passa para o modelo reserva (503 repetido, 404 ou 429) e reconhece a foto. | `GeminiReconhecedorTest.testSobrecargaTemporariaTentaDeNovo`<br>`GeminiReconhecedorTest.testReservaQuandoPrincipalSobrecarregado`<br>`GeminiReconhecedorTest.testReservaQuandoPrincipalAposentadoOuSemCota` | Passou |
| CT-24 | Reconhecimento indisponível | POST /api/produtos/foto | Desiste sem esperas desnecessárias e a API responde 503 com mensagem. | `GeminiReconhecedorTest.testGeminiIndisponivel`<br>`GeminiReconhecedorTest.testChaveInvalidaNaoTentaReserva`<br>`GeminiReconhecedorTest.testReservaTambemSobrecarregada`<br>`PesquisaFotoControllerTest.testReconhecimentoIndisponivel`<br>`PesquisaPorFotoServiceTest.testReconhecimentoIndisponivel` | Passou |

### REQ-05 / UC05: Avisar quando a foto não for identificada

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-25 | Foto sem produto identificável | POST /api/produtos/foto | Status 422 com a mensagem de que o produto não foi identificado; a tela mostra o aviso. | `PesquisaFotoControllerTest.testFotoNaoReconhecida`<br>`GeminiReconhecedorTest.testNaoReconhecido`<br>`PesquisaPorFotoServiceTest.testFotoNaoReconhecida`<br>`ReconhecedorFotoFakeTest.testNaoReconhece` | Passou |

### REQ-06 / UC06: Atualizar preços

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-26 | Preço novo atualiza a oferta e guarda o antigo | Nova pesquisa em que a loja tem preço diferente. | A oferta recebe o preço novo, sem ser duplicada, e o preço antigo fica no histórico. | `ImportacaoCatalogoServiceTest.testAtualizaPrecoNaSegundaImportacao`<br>`ProdutoTest.testAplicarPrecoLojaExistente`<br>`OfertaTest.testAtualizarPreco` | Passou |
| CT-27 | Preço igual não altera nada | Nova pesquisa com os mesmos preços. | Nem a oferta nem o histórico mudam. | `HistoricoPrecoServiceTest.testPrecoIgualNaoRepete` | Passou |
| CT-28 | Loja nova e dado inválido | Aplicar os preços. | A loja nova ganha uma oferta; a oferta inválida é ignorada e as demais são aplicadas. | `ImportacaoCatalogoServiceTest.testOfertaInvalidaNaoInterrompe`<br>`ImportacaoCatalogoServiceTest.testFonteComErroNasOfertas` | Passou |

### REQ-07 / UC07: Filtros na busca (categoria)

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-29 | Filtrar pela categoria | GET /api/produtos?nome=notebook&dominio=MLB-NOTEBOOKS | Só os notebooks aparecem, inclusive os que já estavam salvos; sem o parâmetro, aparece tudo. | `ProdutoControllerDominioTest.testPesquisaComDominio`<br>`ProdutoControllerDominioTest.testPesquisaSemDominio`<br>`FiltroPorDominioTest.testImportacaoComDominio`<br>`FiltroPorDominioTest.testResumosFiltramTambemOQueJaEstavaSalvo`<br>`MercadoLivreFonteDominioTest.testDominioPedidoVaiNaPesquisa` | Passou |
| CT-30 | Categoria inválida | dominio="MLB NOTEBOOKS", valor longo demais ou tentativa de acrescentar outro parâmetro na URL. | Status 400 com mensagem; nada é importado. | `ProdutoControllerDominioTest.testDominioInvalidoDevolve400`<br>`FiltroPorDominioTest.testDominioInvalidoLancaErro` | Passou |

### REQ-09 / UC09: Consultar histórico de preços

| ID | Caso | Entrada / passos | Resultado esperado | Testes | Resultado |
|---|---|---|---|---|---|
| CT-31 | Primeira pesquisa grava todas as lojas | Pesquisar um produto e abrir os detalhes. | Um registro por loja é gravado e os detalhes mostram o histórico de cada loja. | `HistoricoPrecoServiceTest.testPrimeiraVezGravaTodasAsLojas`<br>`HistoricoNosDetalhesTest.testPesquisaGravaEDetalhesMostram` | Passou |
| CT-32 | Só grava quando o preço muda | Nova pesquisa em que só uma loja mudou; depois o preço volta ao antigo. | Só a loja que mudou é gravada; a volta ao preço antigo também é gravada. | `HistoricoPrecoServiceTest.testSoGravaALojaQueMudou`<br>`HistoricoPrecoServiceTest.testVoltarAoPrecoAntigoTambemGrava`<br>`HistoricoPrecoServiceTest.testProdutoSemIdExternoNaoGrava` | Passou |
| CT-33 | Histórico continua depois de reiniciar o servidor | Montar o sistema de novo (memória vazia) usando o mesmo banco e abrir os detalhes. | A Loja A mostra os dois preços, do mais antigo para o mais recente; a Loja B, que não mudou, mostra um. | `HistoricoNosDetalhesTest.testHistoricoSobreviveAoReinicio` | Passou |
| CT-34 | Menor preço já registrado | Abrir os detalhes. | O menor preço histórico considera os preços antigos gravados no banco. | `HistoricoNosDetalhesTest.testMenorPrecoHistoricoUsaOBanco`<br>`ProdutoServiceTest.testMenoresPrecos`<br>`OfertaTest.testMenorPrecoHistorico` | Passou |
| CT-35 | Banco fora do ar não atrapalha a pesquisa | Pesquisar e abrir os detalhes. | A pesquisa funciona; gravar não lança erro e o histórico aparece vazio. | `HistoricoPrecoServiceTest.testBancoForaDoArNaoAtrapalhaAPesquisa`<br>`HistoricoNosDetalhesTest.testSemHistoricoConfigurado` | Passou |
| CT-36 | Gravação no banco SQL | Gravar e ler registros; rodar o schema.sql de novo. | Os registros voltam em ordem de data, com preço e data iguais aos gravados; rodar o schema de novo não apaga dados. | `HistoricoPrecosJdbcTest.testGravaELeEmOrdemDeData`<br>`HistoricoPrecosJdbcTest.testProdutoSemHistorico`<br>`HistoricoPrecosJdbcTest.testSchemaPodeRodarDeNovo` | Passou |

## Roteiros de teste manual (tela)

| ID | Req. | Caso | Passos | Resultado esperado |
|---|---|---|---|---|
| CTM-01 | REQ-01 | Pesquisar pelo nome na tela | Na tela inicial, digitar "Galaxy S23" no campo de pesquisa e clicar em Pesquisar. | Aparece a lista de produtos com foto, menor preço e quantidade de ofertas. |
| CTM-02 | REQ-03 | Ver detalhes do produto | Clicar num produto da lista; trocar "Ordenar ofertas por" para menor frete e para loja; clicar numa oferta. | Mostra as ofertas de cada loja com preço, frete e parcela; a ordem muda conforme a opção; o link abre a página da loja. |
| CTM-03 | REQ-02 | Pesquisar por foto | Clicar em Pesquisar por foto, depois em Carregar imagem, e escolher a foto de um celular. | A tela mostra o nome reconhecido e os produtos encontrados. |
| CTM-04 | REQ-05 | Foto não identificada | Enviar a foto de uma paisagem (sem produto). | A tela mostra o aviso de que o produto não foi identificado. |
| CTM-05 | REQ-04 | Histórico de pesquisas | Fazer três pesquisas, voltar ao Início e recarregar a página; clicar numa pesquisa recente; remover uma; clicar em Limpar histórico. | As pesquisas aparecem em Pesquisas recentes (no máximo 10), continuam depois de recarregar, podem ser refeitas e removidas. |
| CTM-06 | REQ-07 | Filtros e ordenação | Clicar em Notebooks nas Categorias populares; ordenar por Menor preço, Maior preço e Mais ofertas; informar mín. e máx. de preço; clicar em Limpar filtros. | Só aparecem notebooks; a lista é reordenada e filtrada conforme cada opção; Limpar filtros volta à lista original. |
| CTM-07 | REQ-08 | Favoritos | Clicar na estrela (☆) de um produto e de uma oferta de uma loja; abrir Favoritos no menu; buscar pelo nome da loja; filtrar Só ofertas; recarregar a página; remover dos favoritos. | Os itens aparecem em Meus favoritos, a busca e os filtros funcionam, os favoritos continuam após recarregar e somem ao remover. |
| CTM-08 | REQ-09 | Histórico de preços na tela | Abrir os detalhes de um produto pesquisado em dias diferentes; passar o mouse no gráfico; abrir a tabela de registros. | Mostra o menor preço registrado, o gráfico com uma linha por loja, o preço de cada loja no ponto do mouse e a tabela com todos os registros. |
| CTM-09 | Extra | Pesquisa por voz | No Chrome ou Edge, clicar no microfone, permitir o acesso e falar "Galaxy S23". | O texto falado aparece no campo e a pesquisa é feita automaticamente. |
| CTM-10 | REQ-11 | Esteira de testes (CI) | Fazer push numa branch ou abrir um pull request; abrir a aba Actions do GitHub. | O workflow CI roda "mvn clean test" com Java 17 e termina verde, com 155 testes e 0 falhas. |
| CTM-11 | REQ-12 | Sistema na nuvem | Abrir https://chora-me-liga.onrender.com e fazer uma pesquisa. | O site abre em HTTPS (o primeiro acesso pode levar cerca de 1 minuto) e a pesquisa traz produtos do Mercado Livre. |

## Classes de teste

| Classe | Nível | Testes |
|---|---|---|
| [`FonteCatalogoFakeTest`](../CodigosTest/br/mackenzie/chorameliga/catalogo/FonteCatalogoFakeTest.java) | Integração com o Mercado Livre (HTTP simulado) | 4 |
| [`MercadoLivreAuthTest`](../CodigosTest/br/mackenzie/chorameliga/catalogo/MercadoLivreAuthTest.java) | Integração com o Mercado Livre (HTTP simulado) | 7 |
| [`MercadoLivreFonteDominioTest`](../CodigosTest/br/mackenzie/chorameliga/catalogo/MercadoLivreFonteDominioTest.java) | Integração com o Mercado Livre (HTTP simulado) | 4 |
| [`MercadoLivreFonteTest`](../CodigosTest/br/mackenzie/chorameliga/catalogo/MercadoLivreFonteTest.java) | Integração com o Mercado Livre (HTTP simulado) | 17 |
| [`PesquisaFotoControllerTest`](../CodigosTest/br/mackenzie/chorameliga/controller/PesquisaFotoControllerTest.java) | API REST (Spring MockMvc) | 4 |
| [`ProdutoControllerDominioTest`](../CodigosTest/br/mackenzie/chorameliga/controller/ProdutoControllerDominioTest.java) | API REST (Spring MockMvc) | 3 |
| [`ProdutoControllerTest`](../CodigosTest/br/mackenzie/chorameliga/controller/ProdutoControllerTest.java) | API REST (Spring MockMvc) | 9 |
| [`HistoricoPrecosJdbcTest`](../CodigosTest/br/mackenzie/chorameliga/historico/HistoricoPrecosJdbcTest.java) | Integração com banco de dados (H2 em memória) | 3 |
| [`OfertaTest`](../CodigosTest/br/mackenzie/chorameliga/model/OfertaTest.java) | Unidade (regras de negócio) | 7 |
| [`ProdutoTest`](../CodigosTest/br/mackenzie/chorameliga/model/ProdutoTest.java) | Unidade (regras de negócio) | 9 |
| [`FormatoImagemTest`](../CodigosTest/br/mackenzie/chorameliga/reconhecimento/FormatoImagemTest.java) | Integração com o Gemini (HTTP simulado) | 2 |
| [`GeminiReconhecedorTest`](../CodigosTest/br/mackenzie/chorameliga/reconhecimento/GeminiReconhecedorTest.java) | Integração com o Gemini (HTTP simulado) | 18 |
| [`ReconhecedorFotoFakeTest`](../CodigosTest/br/mackenzie/chorameliga/reconhecimento/ReconhecedorFotoFakeTest.java) | Integração com o Gemini (HTTP simulado) | 4 |
| [`ProdutoRepositoryTest`](../CodigosTest/br/mackenzie/chorameliga/repository/ProdutoRepositoryTest.java) | Unidade (armazenamento) | 10 |
| [`FiltroPorDominioTest`](../CodigosTest/br/mackenzie/chorameliga/service/FiltroPorDominioTest.java) | Serviço (casos de uso, com dublês) | 6 |
| [`HistoricoNosDetalhesTest`](../CodigosTest/br/mackenzie/chorameliga/service/HistoricoNosDetalhesTest.java) | Serviço (casos de uso, com dublês) | 4 |
| [`HistoricoPrecoServiceTest`](../CodigosTest/br/mackenzie/chorameliga/service/HistoricoPrecoServiceTest.java) | Serviço (casos de uso, com dublês) | 7 |
| [`ImportacaoCatalogoServiceTest`](../CodigosTest/br/mackenzie/chorameliga/service/ImportacaoCatalogoServiceTest.java) | Serviço (casos de uso, com dublês) | 13 |
| [`PesquisaPorFotoServiceTest`](../CodigosTest/br/mackenzie/chorameliga/service/PesquisaPorFotoServiceTest.java) | Serviço (casos de uso, com dublês) | 8 |
| [`ProdutoServicePesquisaTest`](../CodigosTest/br/mackenzie/chorameliga/service/ProdutoServicePesquisaTest.java) | Serviço (casos de uso, com dublês) | 10 |
| [`ProdutoServiceTest`](../CodigosTest/br/mackenzie/chorameliga/service/ProdutoServiceTest.java) | Serviço (casos de uso, com dublês) | 6 |
| **Total** | | **155** |
