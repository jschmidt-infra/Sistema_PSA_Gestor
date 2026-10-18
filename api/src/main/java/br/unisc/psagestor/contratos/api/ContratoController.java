package br.unisc.psagestor.contratos.api;

import br.unisc.psagestor.contratos.service.ContratoService;
import br.unisc.psagestor.contratos.service.ContratoService.Assinatura;
import br.unisc.psagestor.contratos.service.ContratoService.Entrada;
import br.unisc.psagestor.financeiro.service.FinanceiroService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/v1/contratos")
@PreAuthorize("hasRole('GESTOR')")
public class ContratoController {
    private final ContratoService service;
    private final FinanceiroService financeiro;

    public ContratoController(ContratoService service, FinanceiroService financeiro) {
        this.service = service;
        this.financeiro = financeiro;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO')")
    public Object listar(
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate vencimentoAte,
            @RequestParam(required = false) String situacaoVencimento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listar(projetoId, status, vencimentoAte, situacaoVencimento, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public Object buscar(@PathVariable long id) {
        return service.saida(id);
    }

    @PostMapping
    public Object criar(@Valid @RequestBody Entrada e) {
        long id = service.salvar(null, e);
        return ResponseEntity.created(URI.create("/api/v1/contratos/" + id))
                .body(service.saida(id));
    }

    @PutMapping("/{id}")
    public Object editar(@PathVariable long id, @Valid @RequestBody Entrada e) {
        service.salvar(id, e);
        return service.saida(id);
    }

    @PostMapping("/{id}/gerar-documento")
    public Object gerar(@PathVariable long id) {
        service.gerar(id);
        return Map.of("arquivoDisponivel", true);
    }

    @PostMapping("/{id}/assinatura-simulada")
    public Object assinar(@PathVariable long id, @Valid @RequestBody Assinatura a) {
        service.assinar(id, a);
        return service.saida(id);
    }

    @PostMapping("/{id}/aprovar")
    public Object aprovar(
            @PathVariable long id, @Valid @RequestBody FinanceiroService.Aprovacao a) {
        financeiro.aprovar(id, a);
        return service.saida(id);
    }

    @GetMapping("/{id}/documento")
    @PreAuthorize("hasAnyRole('GESTOR','TECNICO','PRODUTOR')")
    public ResponseEntity<byte[]> documento(@PathVariable long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=contrato-" + id + ".pdf")
                .header("Cache-Control", "no-store")
                .body(service.documento(id));
    }

    @GetMapping("/{id}/compromissos")
    public Object compromissos(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.compromissos(id, page, size);
    }
}
