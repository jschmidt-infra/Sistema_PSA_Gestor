package br.unisc.psagestor.cadastros.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class CadastroEntradas {
    private CadastroEntradas() {}

    public record Produtor(
            @NotBlank @Size(max = 150) String nome,
            @Pattern(regexp = "[0-9]{11}|[0-9]{14}") String documento,
            @Size(max = 255) String endereco,
            @Size(max = 30) String telefone,
            @Email @Size(max = 254) String email) {}

    public record Propriedade(
            @NotNull @Positive Long produtorId,
            @NotNull @Positive Long localidadeId,
            @Positive Long subBaciaId,
            @NotBlank @Size(max = 150) String nome,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 8, fraction = 4) BigDecimal areaTotal,
            @NotNull CarSituacao carSituacao,
            @Size(max = 100) String carNumero) {}

    public enum CarSituacao {
        POSSUI,
        NAO_POSSUI,
        NAO_SE_APLICA
    }

    public enum TipoFonte {
        NASCENTE,
        RIO,
        SANGA,
        ACUDE
    }

    public enum Protecao {
        PROTEGIDA,
        NAO_PROTEGIDA,
        NAO_AVALIADA
    }

    public enum SituacaoFontes {
        AUSENTES,
        PRESENTES
    }

    public record Fonte(
            @NotNull TipoFonte tipo,
            @NotNull Protecao protecao,
            @NotNull Boolean ativo,
            @Size(max = 5000) String observacoes) {}

    public record Levantamento(@NotNull SituacaoFontes situacao) {}

    public record Visita(
            @NotNull @PastOrPresent LocalDate dataVisita,
            @NotBlank @Size(max = 5000) String observacoes) {}
}
