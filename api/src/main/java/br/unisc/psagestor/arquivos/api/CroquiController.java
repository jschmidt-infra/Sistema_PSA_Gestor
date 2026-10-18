package br.unisc.psagestor.arquivos.api;

import br.unisc.psagestor.arquivos.service.CroquiService;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
public class CroquiController {
    private final CroquiService service;

    public CroquiController(CroquiService service) {
        this.service = service;
    }

    @PostMapping(value = "/propriedades/{id}/croquis", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('TECNICO')")
    public Object criar(
            @PathVariable long id,
            @RequestPart MultipartFile arquivo,
            @RequestParam BigDecimal areaPsa,
            @RequestParam(required = false) String observacoes) {
        long novo = service.criar(id, arquivo, areaPsa, observacoes);
        return ResponseEntity.created(URI.create("/api/v1/croquis/" + novo + "/arquivo"))
                .body(Map.of("id", novo));
    }

    @GetMapping("/propriedades/{id}/croquis")
    public Object listar(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(id, page, size);
    }

    @GetMapping("/croquis/{id}/arquivo")
    public ResponseEntity<byte[]> baixar(@PathVariable long id) {
        var c = service.baixar(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(c.mime()))
                .header(
                        "Content-Disposition",
                        ContentDisposition.attachment()
                                .filename(c.nome(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("Cache-Control", "no-store")
                .body(c.conteudo());
    }

    @PutMapping(value = "/croquis/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('TECNICO')")
    public Object editar(
            @PathVariable long id,
            @RequestPart MultipartFile arquivo,
            @RequestParam BigDecimal areaPsa,
            @RequestParam(required = false) String observacoes) {
        service.editar(id, arquivo, areaPsa, observacoes);
        return Map.of("id", id);
    }
}
