package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.dto.DetalhesProduto;
import br.mackenzie.chorameliga.dto.ProdutoResumo;
import br.mackenzie.chorameliga.service.ProdutoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService service;

    public ProdutoController(ProdutoService service) {
        this.service = service;
    }

    // GET /api/produtos?nome=galaxy  (UC01)
    @GetMapping
    public List<ProdutoResumo> pesquisar(@RequestParam("nome") String nome) {
        return service.pesquisarResumos(nome);
    }

    // GET /api/produtos/1  (UC03)
    @GetMapping("/{id}")
    public DetalhesProduto detalhes(@PathVariable("id") long id) {
        return service.buscarDetalhes(id);
    }
}