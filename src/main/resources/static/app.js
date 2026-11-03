const formatoMoeda = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });

// nome em português de cada domínio do Mercado Livre; os que não estão aqui não mostram categoria
const NOMES_CATEGORIAS = {
  "MLB-CELLPHONES": "Smartphones",
  "MLB-TELEVISIONS": "Smart TVs",
  "MLB-NOTEBOOKS": "Notebooks",
  "MLB-REFRIGERATORS": "Geladeiras",
  "MLB-GAME_CONSOLES": "Games & Consoles"
};

const telaBusca = document.getElementById("tela-busca");
const telaCategorias = document.getElementById("tela-categorias");
const telaResultados = document.getElementById("tela-resultados");
const telaDetalhes = document.getElementById("tela-detalhes");
const listaResultados = document.getElementById("lista-resultados");
const conteudoDetalhes = document.getElementById("conteudo-detalhes");
const formulario = document.getElementById("form-pesquisa");
const campoNome = document.getElementById("campo-nome");
const mensagem = document.getElementById("mensagem");
const botaoVoltar = document.getElementById("botao-voltar");

// área da seção "sem ofertas", criada aqui para não depender do HTML
const areaSemOfertas = document.createElement("div");
listaResultados.insertAdjacentElement("afterend", areaSemOfertas);

let numeroPesquisa = 0; // identifica a pesquisa mais recente
let dominioAtual = null; // categoria escolhida pelo botão; null = pesquisa livre

formulario.addEventListener("submit", (evento) => {
  evento.preventDefault();
  const nome = campoNome.value.trim();
  if (nome === "") {
    mostrarMensagem("Digite o nome de um produto.", true);
    return;
  }
  pesquisar(nome, dominioAtual);
});

// qualquer alteração digitada à mão vira pesquisa livre; se o campo ficar vazio, volta ao início
campoNome.addEventListener("input", () => {
  dominioAtual = null;
  if (campoNome.value.trim() === "") {
    voltarAoInicio();
  }
});

// cada categoria é um atalho: preenche o campo e pesquisa com o termo e o domínio do botão
document.querySelectorAll(".categoria").forEach((botao) => {
  botao.addEventListener("click", () => {
    campoNome.value = botao.dataset.termo;
    dominioAtual = botao.dataset.dominio || null;
    pesquisar(botao.dataset.termo, dominioAtual);
  });
});

botaoVoltar.addEventListener("click", () => mostrarTela("resultados"));

// ---------- pesquisa por foto ----------

const botaoFoto = document.getElementById("botao-foto");
const campoFoto = document.getElementById("campo-foto");
const previaFoto = document.getElementById("previa-foto");
const TIPOS_FOTO = ["image/jpeg", "image/png", "image/webp"];
const TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024; // mesmo limite do servidor
let urlPrevia = null;

botaoFoto.addEventListener("click", () => campoFoto.click());

document.getElementById("menu-foto").addEventListener("click", (evento) => {
  evento.preventDefault();
  campoFoto.click();
});

campoFoto.addEventListener("change", () => {
  const arquivo = campoFoto.files[0];
  campoFoto.value = ""; // permite escolher a mesma foto de novo
  if (!arquivo) {
    return;
  }
  // confere antes de enviar para dar a resposta na hora; o servidor confere de novo
  if (!TIPOS_FOTO.includes(arquivo.type)) {
    mostrarMensagem("Formato de imagem não aceito. Use JPG, PNG ou WEBP.", true);
    return;
  }
  if (arquivo.size > TAMANHO_MAXIMO_FOTO) {
    mostrarMensagem("A foto deve ter no máximo 5 MB.", true);
    return;
  }

  if (urlPrevia) {
    URL.revokeObjectURL(urlPrevia);
  }
  urlPrevia = URL.createObjectURL(arquivo);
  previaFoto.src = urlPrevia;
  previaFoto.hidden = false;

  pesquisarPorFoto(arquivo);
});

// ---------- pesquisa por voz ----------

// usa o reconhecimento de fala do próprio navegador (Chrome/Edge); sem suporte, o botão fica escondido
const botaoVoz = document.getElementById("botao-voz");
const Reconhecimento = window.SpeechRecognition || window.webkitSpeechRecognition;

