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
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "propriedade")
public class Propriedade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "produtor_id", nullable = false)
    private Long produtorId;

    @Column(name = "localidade_id", nullable = false)
    private Long localidadeId;

    @Column(name = "sub_bacia_id")
    private Long subBaciaId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "area_total", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaTotal;

    @Column(name = "car_situacao", nullable = false, length = 20)
    private String carSituacao;

    @Column(name = "car_numero", length = 100)
    private String carNumero;

    @Column(name = "fontes_situacao", nullable = false, length = 20)
    private String fontesSituacao = "NAO_AVALIADO";

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

    public Propriedade() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getProdutorId() {
        return produtorId;
    }

    public void setProdutorId(Long value) {
        this.produtorId = value;
    }

    public Long getLocalidadeId() {
        return localidadeId;
    }

    public void setLocalidadeId(Long value) {
        this.localidadeId = value;
    }

    public Long getSubBaciaId() {
        return subBaciaId;
    }

    public void setSubBaciaId(Long value) {
        this.subBaciaId = value;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String value) {
        this.nome = value;
    }

    public BigDecimal getAreaTotal() {
        return areaTotal;
    }

    public void setAreaTotal(BigDecimal value) {
        this.areaTotal = value;
    }

    public String getCarSituacao() {
        return carSituacao;
    }

    public void setCarSituacao(String value) {
        this.carSituacao = value;
    }

    public String getCarNumero() {
        return carNumero;
    }

    public void setCarNumero(String value) {
        this.carNumero = value;
    }

    public String getFontesSituacao() {
        return fontesSituacao;
    }

    public void setFontesSituacao(String value) {
        this.fontesSituacao = value;
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
        out.put("produtor_id", produtorId);
        out.put("localidade_id", localidadeId);
        out.put("sub_bacia_id", subBaciaId);
        out.put("nome", nome);
        out.put("area_total", areaTotal);
        out.put("car_situacao", carSituacao);
        out.put("car_numero", carNumero);
        out.put("fontes_situacao", fontesSituacao);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        out.put("atualizado_em", atualizadoEm == null ? null : Timestamp.valueOf(atualizadoEm));
        out.put("atualizado_por", atualizadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produtor_id", insertable = false, updatable = false)
    private Produtor produtor;

    public Produtor getProdutor() {
        return produtor;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "localidade_id", insertable = false, updatable = false)
    private Localidade localidade;

    public Localidade getLocalidade() {
        return localidade;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_bacia_id", insertable = false, updatable = false)
    private SubBacia subBacia;

    public SubBacia getSubBacia() {
        return subBacia;
    }
}
