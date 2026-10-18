package br.unisc.psagestor.financeiro.model;

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
@Table(name = "pagamento")
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "compromisso_id", nullable = false)
    private Long compromissoId;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "referencia", nullable = false, length = 100)
    private String referencia;

    @Column(name = "chave_operacao", nullable = false, length = 64)
    private String chaveOperacao;

    @Column(name = "hash_requisicao", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String hashRequisicao;

    @Column(name = "observacao", columnDefinition = "TEXT")
    private String observacao;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public Pagamento() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getCompromissoId() {
        return compromissoId;
    }

    public void setCompromissoId(Long value) {
        this.compromissoId = value;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate value) {
        this.dataPagamento = value;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal value) {
        this.valor = value;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String value) {
        this.referencia = value;
    }

    public String getChaveOperacao() {
        return chaveOperacao;
    }

    public void setChaveOperacao(String value) {
        this.chaveOperacao = value;
    }

    public String getHashRequisicao() {
        return hashRequisicao;
    }

    public void setHashRequisicao(String value) {
        this.hashRequisicao = value;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String value) {
        this.observacao = value;
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
        out.put("compromisso_id", compromissoId);
        out.put("data_pagamento", dataPagamento == null ? null : Date.valueOf(dataPagamento));
        out.put("valor", valor);
        out.put("referencia", referencia);
        out.put("chave_operacao", chaveOperacao);
        out.put("hash_requisicao", hashRequisicao);
        out.put("observacao", observacao);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("criado_por", criadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compromisso_id", insertable = false, updatable = false)
    private CompromissoAnual compromisso;

    public CompromissoAnual getCompromisso() {
        return compromisso;
    }
}
