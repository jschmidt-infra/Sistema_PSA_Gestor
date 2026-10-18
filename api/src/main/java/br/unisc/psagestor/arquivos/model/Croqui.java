package br.unisc.psagestor.arquivos.model;

import br.unisc.psagestor.cadastros.model.Propriedade;
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
@Table(name = "croqui")
public class Croqui {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "propriedade_id", nullable = false)
    private Long propriedadeId;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String nomeOriginal;

    @Column(name = "chave", nullable = false, length = 255)
    private String chave;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "tamanho", nullable = false)
    private Long tamanho;

    @Column(name = "area_psa", nullable = false, precision = 12, scale = 4)
    private BigDecimal areaPsa;

    @Column(name = "observacoes", columnDefinition = "TEXT")
    private String observacoes;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public Croqui() {}

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

    public String getNomeOriginal() {
        return nomeOriginal;
    }

    public void setNomeOriginal(String value) {
        this.nomeOriginal = value;
    }

    public String getChave() {
        return chave;
    }

    public void setChave(String value) {
        this.chave = value;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String value) {
        this.tipoMime = value;
    }

    public Long getTamanho() {
        return tamanho;
    }

    public void setTamanho(Long value) {
        this.tamanho = value;
    }

    public BigDecimal getAreaPsa() {
        return areaPsa;
    }

    public void setAreaPsa(BigDecimal value) {
        this.areaPsa = value;
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

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("propriedade_id", propriedadeId);
        out.put("nome_original", nomeOriginal);
        out.put("chave", chave);
        out.put("tipo_mime", tipoMime);
        out.put("tamanho", tamanho);
        out.put("area_psa", areaPsa);
        out.put("observacoes", observacoes);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
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
