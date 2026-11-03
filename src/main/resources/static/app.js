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
  registrarNoHistorico(nome, dominioAtual, dominioAtual ? "categoria" : "texto");
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
    registrarNoHistorico(botao.dataset.termo, dominioAtual, "categoria");
    pesquisar(botao.dataset.termo, dominioAtual);
  });
});

let telaDeVolta = "resultados"; // para onde o "Voltar" dos detalhes leva (resultados ou favoritos)

botaoVoltar.addEventListener("click", () => {
  if (telaDeVolta === "favoritos") {
    mostrarFavoritos();
  } else {
    mostrarTela("resultados");
  }
});

// ---------- filtros dos resultados (REQ-07 / UC07) ----------

// Os filtros trabalham sobre o que a pesquisa já trouxe: não fazem nova chamada à API.
const areaFiltros = document.getElementById("filtros");
const filtroOrdem = document.getElementById("filtro-ordem");
const filtroMin = document.getElementById("filtro-min");
const filtroMax = document.getElementById("filtro-max");
let produtosComOfertas = []; // resultado da última pesquisa, na ordem da API
let prefixoAtual = "";       // texto antes da contagem (ex.: o nome reconhecido na foto)

filtroOrdem.addEventListener("change", aplicarFiltros);
filtroMin.addEventListener("input", aplicarFiltros);
filtroMax.addEventListener("input", aplicarFiltros);
document.getElementById("botao-limpar-filtros").addEventListener("click", () => {
  filtroOrdem.value = "relevancia";
  filtroMin.value = "";
  filtroMax.value = "";
  aplicarFiltros();
});

// campo vazio ou inválido = sem limite
function lerPreco(campo) {
  const valor = Number(campo.value.replace(",", "."));
  return campo.value.trim() === "" || !Number.isFinite(valor) || valor < 0 ? null : valor;
}

function aplicarFiltros() {
  const minimo = lerPreco(filtroMin);
  const maximo = lerPreco(filtroMax);

  let lista = produtosComOfertas.filter((p) =>
      (minimo === null || p.menorPreco >= minimo) && (maximo === null || p.menorPreco <= maximo));

  const ordem = filtroOrdem.value;
  if (ordem === "menor-preco") {
    lista = [...lista].sort((a, b) => a.menorPreco - b.menorPreco);
  } else if (ordem === "maior-preco") {
    lista = [...lista].sort((a, b) => b.menorPreco - a.menorPreco);
  } else if (ordem === "mais-ofertas") {
    lista = [...lista].sort((a, b) => b.quantidadeOfertas - a.quantidadeOfertas);
  }

  limpar(listaResultados);
  lista.forEach((produto) => listaResultados.appendChild(criarCard(produto)));

  const total = produtosComOfertas.length;
  const filtrando = minimo !== null || maximo !== null;
  if (minimo !== null && maximo !== null && minimo > maximo) {
    mostrarMensagem("O preço mínimo está maior que o máximo.", true);
  } else if (filtrando && lista.length === 0) {
    mostrarMensagem(prefixoAtual + "Nenhum dos " + total + " produtos está nessa faixa de preço.");
  } else if (filtrando) {
    mostrarMensagem(prefixoAtual + lista.length + " de " + total + " produtos nessa faixa de preço");
  } else {
    mostrarMensagem(prefixoAtual + total + (total === 1 ? " produto encontrado" : " produtos encontrados"));
  }
}

// ---------- favoritos (REQ-08 / UC08) ----------

// Guardados no próprio navegador (localStorage), como o histórico.
// O id do produto muda quando o servidor reinicia, então o favorito é identificado por nome + categoria;
// o id e o preço são atualizados sempre que o produto aparece de novo numa pesquisa ou nos detalhes.
const CHAVE_FAVORITOS = "chorameliga.favoritos";
const telaFavoritos = document.getElementById("tela-favoritos");
const listaFavoritos = document.getElementById("lista-favoritos");

function lerFavoritos() {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_FAVORITOS) || "[]");
    return Array.isArray(salvo)
        ? salvo.filter((f) => f && typeof f.nome === "string" && f.nome.trim() !== "")
        : [];
  } catch (e) {
    return [];
  }
}

function salvarFavoritos(favoritos) {
  try {
    localStorage.setItem(CHAVE_FAVORITOS, JSON.stringify(favoritos));
    return true;
  } catch (e) {
    return false;
  }
}

