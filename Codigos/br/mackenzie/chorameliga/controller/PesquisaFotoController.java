package br.mackenzie.chorameliga.controller;

import br.mackenzie.chorameliga.dto.ResultadoPesquisaFoto;
import br.mackenzie.chorameliga.service.PesquisaPorFotoService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/produtos")
public class PesquisaFotoController {

    private final PesquisaPorFotoService service;

    public PesquisaFotoController(PesquisaPorFotoService service) {
        this.service = service;
    }

    // POST /api/produtos/foto  (UC02) — formulário multipart com o arquivo no campo "foto"
    @PostMapping(value = "/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResultadoPesquisaFoto pesquisarPorFoto(@RequestPart("foto") MultipartFile foto) throws IOException {
        return service.pesquisar(foto.getBytes());
    }
}
