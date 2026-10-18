package br.unisc.psagestor.diagnostico.api;

import br.unisc.psagestor.diagnostico.service.DiagnosticoService;
import br.unisc.psagestor.diagnostico.service.DiagnosticoService.Calculo;
import br.unisc.psagestor.diagnostico.service.DiagnosticoService.Correcao;
import br.unisc.psagestor.diagnostico.service.DiagnosticoService.Criar;
import br.unisc.psagestor.diagnostico.service.DiagnosticoService.Parametros;
import br.unisc.psagestor.diagnostico.service.DiagnosticoService.Respostas;
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
public class DiagnosticoController {
    private final DiagnosticoService service;

    public DiagnosticoController(DiagnosticoService service) {
        this.service = service;
    }

    private Object criado(String rota, long id) {
        return ResponseEntity.created(URI.create("/api/v1/" + rota + "/" + id))
                .body(Map.of("id", id));
    }

    @GetMapping("/projetos/{id}/parametros")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','GESTOR','TECNICO')")
    public Object parametros(@PathVariable long id) {
        return service.consultarParametros(id);
    }

    @PutMapping("/projetos/{id}/parametros")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Object configurar(@PathVariable long id, @Valid @RequestBody Parametros p) {
        return service.configurar(id, p);
    }

    @PostMapping("/propriedades/{id}/diagnosticos")
    @PreAuthorize("hasRole('TECNICO')")
    public Object criar(@PathVariable long id, @Valid @RequestBody Criar c) {
        return criado("diagnosticos", service.criar(id, c));
    }

    @GetMapping("/propriedades/{id}/diagnosticos")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object listar(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(id, page, size);
    }

    @GetMapping("/diagnosticos/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object buscar(@PathVariable long id) {
        return service.consultar(id);
    }

    @GetMapping("/diagnosticos/{id}/questionario")
    public Object questionario(@PathVariable long id) {
        return service.questionario(id);
    }

    @PutMapping("/diagnosticos/{id}/respostas")
    @PreAuthorize("hasRole('TECNICO')")
    public Object respostas(@PathVariable long id, @Valid @RequestBody Respostas r) {
        service.responder(id, r);
        return buscar(id);
    }

    @PostMapping("/diagnosticos/{id}/concluir")
    @PreAuthorize("hasRole('TECNICO')")
    public Object concluir(@PathVariable long id) {
        service.concluir(id, false);
        return buscar(id);
    }

    @PostMapping("/diagnosticos/{id}/reapurar")
    @PreAuthorize("hasRole('TECNICO')")
    public Object reapurar(@PathVariable long id) {
        service.concluir(id, true);
        return buscar(id);
    }

    @PostMapping("/diagnosticos/{id}/abrir-correcao")
    @PreAuthorize("hasRole('TECNICO')")
    public Object corrigir(@PathVariable long id, @Valid @RequestBody Correcao c) {
        service.corrigir(id, c);
        return buscar(id);
    }

    @PostMapping("/calculos")
    public Object calcular(@Valid @RequestBody Calculo c) {
        return criado("calculos", service.calcular(c));
    }

    @GetMapping("/calculos/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object calculo(@PathVariable long id) {
        return service.consultarCalculo(id);
    }
}