function mesmoProduto(a, b) {
  return a.nome.toLowerCase() === b.nome.toLowerCase() && (a.categoria || null) === (b.categoria || null);
}

function ehFavorito(produto) {
  return lerFavoritos().some((f) => mesmoProduto(f, produto));
}

function paraFavorito(produto) {
  return {
    id: produto.id,
    nome: produto.nome,
    categoria: produto.categoria || null,
    menorPreco: produto.menorPreco ?? null,
    quantidadeOfertas: produto.quantidadeOfertas ?? null,
    vistoEm: Date.now()
  };
}

// devolve true se o produto ficou favorito, false se deixou de ser
function alternarFavorito(produto) {
  const favoritos = lerFavoritos();
  const jaEra = favoritos.some((f) => mesmoProduto(f, produto));
  const novos = jaEra
      ? favoritos.filter((f) => !mesmoProduto(f, produto))
      : [paraFavorito(produto), ...favoritos];
  if (!salvarFavoritos(novos)) {
    mostrarMensagem("O navegador não deixou salvar os favoritos (talvez uma janela anônima).", true);
    return jaEra;
  }
  return !jaEra;
}

// o produto apareceu de novo: guarda o id e o preço mais recentes, se ele for favorito
function atualizarFavorito(produto) {
  const favoritos = lerFavoritos();
  const indice = favoritos.findIndex((f) => mesmoProduto(f, produto));
  if (indice >= 0) {
    favoritos[indice] = paraFavorito(produto);
    salvarFavoritos(favoritos);
  }
}

// estrela genérica: "estado" diz se já é favorito e "alternar" troca e devolve o novo estado
function criarEstrelaGenerica(rotulo, estado, alternar, aoMudar) {
  const estrela = criar("button", "estrela");
  estrela.type = "button";
  const pintar = (favorito) => {
    estrela.textContent = favorito ? "★" : "☆";
    estrela.classList.toggle("ativa", favorito);
    estrela.title = favorito ? "Remover dos favoritos" : "Adicionar aos favoritos";
    estrela.setAttribute("aria-label", estrela.title + ": " + rotulo);
    estrela.setAttribute("aria-pressed", String(favorito));
  };
  pintar(estado());
  estrela.addEventListener("click", (evento) => {
    evento.stopPropagation();
    const favorito = alternar();
    pintar(favorito);
    if (aoMudar) {
      aoMudar(favorito);
    }
  });
  return estrela;
}

function criarEstrela(produto, aoMudar) {
  return criarEstrelaGenerica(produto.nome, () => ehFavorito(produto), () => alternarFavorito(produto), aoMudar);
}

// ---------- ofertas favoritas ----------

// Uma oferta é identificada pelo produto (nome + categoria) e pela loja.
const CHAVE_OFERTAS_FAVORITAS = "chorameliga.ofertasFavoritas";
const listaOfertasFavoritas = document.getElementById("lista-ofertas-favoritas");
const tituloFavoritosProdutos = document.getElementById("titulo-favoritos-produtos");
const tituloFavoritosOfertas = document.getElementById("titulo-favoritos-ofertas");

function lerOfertasFavoritas() {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_OFERTAS_FAVORITAS) || "[]");
    return Array.isArray(salvo)
        ? salvo.filter((o) => o && typeof o.loja === "string" && o.produto && typeof o.produto.nome === "string")
        : [];
  } catch (e) {
    return [];
  }
}

function salvarOfertasFavoritas(ofertas) {
  try {
    localStorage.setItem(CHAVE_OFERTAS_FAVORITAS, JSON.stringify(ofertas));
    return true;
  } catch (e) {
    return false;
  }
}

function mesmaOferta(a, b) {
  return a.loja === b.loja && mesmoProduto(a.produto, b.produto);
}

function paraOfertaFavorita(oferta, produto) {
  return {
    produto: { id: produto.id, nome: produto.nome, categoria: produto.categoria || null },
    loja: oferta.loja,
    preco: oferta.preco,
    frete: oferta.frete,
    precoTotal: oferta.precoTotal,
    freteGratis: oferta.freteGratis,
    url: oferta.url,
    vistoEm: Date.now()
  };
}

function ehOfertaFavorita(oferta, produto) {
  const alvo = { loja: oferta.loja, produto };
  return lerOfertasFavoritas().some((o) => mesmaOferta(o, alvo));
}

