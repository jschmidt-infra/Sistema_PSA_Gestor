package br.unisc.psagestor.financeiro.api;

import br.unisc.psagestor.financeiro.service.FinanceiroService;
import br.unisc.psagestor.financeiro.service.FinanceiroService.Aporte;
import br.unisc.psagestor.financeiro.service.FinanceiroService.Pagamento;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('GESTOR')")
public class FinanceiroController {
    private final FinanceiroService service;

    public FinanceiroController(FinanceiroService service) {
        this.service = service;
    }

    @PostMapping("/projetos/{id}/aportes")
    public Object aporte(@PathVariable long id, @Valid @RequestBody Aporte a) {
        long novo = service.aporte(id, a);
        return ResponseEntity.created(URI.create("/api/v1/projetos/" + id + "/aportes/" + novo))
                .body(service.consultarAporte(novo));
    }

    @GetMapping("/projetos/{id}/aportes")
    public Object aportes(
            @PathVariable long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarAportes(id, page, size);
    }

    @GetMapping("/financeiro/saldo")
    public Object saldo(@RequestParam(required = false) Long projetoId) {
        return service.saldo(projetoId);
    }

    @PostMapping("/pagamentos")
    public Object pagar(@Valid @RequestBody Pagamento p) {
        var r = service.pagar(p);
        return ResponseEntity.status(r.repetido() ? 200 : 201)
                .location(URI.create("/api/v1/pagamentos/" + r.pagamento().get("id")))
                .body(r.pagamento());
    }

    @GetMapping("/pagamentos/{id}")
    @PreAuthorize("hasAnyRole('GESTOR','PRODUTOR')")
    public Object pagamento(@PathVariable long id) {
        return service.consultarPagamento(id);
    }

    @GetMapping("/pagamentos")
    public Object pagamentos(
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) Long contratoId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(required = false) String chaveOperacao,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.listarPagamentos(
                projetoId, contratoId, dataInicio, dataFim, chaveOperacao, page, size);
    }
}
