package br.unisc.psagestor.configuracao.service;

import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.configuracao.repository.ConfiguracaoVencimentoRepository;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfiguracaoVencimentoService {
    private final ConfiguracaoVencimentoRepository repository;
    private final AuditoriaService auditoria;

    public ConfiguracaoVencimentoService(
            ConfiguracaoVencimentoRepository repository, AuditoriaService auditoria) {
        this.repository = repository;
        this.auditoria = auditoria;
    }

    public record Entrada(
            @NotNull @DecimalMin("0") @DecimalMax("3650") @Digits(integer = 4, fraction = 0)
                    BigDecimal antecedenciaDias) {}

    public record Saida(int antecedenciaDias) {}

    public Saida consultar() {
        return new Saida(repository.findById(1L).orElseThrow().getAntecedenciaDias());
    }

    @Transactional
    public Saida alterar(Entrada entrada) {
        var configuracao = repository.findById(1L).orElseThrow();
        configuracao.definirAntecedencia(entrada.antecedenciaDias().intValueExact());
        auditoria.registrar("ALTERAR", "configuracao_vencimento", 1L);
        return new Saida(configuracao.getAntecedenciaDias());
    }
}
