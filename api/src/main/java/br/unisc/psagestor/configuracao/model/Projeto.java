package br.unisc.psagestor.configuracao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.LinkedHashMap;
import java.util.Map;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "projeto")
public class Projeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "origem_recursos", nullable = false, length = 255)
    private String origemRecursos;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    public Projeto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String value) {
        this.nome = value;
    }

    public String getOrigemRecursos() {
        return origemRecursos;
    }

    public void setOrigemRecursos(String value) {
        this.origemRecursos = value;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean value) {
        this.ativo = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("nome", nome);
        out.put("origem_recursos", origemRecursos);
        out.put("ativo", ativo);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
