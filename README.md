# Chora-me-liga

Projeto da disciplina Laboratório de Engenharia de Software, Ciência da Computação, Universidade Presbiteriana Mackenzie. Alinhado ao ODS 12 (consumo e produção responsáveis).

Comparador de preços: o usuário pesquisa um produto pelo nome, por voz ou por foto e vê em quais lojas ele é mais barato, com histórico de preços.

**Acesse:** https://chora-me-liga.onrender.com

O sistema está publicado no plano gratuito do Render: se ninguém acessou nos últimos minutos, a primeira abertura pode levar cerca de 1 minuto.

## Equipe

- Caio Cesar Navarro Pugliese
- Guilherme Clauz Morlina da Silva
- Matheus Lemos Rosal
- Vinicius Barbosa Pereira Allegretti

## Status

Todos os requisitos planejados (REQ-01 a REQ-12) estão prontos.

### Sprint 1 (concluída)

| Item | Requisito | Status |
|---|---|---|
| UC01 Pesquisar produto pelo nome | REQ-01 | Pronto, com API e tela |
| UC03 Ver detalhes e preços do produto | REQ-03 | Pronto, com API e tela |
| Integração com a API do Mercado Livre | — | Pronta (produtos, ofertas e filtro por categoria) |
| Código organizado no GitHub | REQ-10 | Cada funcionalidade numa branch, com pull request para a `main` |
| Entrega automática (CI/CD) | REQ-11 | Testes rodam no GitHub Actions a cada push e pull request; o que entra na `main` é publicado na nuvem (Sprint 4) |

### Sprint 2 (concluída)

| Item | Requisito | Status |
|---|---|---|
| UC02 Reconhecer o produto por foto | REQ-02 | Pronto: a foto é analisada pelo Gemini (Google), que devolve o nome e a categoria do produto para a pesquisa; sem a chave configurada, usa o reconhecimento simulado |
| UC05 Avisar quando a foto não for identificada | REQ-05 | Pronto: a API responde 422 com a mensagem e a tela mostra o aviso |

### Sprint 3 (concluída)

| Item | Requisito | Status |
|---|---|---|
| UC04 Histórico de pesquisas | REQ-04 | Pronto no navegador: as 10 pesquisas mais recentes (digitadas, por categoria, por voz e por foto) ficam na tela inicial e podem ser refeitas ou removidas. Ficam salvas no próprio navegador, sem login |
| UC06 Atualizar preços | REQ-06 | Pronto: a cada pesquisa os preços vêm do Mercado Livre e, quando o preço de uma loja muda, o novo valor é gravado no banco com a data |
| UC07 Filtros na busca | REQ-07 | Pronto: ordenar os resultados por relevância, menor preço, maior preço ou mais ofertas, e filtrar por faixa de preço (além das categorias); nos detalhes, ordenar as ofertas do produto por preço, frete ou loja |

### Sprint 4 (concluída)

| Item | Requisito | Status |
|---|---|---|
| UC08 Favoritos | REQ-08 | Pronto no navegador: dá para favoritar produtos e ofertas específicas de uma loja, e ver tudo na tela "Favoritos" do menu, com busca por nome ou loja, filtro por tipo e categoria e ordenação. Ficam salvos no próprio navegador, sem login |
| UC09 Consultar histórico de preços | REQ-09 | Pronto: nos detalhes do produto, gráfico do preço de cada loja ao longo do tempo (passando o mouse mostra o preço de cada loja no dia), menor preço já registrado e a tabela com todos os registros. O histórico continua existindo quando o servidor reinicia |
| Publicação na nuvem | REQ-12 | Pronta: aplicação no Render (via Docker) e banco PostgreSQL no Neon, ambos no plano gratuito; cada merge na `main` é publicado automaticamente |

### Extra

- **Pesquisa por voz:** botão de microfone ao lado do campo de pesquisa. O nome falado vira texto e a pesquisa é feita automaticamente. Usa o reconhecimento de fala do navegador (Chrome e Edge); em navegadores sem suporte o botão não aparece.

## Arquitetura

O sistema adota uma arquitetura em camadas, em `Codigos/br/mackenzie/chorameliga/`:

| Pasta | Responsabilidade |
|---|---|
| `controller` | API REST (`/api/produtos`) e tratamento de erros |
| `service` | Casos de uso: pesquisa, detalhes, importação do catálogo e atualização de preços |
| `model` | Entidades e regras de negócio (`Produto`, `Oferta`) |
| `repository` | Armazenamento e busca de produtos |
| `historico` | Histórico de preços gravado no banco de dados (`HistoricoPrecos`) |
| `dto` | Dados prontos para a tela (`ProdutoResumo`, `DetalhesProduto`, `OfertaDetalhe`) |
| `catalogo` | Origem dos produtos e ofertas (`FonteCatalogo`): Mercado Livre ou dados simulados |
| `reconhecimento` | Reconhecimento de produto por foto (`ReconhecedorFoto`): com o Gemini (`GeminiReconhecedor`) ou simulado; e validação do formato da imagem |
| `config` | Configuração dos componentes do Spring |

