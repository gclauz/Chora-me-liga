const formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

const formulario = document.getElementById("form-pesquisa");
const campoNome = document.getElementById("campo-nome");
const mensagem = document.getElementById("mensagem");
const telaResultados = document.getElementById("tela-resultados");
const listaResultados = document.getElementById("lista-resultados");
const telaDetalhes = document.getElementById("tela-detalhes");
const conteudoDetalhes = document.getElementById("conteudo-detalhes");
const botaoVoltar = document.getElementById("botao-voltar");

formulario.addEventListener("submit", (evento) => {
  evento.preventDefault();
  const nome = campoNome.value.trim();
  if (nome === "") {
    mostrarMensagem("Digite o nome de um produto.", true);
    return;
  }
  pesquisar(nome);
});

botaoVoltar.addEventListener("click", () => {
  telaDetalhes.hidden = true;
  telaResultados.hidden = false;
});

// ---------- chamadas à API ----------

async function chamarApi(url) {
  let resposta;
  try {
    resposta = await fetch(url);
  } catch (e) {
    throw new Error("Não foi possível falar com o servidor.");
  }
  if (!resposta.ok) {
    let texto = "Erro ao consultar o servidor.";
    try {
      const corpo = await resposta.json();
      if (corpo.erro) {
        texto = corpo.erro;
      }
    } catch (e) {
      // resposta sem JSON: mantém a mensagem padrão
    }
    throw new Error(texto);
  }
  return resposta.json();
}

async function pesquisar(nome) {
  telaDetalhes.hidden = true;
  telaResultados.hidden = false;
  limpar(listaResultados);
  mostrarMensagem("Pesquisando...");
  try {
    const produtos = await chamarApi("/api/produtos?nome=" + encodeURIComponent(nome));
    if (produtos.length === 0) {
      mostrarMensagem("Nenhum produto encontrado.");
      return;
    }
    mostrarMensagem(produtos.length + (produtos.length === 1 ? " produto encontrado" : " produtos encontrados"));
    produtos.forEach((produto) => listaResultados.appendChild(criarCard(produto)));
  } catch (e) {
    mostrarMensagem(e.message, true);
  }
}

async function abrirDetalhes(id) {
  telaResultados.hidden = true;
  telaDetalhes.hidden = false;
  limpar(conteudoDetalhes);
  mostrarMensagem("Carregando detalhes...");
  try {
    const detalhes = await chamarApi("/api/produtos/" + encodeURIComponent(id));
    mostrarMensagem("");
    mostrarDetalhes(detalhes);
  } catch (e) {
    mostrarMensagem(e.message, true);
  }
}

// ---------- montagem da tela ----------

function criarCard(produto) {
  const card = criar("button", "card");
  card.type = "button";
  card.appendChild(criar("span", "card-nome", produto.nome));
  card.appendChild(criar("span", "card-categoria", produto.categoria || ""));

  if (produto.quantidadeOfertas > 0 && produto.menorPreco != null) {
    card.appendChild(criar("span", "card-preco", "a partir de " + formatoMoeda.format(produto.menorPreco)));
    card.appendChild(criar("span", "card-ofertas",
        produto.quantidadeOfertas + (produto.quantidadeOfertas === 1 ? " oferta" : " ofertas")));
  } else {
    card.classList.add("sem-ofertas");
    card.appendChild(criar("span", "card-ofertas", "Sem ofertas no momento"));
  }

  card.addEventListener("click", () => abrirDetalhes(produto.id));
  return card;
}

function mostrarDetalhes(detalhes) {
  conteudoDetalhes.appendChild(criar("h2", "detalhe-nome", detalhes.nome));
  conteudoDetalhes.appendChild(criar("p", "detalhe-categoria", detalhes.categoria || ""));

  if (detalhes.ofertas.length === 0) {
    conteudoDetalhes.appendChild(criar("p", "mensagem", "Este produto não tem ofertas no momento."));
    return;
  }

  if (detalhes.menorPrecoAtual != null) {
    conteudoDetalhes.appendChild(
        criar("p", "destaque", "Menor preço encontrado: " + formatoMoeda.format(detalhes.menorPrecoAtual)));
  }

  const lista = criar("div", "ofertas");
  detalhes.ofertas.forEach((oferta, indice) => lista.appendChild(criarOferta(oferta, indice === 0)));
  conteudoDetalhes.appendChild(lista);
}

function criarOferta(oferta, melhor) {
  const linha = criar("div", "oferta");
  if (melhor) {
    linha.classList.add("melhor");
    linha.appendChild(criar("span", "selo", "Melhor preço"));
  }
  linha.appendChild(criar("span", "oferta-loja", oferta.loja));
  linha.appendChild(criar("span", "oferta-preco", formatoMoeda.format(oferta.preco)));
  linha.appendChild(criar("span", "oferta-frete",
      oferta.freteGratis ? "Frete grátis" : "Frete " + formatoMoeda.format(oferta.frete)));

  if (urlSegura(oferta.url)) {
    const link = criar("a", "botao", "Ir para a loja");
    link.href = oferta.url;
    link.target = "_blank";
    link.rel = "noopener noreferrer";
    linha.appendChild(link);
  }
  return linha;
}

// ---------- utilitários ----------

function criar(tag, classe, texto) {
  const elemento = document.createElement(tag);
  elemento.className = classe;
  if (texto !== undefined) {
    elemento.textContent = texto;
  }
  return elemento;
}

function limpar(elemento) {
  elemento.replaceChildren();
}

function mostrarMensagem(texto, erro = false) {
  mensagem.textContent = texto;
  mensagem.classList.toggle("erro", erro);
}

// só aceita links https, para um link malicioso não virar "javascript:..." na tela
function urlSegura(url) {
  return typeof url === "string" && url.startsWith("https://");
}