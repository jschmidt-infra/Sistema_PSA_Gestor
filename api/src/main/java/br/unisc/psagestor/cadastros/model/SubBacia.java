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
@Table(name = "sub_bacia")
public class SubBacia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    public SubBacia() {}

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

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("nome", nome);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
