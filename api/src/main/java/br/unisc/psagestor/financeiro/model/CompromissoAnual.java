package br.unisc.psagestor.financeiro.model;

import br.unisc.psagestor.contratos.model.Contrato;
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
@Table(name = "compromisso_anual")
public class CompromissoAnual {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "contrato_id", nullable = false)
    private Long contratoId;

    @Column(name = "exercicio", nullable = false)
    private Integer exercicio;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public CompromissoAnual() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getContratoId() {
        return contratoId;
    }

    public void setContratoId(Long value) {
        this.contratoId = value;
    }

    public Integer getExercicio() {
        return exercicio;
    }

    public void setExercicio(Integer value) {
        this.exercicio = value;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal value) {
        this.valor = value;
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
        out.put("contrato_id", contratoId);
        out.put("exercicio", exercicio);
        out.put("valor", valor);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrato_id", insertable = false, updatable = false)
    private Contrato contrato;

    public Contrato getContrato() {
        return contrato;
    }
}