if (Reconhecimento) {
  const reconhecimento = new Reconhecimento();
  reconhecimento.lang = "pt-BR";
  reconhecimento.interimResults = false;
  reconhecimento.maxAlternatives = 1;
  let ouvindo = false;

  botaoVoz.hidden = false;

  botaoVoz.addEventListener("click", () => {
    if (ouvindo) {
      reconhecimento.stop();
      return;
    }
    try {
      reconhecimento.start();
    } catch (e) {
      // start() lança erro se já estiver ouvindo; nesse caso não há o que fazer
    }
  });

  reconhecimento.addEventListener("start", () => {
    ouvindo = true;
    botaoVoz.classList.add("ouvindo");
    mostrarMensagem("Ouvindo... fale o nome do produto.");
  });

  reconhecimento.addEventListener("end", () => {
    ouvindo = false;
    botaoVoz.classList.remove("ouvindo");
  });

  // o que foi falado vai para o campo e vira uma pesquisa livre, como se tivesse sido digitado
  reconhecimento.addEventListener("result", (evento) => {
    const falado = evento.results[0][0].transcript.trim().replace(/[.!?]+$/, "");
    if (falado === "") {
      mostrarMensagem("Não entendi o que foi falado. Tente de novo.", true);
      return;
    }
    campoNome.value = falado;
    dominioAtual = null;
    pesquisar(falado, null);
  });

  reconhecimento.addEventListener("error", (evento) => {
    const mensagens = {
      "no-speech": "Não ouvi nada. Clique no microfone e fale o nome do produto.",
      "not-allowed": "O navegador bloqueou o microfone. Libere o acesso para pesquisar por voz.",
      "service-not-allowed": "O navegador bloqueou o microfone. Libere o acesso para pesquisar por voz.",
      "audio-capture": "Nenhum microfone foi encontrado.",
      "network": "Sem conexão com o serviço de voz do navegador. Tente digitar o nome."
    };
    if (evento.error !== "aborted") {
      mostrarMensagem(mensagens[evento.error] || "Não foi possível usar a pesquisa por voz.", true);
    }
  });
}

// ---------- telas ----------

// "inicio" = busca + categorias; "resultados" = busca + lista; "detalhes" = só o produto
function mostrarTela(tela) {
  telaBusca.hidden = tela === "detalhes";
  telaCategorias.hidden = tela !== "inicio";
  telaResultados.hidden = tela !== "resultados";
  telaDetalhes.hidden = tela !== "detalhes";
  window.scrollTo(0, 0);
}

function voltarAoInicio() {
  numeroPesquisa++; // invalida qualquer pesquisa que ainda esteja esperando resposta
  dominioAtual = null;
  limpar(listaResultados);
  limpar(areaSemOfertas);
  mostrarMensagem("");
  mostrarTela("inicio");
}

// ---------- chamadas à API ----------