A tela fica em `src/main/resources/static/` (HTML, CSS e JavaScript, sem framework).

Os testes ficam em `CodigosTest/`, espelhando a mesma estrutura.

## API

| Método | Caminho | Descrição |
|---|---|---|
| GET | `/api/produtos?nome=...&dominio=...` | Pesquisa produtos pelo nome; `dominio` (opcional) filtra pela categoria do Mercado Livre, ex.: `MLB-CELLPHONES` |
| GET | `/api/produtos/{id}` | Detalhes de um produto com as ofertas das lojas e o histórico de preços de cada loja (`historico`) |
| POST | `/api/produtos/foto` | Pesquisa por foto (multipart, campo `foto`; JPG, PNG ou WEBP até 5 MB). Responde o nome reconhecido e os produtos; 422 se a foto não for identificada, 503 se o reconhecimento estiver fora do ar |

## Tecnologias

- Java 17
- Spring Boot 3.5
- Maven
- Banco de dados: H2 no computador e nos testes, PostgreSQL na nuvem (Neon)
- Docker e Render, para publicar na nuvem
- JUnit 5
- HTML, CSS e JavaScript
- API do Mercado Livre
- API do Gemini (Google), para reconhecer produtos em fotos
- GitHub Actions

## Documentação

| Documento | Conteúdo |
|---|---|
| [`Documentacao/Projeto Parte1.pdf`](Documentacao/Projeto%20Parte1.pdf) | Documento do projeto: requisitos, casos de uso e diagramas |
| [`Documentacao/casos-de-teste.md`](Documentacao/casos-de-teste.md) | Casos de teste, para ler no GitHub |
| [`Documentacao/casos-de-teste.docx`](Documentacao/casos-de-teste.docx) | Casos de teste completos, com o catálogo de todos os testes automatizados |

## Manual de instalação

### Pré-requisitos

