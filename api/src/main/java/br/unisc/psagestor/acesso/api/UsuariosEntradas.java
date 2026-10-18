package br.unisc.psagestor.acesso.api;

import br.unisc.psagestor.acesso.model.Perfil;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public final class UsuariosEntradas {
    public record Entrada(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull Perfil perfil,
            @Positive Long produtorId,
            @NotNull Boolean ativo) {}

    public record Criar(
            @NotBlank @Size(max = 150) String nome,
            @NotBlank @Email @Size(max = 254) String email,
            @NotNull Perfil perfil,
            @Positive Long produtorId,
            @NotNull Boolean ativo,
            @NotBlank @Size(min = 12, max = 72) String senha) {}

    public record Senha(@NotBlank @Size(min = 12, max = 72) String novaSenha) {}
}
