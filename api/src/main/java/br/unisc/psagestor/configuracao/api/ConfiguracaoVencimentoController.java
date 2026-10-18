package br.unisc.psagestor.configuracao.api;

import br.unisc.psagestor.configuracao.service.ConfiguracaoVencimentoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/configuracoes/vencimento-contratos")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class ConfiguracaoVencimentoController {
    private final ConfiguracaoVencimentoService service;

    public ConfiguracaoVencimentoController(ConfiguracaoVencimentoService service) {
        this.service = service;
    }

    @GetMapping
    public ConfiguracaoVencimentoService.Saida consultar() {
        return service.consultar();
    }

    @PutMapping
    public ConfiguracaoVencimentoService.Saida alterar(
            @Valid @RequestBody ConfiguracaoVencimentoService.Entrada entrada) {
        return service.alterar(entrada);
    }
}