function alternarOfertaFavorita(oferta, produto) {
  const nova = paraOfertaFavorita(oferta, produto);
  const atuais = lerOfertasFavoritas();
  const jaEra = atuais.some((o) => mesmaOferta(o, nova));
  const novas = jaEra ? atuais.filter((o) => !mesmaOferta(o, nova)) : [nova, ...atuais];
  if (!salvarOfertasFavoritas(novas)) {
    mostrarMensagem("O navegador não deixou salvar os favoritos (talvez uma janela anônima).", true);
    return jaEra;
  }
  return !jaEra;
}

// os detalhes do produto foram abertos: atualiza preço e frete das ofertas dele que são favoritas
function atualizarOfertasFavoritas(ofertas, produto) {
  const favoritas = lerOfertasFavoritas();
  let mudou = false;
  ofertas.forEach((oferta) => {
    const nova = paraOfertaFavorita(oferta, produto);
    const indice = favoritas.findIndex((o) => mesmaOferta(o, nova));
    if (indice >= 0) {
      favoritas[indice] = nova;
      mudou = true;
    }
  });
  if (mudou) {
    salvarOfertasFavoritas(favoritas);
  }
}

// tela de onde o usuário veio, para a seta "Voltar" dos favoritos
let telaAntesDosFavoritos = "inicio";

document.getElementById("botao-voltar-favoritos").addEventListener("click", () => {
  if (telaAntesDosFavoritos === "inicio") {
    voltarAoInicio();
  } else {
    mostrarTela(telaAntesDosFavoritos);
    mostrarMensagem("");
  }
});

function mostrarFavoritos() {
  // Mantém a origem já guardada ao redesenhar a própria tela (ex.: depois de desfavoritar) e ao
  // voltar de detalhes abertos a partir dos favoritos; senão o "Voltar" ficaria indo e vindo entre os dois.
  const vindoDeUmFavorito = telaAtual === "detalhes" && telaDeVolta === "favoritos";
  if (telaAtual !== "favoritos" && !vindoDeUmFavorito) {
    telaAntesDosFavoritos = telaAtual;
  }
  numeroPesquisa++; // uma pesquisa que ainda estava carregando não deve trocar de tela
  mostrarTela("favoritos");
  desenharFavoritos();
}

// ---------- filtros dos favoritos ----------

const filtrosFavoritos = document.getElementById("filtros-favoritos");
const filtroFavTexto = document.getElementById("filtro-fav-texto");
const filtroFavTipo = document.getElementById("filtro-fav-tipo");
const filtroFavCategoria = document.getElementById("filtro-fav-categoria");
const filtroFavOrdem = document.getElementById("filtro-fav-ordem");

filtroFavTexto.addEventListener("input", desenharFavoritos);
filtroFavTipo.addEventListener("change", desenharFavoritos);
filtroFavCategoria.addEventListener("change", desenharFavoritos);
filtroFavOrdem.addEventListener("change", desenharFavoritos);
document.getElementById("botao-limpar-filtros-favoritos").addEventListener("click", () => {
  filtroFavTexto.value = "";
  filtroFavTipo.value = "todos";
  filtroFavCategoria.value = "";
  filtroFavOrdem.value = "recentes";
  desenharFavoritos();
});

