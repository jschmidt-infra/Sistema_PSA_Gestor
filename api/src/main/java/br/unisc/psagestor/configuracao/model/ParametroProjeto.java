package br.unisc.psagestor.configuracao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "parametro_projeto")
public class ParametroProjeto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "projeto_id", nullable = false)
    private Long projetoId;

    @Column(name = "versao", nullable = false)
    private Integer versao;

    @Column(name = "valor_hectare", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorHectare;

    @Column(name = "percentual", nullable = false, precision = 9, scale = 6)
    private BigDecimal percentual;

    @Column(name = "configuracao_json", nullable = false, columnDefinition = "TEXT")
    private String configuracaoJson;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public ParametroProjeto() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(Long value) {
        this.projetoId = value;
    }

    public Integer getVersao() {
        return versao;
    }

    public void setVersao(Integer value) {
        this.versao = value;
    }

    public BigDecimal getValorHectare() {
        return valorHectare;
    }

    public void setValorHectare(BigDecimal value) {
        this.valorHectare = value;
    }

    public BigDecimal getPercentual() {
        return percentual;
    }

    public void setPercentual(BigDecimal value) {
        this.percentual = value;
    }

    public String getConfiguracaoJson() {
        return configuracaoJson;
    }

    public void setConfiguracaoJson(String value) {
        this.configuracaoJson = value;
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

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("projeto_id", projetoId);
        out.put("versao", versao);
        out.put("valor_hectare", valorHectare);
        out.put("percentual", percentual);
        out.put("configuracao_json", configuracaoJson);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", insertable = false, updatable = false)
    private Projeto projeto;

    public Projeto getProjeto() {
        return projeto;
    }
}