async function chamarApi(url, opcoes) {
  let resposta;
  try {
    resposta = await fetch(url, opcoes);
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

async function pesquisar(nome, dominio) {
  const minhaPesquisa = ++numeroPesquisa;
  mostrarTela("resultados");
  limpar(listaResultados);
  limpar(areaSemOfertas);
  mostrarMensagem("Pesquisando...");

  let url = "/api/produtos?nome=" + encodeURIComponent(nome);
  if (dominio) {
    url += "&dominio=" + encodeURIComponent(dominio);
  }

  try {
    const produtos = await chamarApi(url);
    if (minhaPesquisa !== numeroPesquisa) {
      return; // surgiu outra pesquisa (ou o campo foi apagado) enquanto esperava
    }
    mostrarResultados(produtos, "");
  } catch (e) {
    if (minhaPesquisa !== numeroPesquisa) {
      return; // o erro é de uma pesquisa que já não vale
    }
    mostrarMensagem(e.message, true);
  }
}

// UC02: envia a foto, mostra o que foi reconhecido e os produtos encontrados com esse nome
async function pesquisarPorFoto(arquivo) {
  const minhaPesquisa = ++numeroPesquisa;
  mostrarTela("resultados");
  limpar(listaResultados);
  limpar(areaSemOfertas);
  mostrarMensagem("Reconhecendo o produto na foto...");
  botaoFoto.disabled = true;

  const dados = new FormData();
  dados.append("foto", arquivo);

  try {
    const resultado = await chamarApi("/api/produtos/foto", { method: "POST", body: dados });
    if (minhaPesquisa !== numeroPesquisa) {
      return;
    }
    // o nome reconhecido vai para o campo, para o usuário ajustar e pesquisar de novo se quiser
    campoNome.value = resultado.nomeReconhecido;
    dominioAtual = null;
    mostrarResultados(resultado.produtos, "Produto reconhecido: " + resultado.nomeReconhecido + ". ");
  } catch (e) {
    if (minhaPesquisa !== numeroPesquisa) {
      return;
    }
    mostrarMensagem(e.message, true); // inclui o aviso do REQ 5 quando a foto não é identificada
  } finally {
    botaoFoto.disabled = false;
  }
}

// mostra a lista; "prefixo" vai antes da contagem (ex.: o nome reconhecido na foto)
function mostrarResultados(produtos, prefixo) {
  // separa quem tem oferta de quem não tem; a ordem que veio da API é mantida
  const comOfertas = produtos.filter((p) => p.quantidadeOfertas > 0 && p.menorPreco != null);
  const semOfertas = produtos.filter((p) => !(p.quantidadeOfertas > 0 && p.menorPreco != null));

  if (comOfertas.length === 0) {
    mostrarMensagem(prefixo + (semOfertas.length === 0
        ? "Não encontramos produtos para essa pesquisa. Tente outro termo ou outra categoria."
        : "Não encontramos ofertas para essa pesquisa no momento."));
  } else {
    mostrarMensagem(prefixo + comOfertas.length + (comOfertas.length === 1 ? " produto encontrado" : " produtos encontrados"));
    comOfertas.forEach((produto) => listaResultados.appendChild(criarCard(produto)));
  }

  if (semOfertas.length > 0) {
    areaSemOfertas.appendChild(criarSecaoSemOfertas(semOfertas));
  }
}

async function abrirDetalhes(id) {
  mostrarTela("detalhes");
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

function nomeDaCategoria(codigo) {
  return NOMES_CATEGORIAS[codigo] || "";
}

function criarCard(produto) {
  const card = criar("button", "card");
  card.type = "button";
  card.appendChild(criar("span", "card-nome", produto.nome));
  const categoria = nomeDaCategoria(produto.categoria);
  if (categoria !== "") {
    card.appendChild(criar("span", "card-categoria", categoria));
  }
  card.appendChild(criar("span", "card-preco", "a partir de " + formatoMoeda.format(produto.menorPreco)));
  card.appendChild(criar("span", "card-ofertas",
      produto.quantidadeOfertas + (produto.quantidadeOfertas === 1 ? " oferta" : " ofertas")));

  card.addEventListener("click", () => abrirDetalhes(produto.id));
  return card;
}

// seção recolhida: só abre se o usuário quiser ver os produtos sem oferta
function criarSecaoSemOfertas(produtos) {
  const secao = criar("details", "sem-ofertas-secao");
  const quantidade = produtos.length;
  secao.appendChild(criar("summary", "sem-ofertas-titulo",
      "Ver " + quantidade + (quantidade === 1 ? " produto" : " produtos") + " sem ofertas no momento"));
  secao.appendChild(criar("p", "sem-ofertas-aviso",
      "Estes produtos existem no catálogo do Mercado Livre, mas não encontramos ofertas para eles agora."));

  const lista = criar("ul", "sem-ofertas-lista");
  produtos.forEach((produto) => {
    const item = criar("li", "sem-ofertas-item");
    item.appendChild(criar("span", "sem-ofertas-nome", produto.nome));
    if (urlSegura(produto.urlCatalogo)) {
      const link = criar("a", "sem-ofertas-link", "Ver no Mercado Livre");
      link.href = produto.urlCatalogo;
      link.target = "_blank";
      link.rel = "noopener noreferrer";
      item.appendChild(link);
    }
    lista.appendChild(item);
  });
  secao.appendChild(lista);
  return secao;
}

function mostrarDetalhes(detalhes) {
  conteudoDetalhes.appendChild(criar("h2", "detalhe-nome", detalhes.nome));
  const categoria = nomeDaCategoria(detalhes.categoria);
  if (categoria !== "") {
    conteudoDetalhes.appendChild(criar("p", "detalhe-categoria", categoria));
  }

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