// sem acentos e em minúsculas, para "eletronico" achar "Eletrônico"
function normalizarTexto(texto) {
  return String(texto || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
}

// só oferece as categorias que existem nos favoritos; mantém a escolhida se ainda existir
function preencherCategoriasFavoritos(produtos, ofertas) {
  const codigos = new Set();
  produtos.forEach((p) => p.categoria && codigos.add(p.categoria));
  ofertas.forEach((o) => o.produto.categoria && codigos.add(o.produto.categoria));
  const escolhida = filtroFavCategoria.value;

  limpar(filtroFavCategoria);
  const todas = criar("option", "", "Todas");
  todas.value = "";
  filtroFavCategoria.appendChild(todas);
  [...codigos]
      .map((codigo) => ({ codigo, nome: nomeDaCategoria(codigo) || codigo }))
      .sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR"))
      .forEach(({ codigo, nome }) => {
        const opcao = criar("option", "", nome);
        opcao.value = codigo;
        filtroFavCategoria.appendChild(opcao);
      });
  filtroFavCategoria.value = codigos.has(escolhida) ? escolhida : "";
}

// preço nulo (produto sem oferta) fica sempre no fim
function compararPreco(a, b, crescente) {
  if (a == null && b == null) return 0;
  if (a == null) return 1;
  if (b == null) return -1;
  return crescente ? a - b : b - a;
}

// "recentes" mantém a ordem guardada (o mais recente primeiro)
function ordenarFavoritos(itens, ordem, preco, nome) {
  const copia = [...itens];
  if (ordem === "menor-preco") {
    copia.sort((a, b) => compararPreco(preco(a), preco(b), true));
  } else if (ordem === "maior-preco") {
    copia.sort((a, b) => compararPreco(preco(a), preco(b), false));
  } else if (ordem === "nome") {
    copia.sort((a, b) => nome(a).localeCompare(nome(b), "pt-BR"));
  }
  return copia;
}

function desenharFavoritos() {
  limpar(listaFavoritos);
  limpar(listaOfertasFavoritas);
  const todosProdutos = lerFavoritos();
  const todasOfertas = lerOfertasFavoritas();
  const total = todosProdutos.length + todasOfertas.length;

  filtrosFavoritos.hidden = total === 0;
  if (total === 0) {
    tituloFavoritosProdutos.hidden = true;
    tituloFavoritosOfertas.hidden = true;
    mostrarMensagem("Você ainda não tem favoritos. Clique na ☆ de um produto ou de uma oferta para guardá-lo aqui.");
    return;
  }
  preencherCategoriasFavoritos(todosProdutos, todasOfertas);

  const termo = normalizarTexto(filtroFavTexto.value.trim());
  const tipo = filtroFavTipo.value;
  const categoria = filtroFavCategoria.value;
  const ordem = filtroFavOrdem.value;

  // o texto procura no nome do produto e, nas ofertas, também no nome da loja
  const produtos = tipo === "ofertas" ? [] : ordenarFavoritos(
      todosProdutos.filter((p) =>
          (categoria === "" || p.categoria === categoria) && normalizarTexto(p.nome).includes(termo)),
      ordem, (p) => p.menorPreco, (p) => p.nome);
  const ofertas = tipo === "produtos" ? [] : ordenarFavoritos(
      todasOfertas.filter((o) =>
          (categoria === "" || o.produto.categoria === categoria)
          && (normalizarTexto(o.produto.nome).includes(termo) || normalizarTexto(o.loja).includes(termo))),
      ordem, (o) => o.precoTotal, (o) => o.produto.nome + " " + o.loja);

  tituloFavoritosProdutos.hidden = produtos.length === 0;
  tituloFavoritosOfertas.hidden = ofertas.length === 0;

  const mostrados = produtos.length + ofertas.length;
  const filtrando = termo !== "" || tipo !== "todos" || categoria !== "";
  if (mostrados === 0) {
    mostrarMensagem("Nenhum favorito com esses filtros.");
  } else if (filtrando) {
    mostrarMensagem(mostrados + " de " + total + " favoritos com esses filtros");
  } else {
    const partes = [];
    if (produtos.length > 0) {
      partes.push(produtos.length + (produtos.length === 1 ? " produto" : " produtos"));
    }
    if (ofertas.length > 0) {
      partes.push(ofertas.length + (ofertas.length === 1 ? " oferta" : " ofertas"));
    }
    mostrarMensagem("Favoritos: " + partes.join(" e "));
  }

  produtos.forEach((favorito) => {
    listaFavoritos.appendChild(criarCard(favorito, {
      aoClicar: () => abrirFavorito(favorito),
      rodape: typeof favorito.vistoEm === "number"
          ? "preço visto em " + new Date(favorito.vistoEm).toLocaleDateString("pt-BR")
          : "favorito",
      aoDesfavoritar: desenharFavoritos
    }));
  });

  ofertas.forEach((oferta) => {
    listaOfertasFavoritas.appendChild(criarOferta(oferta, false, oferta.produto, {
      mostrarProduto: true,
      aoDesfavoritar: desenharFavoritos
    }));
  });
}

// tenta abrir pelo id guardado; se o servidor reiniciou e o id mudou, pesquisa pelo nome
async function abrirFavorito(favorito) {
  telaDeVolta = "favoritos";
  mostrarTela("detalhes");
  limpar(conteudoDetalhes);
  mostrarMensagem("Carregando detalhes...");
  try {
    if (favorito.id == null) {
      throw new Error("sem id");
    }
    const detalhes = await chamarApi("/api/produtos/" + encodeURIComponent(favorito.id));
    if (!mesmoProduto(detalhes, favorito)) {
      throw new Error("id de outro produto");
    }
    mostrarMensagem("");
    mostrarDetalhes(detalhes);
  } catch (e) {
    campoNome.value = favorito.nome;
    dominioAtual = favorito.categoria || null;
    pesquisar(favorito.nome, dominioAtual);
  }
}

document.getElementById("menu-favoritos").addEventListener("click", (evento) => {
  evento.preventDefault();
  mostrarFavoritos();
});

// ---------- histórico de pesquisas (REQ-04 / UC04) ----------

// Guardado no próprio navegador (localStorage): cada navegador tem o seu, sem login.
// Se o navegador bloquear o armazenamento (ex.: janela anônima), o site funciona igual, só sem histórico.
const CHAVE_HISTORICO = "chorameliga.historico";
const MAXIMO_HISTORICO = 10;
const ORIGENS = {
  texto: { icone: "🔍", nome: "pesquisa digitada" },
  categoria: { icone: "🏷️", nome: "categoria" },
  voz: { icone: "🎤", nome: "pesquisa por voz" },
  foto: { icone: "📷", nome: "pesquisa por foto" }
};
const areaHistorico = document.getElementById("area-historico");
const listaHistorico = document.getElementById("lista-historico");

function lerHistorico() {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_HISTORICO) || "[]");
    // descarta o que não tiver o formato esperado (ex.: dado antigo ou editado à mão)
    return Array.isArray(salvo)
        ? salvo.filter((item) => item && typeof item.nome === "string" && item.nome.trim() !== "")
        : [];
  } catch (e) {
    return [];
  }
}

