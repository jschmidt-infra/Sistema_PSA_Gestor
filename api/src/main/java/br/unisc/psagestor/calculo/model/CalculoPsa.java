package br.unisc.psagestor.calculo.model;

import br.unisc.psagestor.configuracao.model.ParametroProjeto;
import br.unisc.psagestor.diagnostico.model.Diagnostico;
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
@Table(name = "calculo_psa")
public class CalculoPsa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "diagnostico_id", nullable = false)
    private Long diagnosticoId;

    @Column(name = "parametro_id", nullable = false)
    private Long parametroId;

    @Column(name = "revisao_diagnostico", nullable = false)
    private Integer revisaoDiagnostico;

    @Column(name = "memoria_json", nullable = false, columnDefinition = "TEXT")
    private String memoriaJson;

    @Column(name = "area_contratada", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaContratada;

    @Column(name = "valor_hectare", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorHectare;

    @Column(name = "percentual", nullable = false, precision = 9, scale = 6)
    private BigDecimal percentual;

    @Column(name = "pontuacao", nullable = false, precision = 9, scale = 6)
    private BigDecimal pontuacao;

    @Column(name = "valor_anual", nullable = false, precision = 15, scale = 2)
    private BigDecimal valorAnual;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public CalculoPsa() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getDiagnosticoId() {
        return diagnosticoId;
    }

    public void setDiagnosticoId(Long value) {
        this.diagnosticoId = value;
    }

    public Long getParametroId() {
        return parametroId;
    }

    public void setParametroId(Long value) {
        this.parametroId = value;
    }

    public Integer getRevisaoDiagnostico() {
        return revisaoDiagnostico;
    }

    public void setRevisaoDiagnostico(Integer value) {
        this.revisaoDiagnostico = value;
    }

    public String getMemoriaJson() {
        return memoriaJson;
    }

    public void setMemoriaJson(String value) {
        this.memoriaJson = value;
    }

    public BigDecimal getAreaContratada() {
        return areaContratada;
    }

    public void setAreaContratada(BigDecimal value) {
        this.areaContratada = value;
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

    public BigDecimal getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(BigDecimal value) {
        this.pontuacao = value;
    }

    public BigDecimal getValorAnual() {
        return valorAnual;
    }

    public void setValorAnual(BigDecimal value) {
        this.valorAnual = value;
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
        out.put("diagnostico_id", diagnosticoId);
        out.put("parametro_id", parametroId);
        out.put("revisao_diagnostico", revisaoDiagnostico);
        out.put("memoria_json", memoriaJson);
        out.put("area_contratada", areaContratada);
        out.put("valor_hectare", valorHectare);
        out.put("percentual", percentual);
        out.put("pontuacao", pontuacao);
        out.put("valor_anual", valorAnual);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagnostico_id", insertable = false, updatable = false)
    private Diagnostico diagnostico;

    public Diagnostico getDiagnostico() {
        return diagnostico;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parametro_id", insertable = false, updatable = false)
    private ParametroProjeto parametro;

    public ParametroProjeto getParametro() {
        return parametro;
    }
}
