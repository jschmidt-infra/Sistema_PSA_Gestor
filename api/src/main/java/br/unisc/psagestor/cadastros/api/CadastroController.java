package br.unisc.psagestor.cadastros.api;

import br.unisc.psagestor.cadastros.api.CadastroEntradas.Fonte;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Levantamento;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Produtor;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Propriedade;
import br.unisc.psagestor.cadastros.api.CadastroEntradas.Visita;
import br.unisc.psagestor.cadastros.service.CadastroService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasAnyRole('GESTOR','TECNICO')")
public class CadastroController {
    private final CadastroService service;

    public CadastroController(CadastroService service) {
        this.service = service;
    }

    private ResponseEntity<?> criado(String rota, long id) {
        return ResponseEntity.created(URI.create("/api/v1/" + rota + "/" + id))
                .body(Map.of("id", id));
    }

    @GetMapping("/produtores")
    public Object produtores(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarProdutores(page, size);
    }

    @GetMapping("/produtores/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object produtor(@PathVariable long id) {
        return service.consultarProdutor(id);
    }

    @PostMapping("/produtores")
    @PreAuthorize("hasRole('TECNICO')")
    public Object criarProdutor(@Valid @RequestBody Produtor p) {
        return criado("produtores", service.produtor(null, p));
    }

    @PutMapping("/produtores/{id}")
    public Object editarProdutor(@PathVariable long id, @Valid @RequestBody Produtor p) {
        service.produtor(id, p);
        return produtor(id);
    }

    @GetMapping("/propriedades")
    public Object propriedades(
            @RequestParam(required = false) Long produtorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarPropriedades(produtorId, page, size);
    }

    @GetMapping("/propriedades/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object propriedade(@PathVariable long id) {
        return service.consultarPropriedade(id);
    }

    @PostMapping("/propriedades")
    @PreAuthorize("hasRole('TECNICO')")
    public Object criarPropriedade(@Valid @RequestBody Propriedade p) {
        return criado("propriedades", service.propriedade(null, p));
    }

    @PutMapping("/propriedades/{id}")
    public Object editarPropriedade(@PathVariable long id, @Valid @RequestBody Propriedade p) {
        service.propriedade(id, p);
        return propriedade(id);
    }

    @GetMapping("/propriedades/{id}/fontes-hidricas")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object fontes(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarFontes(id, page, size);
    }

    @PostMapping("/propriedades/{id}/fontes-hidricas")
    @PreAuthorize("hasRole('TECNICO')")
    public Object fonte(@PathVariable long id, @Valid @RequestBody Fonte f) {
        return criado("fontes-hidricas", service.fonte(id, null, f));
    }

    @PutMapping("/fontes-hidricas/{id}")
    @PreAuthorize("hasRole('TECNICO')")
    public Object editarFonte(@PathVariable long id, @Valid @RequestBody Fonte f) {
        return service.editarFonte(id, f);
    }

    @PutMapping("/propriedades/{id}/levantamento-hidrico")
    @PreAuthorize("hasRole('TECNICO')")
    public Object levantamento(@PathVariable long id, @Valid @RequestBody Levantamento l) {
        service.levantamento(id, l);
        return propriedade(id);
    }

    @GetMapping("/propriedades/{id}/visitas")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object visitas(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarVisitas(id, page, size);
    }

    @PostMapping("/propriedades/{id}/visitas")
    @PreAuthorize("hasRole('TECNICO')")
    public Object visita(@PathVariable long id, @Valid @RequestBody Visita v) {
        return criado("visitas", service.visita(id, null, v));
    }

    @PutMapping("/visitas/{id}")
    @PreAuthorize("hasRole('TECNICO')")
    public Object editarVisita(@PathVariable long id, @Valid @RequestBody Visita v) {
        return service.editarVisita(id, v);
    }

    @GetMapping("/localidades")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GESTOR','TECNICO')")
    public Object localidades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.localidades(page, size);
    }

    @GetMapping("/sub-bacias")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GESTOR','TECNICO')")
    public Object subBacias(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.subBacias(page, size);
    }

    @GetMapping("/projetos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GESTOR','TECNICO')")
    public Object projetos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.projetos(page, size);
    }
}