- [Git](https://git-scm.com)
- [Java 17](https://adoptium.net) (JDK)
- [Maven 3.9](https://maven.apache.org/download.cgi) ou mais novo

Para conferir se estão instalados: `git --version`, `java -version` e `mvn -version`.

### Passo a passo

1. Baixe o projeto:

   ```bash
   git clone https://github.com/gclauz/Chora-me-liga.git
   cd Chora-me-liga
   ```

2. Rode o sistema:

   ```bash
   mvn spring-boot:run
   ```

3. Abra http://localhost:8080 no navegador.

Para parar o sistema, aperte `Ctrl + C` no terminal.

O histórico de preços fica num banco H2 na pasta `dados/` (criada na primeira execução e fora do GitHub). Para começar o histórico do zero, apague essa pasta com o sistema parado.

Sem configuração, o sistema usa dados simulados (ex.: pesquise "Galaxy S23") e toda foto válida é reconhecida como "Galaxy S23". Para usar os serviços reais, defina as variáveis de ambiente antes de rodar:

| Variável | Obrigatória | Descrição |
|---|---|---|
| `ML_CLIENT_ID` | Para usar o Mercado Livre | ID do aplicativo no Mercado Livre |
| `ML_CLIENT_SECRET` | Para usar o Mercado Livre | Segredo do aplicativo no Mercado Livre |
| `ML_DOMINIO` | Não | Limita as pesquisas a uma categoria, ex.: `MLB-CELLPHONES` |
| `SPRING_DATASOURCE_URL` | Na nuvem | Endereço do PostgreSQL, ex.: `jdbc:postgresql://HOST/BANCO?sslmode=require`; sem ela usa o H2 local |
| `SPRING_DATASOURCE_USERNAME` | Na nuvem | Usuário do PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Na nuvem | Senha do PostgreSQL |
| `GEMINI_API_KEY` | Para usar o Gemini | Chave da API do Gemini, criada no Google AI Studio |
| `GEMINI_MODELO` | Não | Modelo do Gemini usado nas fotos (padrão: `gemini-3.5-flash-lite`) |
| `GEMINI_MODELO_RESERVA` | Não | Modelo usado quando o principal está sobrecarregado, sem cota ou indisponível (padrão: `gemini-3.8-flash`; `nenhum` desliga a reserva) |

No plano gratuito do Gemini (novembro de 2026), o `gemini-3.5-flash-lite` permite 500 pedidos por dia e o `gemini-3.8-flash`, 20. Cada foto enviada gasta pelo menos um pedido.

No PowerShell:

```powershell
$env:ML_CLIENT_ID = "seu_id"
$env:ML_CLIENT_SECRET = "seu_segredo"
$env:GEMINI_API_KEY = "sua_chave"
mvn spring-boot:run
```

As chaves nunca devem ir para o código nem para o GitHub. As fotos da pesquisa por foto são enviadas ao Google para análise; no plano gratuito do Gemini, o Google pode usar esses dados para melhorar os seus produtos.

A pesquisa por voz precisa de Chrome ou Edge, de acesso ao microfone e de internet, e só funciona em `localhost` ou em HTTPS.

## Manual de utilização

Abra https://chora-me-liga.onrender.com (ou http://localhost:8080, se estiver rodando no seu computador).

### Pesquisar um produto

- **Pelo nome:** digite o nome no campo de pesquisa (ex.: "Galaxy S23") e clique em **Pesquisar**.
- **Por categoria:** clique numa das **Categorias populares** da tela inicial (Smartphones, Notebooks, Smart TVs, Games & Consoles).
- **Por voz:** clique no microfone ao lado do campo, permita o acesso ao microfone e fale o nome do produto. A pesquisa é feita sozinha. Funciona no Chrome e no Edge.
- **Por foto:** clique em **Pesquisar por foto**, depois em **Carregar imagem**, e escolha uma foto do produto (JPG, PNG ou WEBP, até 5 MB). O sistema mostra o nome reconhecido e os produtos encontrados. Se não identificar o produto, avisa para tentar outra foto.

### Ver e filtrar os resultados

Cada produto mostra o menor preço encontrado e quantas ofertas tem.

- **Ordenar:** Relevância, Menor preço, Maior preço ou Mais ofertas.
- **Faixa de preço:** preencha os campos **mín.** e **máx.**
- **Limpar filtros:** volta à lista original.

Para ver só uma categoria, pesquise pelos botões das **Categorias populares** da tela inicial.

### Comparar os preços de um produto

Clique num produto para abrir os detalhes.

- **Ofertas:** a lista mostra cada loja com preço, frete e parcelamento. O campo **Ordenar ofertas por** organiza por preço, frete ou loja. Clique numa oferta para abrir a página da loja.
- **Histórico de preços:** mostra o menor preço já registrado e um gráfico com o preço de cada loja ao longo do tempo. Passe o mouse no gráfico para ver o preço de cada loja naquele dia. Abaixo do gráfico fica a tabela com todos os registros. O gráfico aparece depois que o produto é pesquisado em dias diferentes e algum preço muda.

### Pesquisas recentes

As 10 últimas pesquisas aparecem na tela inicial, em **Pesquisas recentes**.

- Clique numa pesquisa para refazê-la.
- Use o **×** para remover uma pesquisa, ou **Limpar histórico** para apagar todas.

Elas ficam salvas no navegador, sem precisar de login.

### Favoritos

- Clique na estrela de um produto (ou de uma oferta de uma loja) para favoritar.
- Abra **Favoritos** no menu para ver tudo o que foi salvo. Dá para buscar por produto ou loja, mostrar **Só produtos** ou **Só ofertas**, filtrar por categoria e ordenar.
- Clique de novo na estrela para remover.

Os favoritos ficam salvos no navegador, sem precisar de login.

## Publicar na nuvem

O sistema roda no [Render](https://render.com) e guarda o histórico de preços no [Neon](https://neon.tech), ambos gratuitos e sem cartão. A tabela do histórico é criada sozinha na primeira vez que o sistema sobe.

1. **Banco (Neon):** crie um projeto (região São Paulo, se houver) e copie a *connection string*, que vem assim: `postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require`. Separe em três valores:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://HOST/BANCO?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `USUARIO`
   - `SPRING_DATASOURCE_PASSWORD` = `SENHA`
2. **Aplicação (Render):** entre com a conta do GitHub, clique em *New* > *Blueprint* e escolha este repositório. O Render lê o `render.yaml`, pede os valores das variáveis (os três do banco e as chaves do Mercado Livre e do Gemini) e monta a imagem com o `Dockerfile`.
3. A cada merge na `main` o Render publica a nova versão sozinho.

No plano gratuito, o Render desliga o sistema depois de 15 minutos sem acesso; o primeiro acesso depois disso demora cerca de um minuto. O histórico não se perde, porque fica no Neon.

## Testes

Os testes automatizados ficam em [`CodigosTest/`](CodigosTest): são 155 testes (JUnit 5) em 21 classes. Para rodar:

```bash
mvn clean test
```

Os testes não precisam de internet nem de chaves:

- o Mercado Livre e o Gemini são simulados;
- a API é testada com o Spring MockMvc;
- o histórico de preços usa um banco H2 em memória.

**Esteira (CI/CD):** a cada push e pull request, o GitHub Actions ([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda `mvn clean test` com Java 17. O resultado aparece na aba **Actions** do repositório. O que entra na `main` é publicado automaticamente no Render.

Os casos de teste estão em [`Documentacao/casos-de-teste.md`](Documentacao/casos-de-teste.md) e, completos, em [`Documentacao/casos-de-teste.docx`](Documentacao/casos-de-teste.docx).

## Uso de inteligência artificial

Este projeto foi desenvolvido com o auxílio de ferramentas de inteligência artificial, usadas para apoiar a escrita de código, de testes automatizados e desta documentação.

Todo o conteúdo gerado com ajuda da IA foi revisado, testado e aprovado pela equipe, que é responsável pelas decisões do projeto e pelo resultado final.

## Fluxo de trabalho

- Cada funcionalidade é feita numa branch `feature/...` e entra na `main` por pull request.
- O que entra na `main` é publicado automaticamente no Render.