function salvarHistorico(itens) {
  try {
    localStorage.setItem(CHAVE_HISTORICO, JSON.stringify(itens));
  } catch (e) {
    // armazenamento bloqueado ou cheio: o histórico só não fica salvo
  }
}

function mesmaPesquisa(a, b) {
  return a.nome.toLowerCase() === b.nome.toLowerCase() && (a.dominio || null) === (b.dominio || null);
}

// a pesquisa mais recente vai para o topo; repetir uma pesquisa só a move para cima
function registrarNoHistorico(nome, dominio, origem) {
  const novo = { nome: nome.trim(), dominio: dominio || null, origem, quando: Date.now() };
  const itens = lerHistorico().filter((item) => !mesmaPesquisa(item, novo));
  itens.unshift(novo);
  salvarHistorico(itens.slice(0, MAXIMO_HISTORICO));
  mostrarHistorico();
}

function removerDoHistorico(alvo) {
  salvarHistorico(lerHistorico().filter((item) => !mesmaPesquisa(item, alvo)));
  mostrarHistorico();
}

function mostrarHistorico() {
  const itens = lerHistorico();
  limpar(listaHistorico);
  areaHistorico.hidden = itens.length === 0;

  itens.forEach((item) => {
    const origem = Object.hasOwn(ORIGENS, item.origem) ? ORIGENS[item.origem] : ORIGENS.texto;
    const li = criar("li", "historico-item");

    const botao = criar("button", "historico-pesquisar");
    botao.type = "button";
    botao.title = "Pesquisar de novo (" + origem.nome + formatarQuando(item.quando) + ")";
    botao.appendChild(criar("span", "historico-icone", origem.icone));
    botao.appendChild(criar("span", "historico-nome", item.nome));
    const categoria = nomeDaCategoria(item.dominio);
    if (categoria !== "") {
      botao.appendChild(criar("span", "historico-detalhe", "em " + categoria));
    }
    // refaz a pesquisa pelo nome; a de foto também, porque a foto não fica guardada
    botao.addEventListener("click", () => {
      campoNome.value = item.nome;
      dominioAtual = item.dominio || null;
      registrarNoHistorico(item.nome, item.dominio, item.origem);
      pesquisar(item.nome, dominioAtual);
    });

    const remover = criar("button", "historico-remover", "×");
    remover.type = "button";
    remover.title = "Remover do histórico";
    remover.setAttribute("aria-label", "Remover " + item.nome + " do histórico");
    remover.addEventListener("click", () => removerDoHistorico(item));

    li.appendChild(botao);
    li.appendChild(remover);
    listaHistorico.appendChild(li);
  });
}

