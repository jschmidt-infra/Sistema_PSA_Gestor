package br.unisc.psagestor.configuracao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "configuracao_vencimento")
public class ConfiguracaoVencimento {
    @Id private Long id;

    @Column(name = "antecedencia_dias", nullable = false)
    private int antecedenciaDias;

    protected ConfiguracaoVencimento() {}

    public int getAntecedenciaDias() {
        return antecedenciaDias;
    }

    public void definirAntecedencia(int dias) {
        this.antecedenciaDias = dias;
    }
}
