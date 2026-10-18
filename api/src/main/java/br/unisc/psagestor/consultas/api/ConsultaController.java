package br.unisc.psagestor.consultas.api;

import br.unisc.psagestor.consultas.service.ConsultaService;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('GESTOR')")
public class ConsultaController {
    private final ConsultaService service;

    public ConsultaController(ConsultaService service) {
        this.service = service;
    }

    @GetMapping("/relatorios/aplicacao-recursos")
    public Object relatorio(
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) Long subBaciaId,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.relatorio(projetoId, subBaciaId, dataInicio, dataFim, page, size);
    }

    @GetMapping("/indicadores")
    public Object indicadores(
            @RequestParam(required = false) Long projetoId,
            @RequestParam(required = false) Long subBaciaId,
            @RequestParam(required = false) LocalDate dataReferencia,
            @RequestParam(required = false) LocalDate dataInicio,
            @RequestParam(required = false) LocalDate dataFim) {
        return service.indicadores(projetoId, subBaciaId, dataReferencia, dataInicio, dataFim);
    }

    @GetMapping("/meu-historico")
    @PreAuthorize("hasRole('PRODUTOR')")
    public Object historico(
            @RequestParam(required = false) String situacaoVencimento,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.historico(situacaoVencimento, page, size);
    }

    @GetMapping("/meu-historico/pagamentos")
    @PreAuthorize("hasRole('PRODUTOR')")
    public Object extrato(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.extrato(page, size);
    }
}
