package br.unisc.psagestor.financeiro.model;

import br.unisc.psagestor.configuracao.model.Projeto;
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
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "aporte")
public class Aporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "projeto_id", nullable = false)
    private Long projetoId;

    @Column(name = "tipo", nullable = false, length = 20)
    private String tipo;

    @Column(name = "data_aporte", nullable = false)
    private LocalDate dataAporte;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "descricao", nullable = false, length = 255)
    private String descricao;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public Aporte() {}

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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String value) {
        this.tipo = value;
    }

    public LocalDate getDataAporte() {
        return dataAporte;
    }

    public void setDataAporte(LocalDate value) {
        this.dataAporte = value;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal value) {
        this.valor = value;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String value) {
        this.descricao = value;
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
        out.put("tipo", tipo);
        out.put("data_aporte", dataAporte == null ? null : Date.valueOf(dataAporte));
        out.put("valor", valor);
        out.put("descricao", descricao);
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
