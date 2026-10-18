package br.unisc.psagestor.contratos.model;

import br.unisc.psagestor.arquivos.model.Croqui;
import br.unisc.psagestor.cadastros.model.Produtor;
import br.unisc.psagestor.cadastros.model.Propriedade;
import br.unisc.psagestor.calculo.model.CalculoPsa;
import br.unisc.psagestor.configuracao.model.Projeto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "contrato")
public class Contrato {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    @Column(name = "produtor_id", nullable = false)
    private Long produtorId;

    @Column(name = "propriedade_id", nullable = false)
    private Long propriedadeId;

    @Column(name = "projeto_id", nullable = false)
    private Long projetoId;

    @Column(name = "calculo_id", nullable = false)
    private Long calculoId;

    @Column(name = "croqui_id")
    private Long croquiId;

    @Column(name = "inicio", nullable = false)
    private LocalDate inicio;

    @Column(name = "fim", nullable = false)
    private LocalDate fim;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "documento_chave", length = 255)
    private String documentoChave;

    @Column(name = "documento_snapshot", columnDefinition = "TEXT")
    private String documentoSnapshot;

    @Column(name = "modelo_versao", length = 30)
    private String modeloVersao;

    @Column(name = "assinatura_em")
    private LocalDate assinaturaEm;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "aprovado_em", columnDefinition = "DATETIME(6)")
    private LocalDateTime aprovadoEm;

    @Column(name = "aprovado_por")
    private Long aprovadoPor;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public Contrato() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String value) {
        this.numero = value;
    }

    public Long getProdutorId() {
        return produtorId;
    }

    public void setProdutorId(Long value) {
        this.produtorId = value;
    }

    public Long getPropriedadeId() {
        return propriedadeId;
    }

    public void setPropriedadeId(Long value) {
        this.propriedadeId = value;
    }

    public Long getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(Long value) {
        this.projetoId = value;
    }

    public Long getCalculoId() {
        return calculoId;
    }

    public void setCalculoId(Long value) {
        this.calculoId = value;
    }

    public Long getCroquiId() {
        return croquiId;
    }

    public void setCroquiId(Long value) {
        this.croquiId = value;
    }

    public LocalDate getInicio() {
        return inicio;
    }

    public void setInicio(LocalDate value) {
        this.inicio = value;
    }

    public LocalDate getFim() {
        return fim;
    }

    public void setFim(LocalDate value) {
        this.fim = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public String getDocumentoChave() {
        return documentoChave;
    }

    public void setDocumentoChave(String value) {
        this.documentoChave = value;
    }

    public String getDocumentoSnapshot() {
        return documentoSnapshot;
    }

    public void setDocumentoSnapshot(String value) {
        this.documentoSnapshot = value;
    }

    public String getModeloVersao() {
        return modeloVersao;
    }

    public void setModeloVersao(String value) {
        this.modeloVersao = value;
    }

    public LocalDate getAssinaturaEm() {
        return assinaturaEm;
    }

    public void setAssinaturaEm(LocalDate value) {
        this.assinaturaEm = value;
    }

    public LocalDateTime getAprovadoEm() {
        return aprovadoEm;
    }

    public void setAprovadoEm(LocalDateTime value) {
        this.aprovadoEm = value;
    }

    public Long getAprovadoPor() {
        return aprovadoPor;
    }

    public void setAprovadoPor(Long value) {
        this.aprovadoPor = value;
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
        out.put("numero", numero);
        out.put("produtor_id", produtorId);
        out.put("propriedade_id", propriedadeId);
        out.put("projeto_id", projetoId);
        out.put("calculo_id", calculoId);
        out.put("croqui_id", croquiId);
        out.put("inicio", inicio == null ? null : Date.valueOf(inicio));
        out.put("fim", fim == null ? null : Date.valueOf(fim));
        out.put("status", status);
        out.put("documento_chave", documentoChave);
        out.put("documento_snapshot", documentoSnapshot);
        out.put("modelo_versao", modeloVersao);
        out.put("assinatura_em", assinaturaEm == null ? null : Date.valueOf(assinaturaEm));
        out.put("aprovado_em", aprovadoEm == null ? null : Timestamp.valueOf(aprovadoEm));
        out.put("aprovado_por", aprovadoPor);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
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
    @JoinColumn(name = "propriedade_id", insertable = false, updatable = false)
    private Propriedade propriedade;

    public Propriedade getPropriedade() {
        return propriedade;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", insertable = false, updatable = false)
    private Projeto projeto;

    public Projeto getProjeto() {
        return projeto;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculo_id", insertable = false, updatable = false)
    private CalculoPsa calculo;

    public CalculoPsa getCalculo() {
        return calculo;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "croqui_id", insertable = false, updatable = false)
    private Croqui croqui;

    public Croqui getCroqui() {
        return croqui;
    }
}
