package br.unisc.psagestor.acesso.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.bootstrap")
public record BootstrapProperties(boolean enabled, String nome, String email, String senha) {
    @Override
    public String toString() {
        return "BootstrapProperties[enabled=" + enabled + ", credenciais omitidas]";
    }
}
