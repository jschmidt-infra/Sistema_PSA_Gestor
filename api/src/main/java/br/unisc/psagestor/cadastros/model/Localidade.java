package br.unisc.psagestor.cadastros.model;

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
@Table(name = "localidade")
public class Localidade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "municipio", nullable = false, length = 120)
    private String municipio;

    @Column(name = "uf", nullable = false, length = 2, columnDefinition = "CHAR(2)")
    private String uf;

    public Localidade() {}

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

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String value) {
        this.municipio = value;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String value) {
        this.uf = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("nome", nome);
        out.put("municipio", municipio);
        out.put("uf", uf);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