function formatarQuando(quando) {
  if (typeof quando !== "number") {
    return "";
  }
  return ", " + new Date(quando).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

document.getElementById("botao-limpar-historico").addEventListener("click", () => {
  salvarHistorico([]);
  mostrarHistorico();
});

mostrarHistorico();

// ---------- pesquisa por foto ----------

const botaoFoto = document.getElementById("botao-foto");
const campoFoto = document.getElementById("campo-foto");
const previaFoto = document.getElementById("previa-foto");
const TIPOS_FOTO = ["image/jpeg", "image/png", "image/webp"];
const TAMANHO_MAXIMO_FOTO = 5 * 1024 * 1024; // mesmo limite do servidor
let urlPrevia = null;

botaoFoto.addEventListener("click", () => campoFoto.click());

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
    registrarNoHistorico(falado, null, "voz");
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

// "inicio" = busca + categorias; "resultados" = busca + lista; "detalhes" = só o produto;
// "favoritos" = só a lista de favoritos
let telaAtual = "inicio";

function mostrarTela(tela) {
  telaAtual = tela;
  telaBusca.hidden = tela === "detalhes" || tela === "favoritos";
  telaCategorias.hidden = tela !== "inicio";
  telaResultados.hidden = tela !== "resultados";
  telaFavoritos.hidden = tela !== "favoritos";
  telaDetalhes.hidden = tela !== "detalhes";
  botaoVoltar.textContent = telaDeVolta === "favoritos" ? "← Voltar aos favoritos" : "← Voltar aos resultados";
  window.scrollTo(0, 0);
}

function limparResultados() {
  produtosComOfertas = [];
  areaFiltros.hidden = true;
  limpar(listaResultados);
  limpar(areaSemOfertas);
}

function voltarAoInicio() {
  numeroPesquisa++; // invalida qualquer pesquisa que ainda esteja esperando resposta
  dominioAtual = null;
  limparResultados();
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
  limparResultados();
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
  limparResultados();
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
    // a foto em si não é guardada: o histórico guarda o nome reconhecido
    registrarNoHistorico(resultado.nomeReconhecido, null, "foto");
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

  telaDeVolta = "resultados";
  produtosComOfertas = comOfertas;
  prefixoAtual = prefixo;
  comOfertas.forEach(atualizarFavorito);

  if (comOfertas.length === 0) {
    areaFiltros.hidden = true;
    mostrarMensagem(prefixo + (semOfertas.length === 0
        ? "Não encontramos produtos para essa pesquisa. Tente outro termo ou outra categoria."
        : "Não encontramos ofertas para essa pesquisa no momento."));
  } else {
    // a faixa de preço é de cada pesquisa; a ordenação escolhida continua valendo
    filtroMin.value = "";
    filtroMax.value = "";
    areaFiltros.hidden = false;
    aplicarFiltros();
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

// opcoes (todas opcionais): aoClicar, rodape (texto da última linha) e aoDesfavoritar
function criarCard(produto, opcoes = {}) {
  // a estrela é um botão separado, porque um botão não pode ficar dentro de outro
  const caixa = criar("div", "card-caixa");
  const card = criar("button", "card");
  card.type = "button";
  card.appendChild(criar("span", "card-nome", produto.nome));
  const categoria = nomeDaCategoria(produto.categoria);
  if (categoria !== "") {
    card.appendChild(criar("span", "card-categoria", categoria));
  }
  card.appendChild(criar("span", "card-preco", produto.menorPreco != null
      ? "a partir de " + formatoMoeda.format(produto.menorPreco)
      : "sem ofertas no momento"));
  const rodape = opcoes.rodape
      || (produto.quantidadeOfertas + (produto.quantidadeOfertas === 1 ? " oferta" : " ofertas"));
  card.appendChild(criar("span", "card-ofertas", rodape));

  card.addEventListener("click", opcoes.aoClicar || (() => abrirDetalhes(produto.id)));

  caixa.appendChild(card);
  caixa.appendChild(criarEstrela(produto, (favorito) => {
    if (!favorito && opcoes.aoDesfavoritar) {
      opcoes.aoDesfavoritar();
    }
  }));
  return caixa;
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
  const produto = {
    id: detalhes.id,
    nome: detalhes.nome,
    categoria: detalhes.categoria,
    menorPreco: detalhes.menorPrecoAtual,
    quantidadeOfertas: detalhes.ofertas.length
  };
  atualizarFavorito(produto);

  const cabecalho = criar("div", "detalhe-cabecalho");
  cabecalho.appendChild(criar("h2", "detalhe-nome", detalhes.nome));
  cabecalho.appendChild(criarEstrela(produto));
  conteudoDetalhes.appendChild(cabecalho);
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

  atualizarOfertasFavoritas(detalhes.ofertas, produto);

  // o selo "Melhor preço" fica na oferta mais barata com frete, seja qual for a ordem escolhida
  const melhor = detalhes.ofertas.reduce((a, b) => (b.precoTotal < a.precoTotal ? b : a));

  const barra = criar("div", "filtros");
  const rotulo = criar("label", "filtro", "Ordenar ofertas por");
  const seletor = criar("select", "");
  ORDENS_OFERTAS.forEach((opcao) => {
    const item = criar("option", "", opcao.nome);
    item.value = opcao.valor;
    seletor.appendChild(item);
  });
  seletor.value = ordemOfertas;
  rotulo.appendChild(seletor);
  barra.appendChild(rotulo);
  conteudoDetalhes.appendChild(barra);

  const lista = criar("div", "ofertas");
  const desenhar = () => {
    limpar(lista);
    ordenarOfertas(detalhes.ofertas, ordemOfertas)
        .forEach((oferta) => lista.appendChild(criarOferta(oferta, oferta === melhor, produto)));
  };
  seletor.addEventListener("change", () => {
    ordemOfertas = seletor.value;
    desenhar();
  });
  desenhar();
  conteudoDetalhes.appendChild(lista);
}

// a ordem escolhida vale para os próximos produtos abertos (até recarregar a página)
const ORDENS_OFERTAS = [
  { valor: "total", nome: "Menor preço com frete" },
  { valor: "preco", nome: "Menor preço sem frete" },
  { valor: "maior", nome: "Maior preço com frete" },
  { valor: "frete", nome: "Menor frete" },
  { valor: "loja", nome: "Loja (A-Z)" }
];
let ordemOfertas = "total";

function ordenarOfertas(ofertas, ordem) {
  const copia = [...ofertas];
  if (ordem === "preco") {
    copia.sort((a, b) => a.preco - b.preco);
  } else if (ordem === "maior") {
    copia.sort((a, b) => b.precoTotal - a.precoTotal);
  } else if (ordem === "frete") {
    copia.sort((a, b) => a.frete - b.frete || a.precoTotal - b.precoTotal);
  } else if (ordem === "loja") {
    copia.sort((a, b) => a.loja.localeCompare(b.loja, "pt-BR"));
  } else {
    copia.sort((a, b) => a.precoTotal - b.precoTotal);
  }
  return copia;
}

// produto: a quem a oferta pertence (para a estrela). opcoes: mostrarProduto (na tela de favoritos,
// mostra o nome do produto com link para os detalhes) e aoDesfavoritar.
function criarOferta(oferta, melhor, produto, opcoes = {}) {
  const linha = criar("div", "oferta");
  if (melhor) {
    linha.classList.add("melhor");
    linha.appendChild(criar("span", "selo", "Melhor preço"));
  }
  if (produto) {
    linha.appendChild(criarEstrelaGenerica(
        oferta.loja + " (" + produto.nome + ")",
        () => ehOfertaFavorita(oferta, produto),
        () => alternarOfertaFavorita(oferta, produto),
        (favorito) => {
          if (!favorito && opcoes.aoDesfavoritar) {
            opcoes.aoDesfavoritar();
          }
        }));
  }
  if (opcoes.mostrarProduto) {
    const info = criar("span", "oferta-info");
    const nomeProduto = criar("button", "oferta-produto", produto.nome);
    nomeProduto.type = "button";
    nomeProduto.title = "Ver todas as ofertas deste produto";
    nomeProduto.addEventListener("click", () => abrirFavorito(produto));
    info.appendChild(nomeProduto);
    info.appendChild(criar("span", "oferta-loja", oferta.loja));
    if (typeof oferta.vistoEm === "number") {
      info.appendChild(criar("span", "oferta-visto",
          "preço visto em " + new Date(oferta.vistoEm).toLocaleDateString("pt-BR")));
    }
    linha.appendChild(info);
  } else {
    linha.appendChild(criar("span", "oferta-loja", oferta.loja));
  }
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