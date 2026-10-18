package br.unisc.psagestor.cadastros.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "fonte_hidrica")
public class FonteHidrica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "propriedade_id", nullable = false)
    private Long propriedadeId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "protecao", nullable = false, length = 20)
    private String protecao;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

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

    public FonteHidrica() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getPropriedadeId() {
        return propriedadeId;
    }

    public void setPropriedadeId(Long value) {
        this.propriedadeId = value;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String value) {
        this.tipo = value;
    }

    public String getProtecao() {
        return protecao;
    }

    public void setProtecao(String value) {
        this.protecao = value;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean value) {
        this.ativo = value;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String value) {
        this.observacoes = value;
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
        out.put("propriedade_id", propriedadeId);
        out.put("tipo", tipo);
        out.put("protecao", protecao);
        out.put("ativo", ativo);
        out.put("observacoes", observacoes);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        out.put("atualizado_em", atualizadoEm == null ? null : Timestamp.valueOf(atualizadoEm));
        out.put("atualizado_por", atualizadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "propriedade_id", insertable = false, updatable = false)
    private Propriedade propriedade;

    public Propriedade getPropriedade() {
        return propriedade;
    }
}
