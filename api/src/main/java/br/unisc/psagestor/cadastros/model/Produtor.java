package br.unisc.psagestor.cadastros.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "produtor")
public class Produtor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "documento", length = 14)
    private String documento;

    @Column(name = "endereco", length = 255)
    private String endereco;

    @Column(name = "telefone", length = 30)
    private String telefone;

    @Column(name = "email", length = 254)
    private String email;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "atualizado_em", columnDefinition = "DATETIME(6)")
    private LocalDateTime atualizadoEm;

    @Column(name = "atualizado_por")
    private Long atualizadoPor;

    public Produtor() {}

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

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String value) {
        this.documento = value;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String value) {
        this.endereco = value;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String value) {
        this.telefone = value;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String value) {
        this.email = value;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime value) {
        this.criadoEm = value;
    }

    public Long getCriadoPor() {
        return criadoPor;
    }

    public void setCriadoPor(Long value) {
        this.criadoPor = value;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime value) {
        this.atualizadoEm = value;
    }

    public Long getAtualizadoPor() {
        return atualizadoPor;
    }

    public void setAtualizadoPor(Long value) {
        this.atualizadoPor = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("nome", nome);
        out.put("documento", documento);
        out.put("endereco", endereco);
        out.put("telefone", telefone);
        out.put("email", email);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        out.put("atualizado_em", atualizadoEm == null ? null : Timestamp.valueOf(atualizadoEm));
        out.put("atualizado_por", atualizadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
