package br.unisc.psagestor.diagnostico.model;

import br.unisc.psagestor.acesso.model.Usuario;
import br.unisc.psagestor.cadastros.model.Propriedade;
import br.unisc.psagestor.configuracao.model.ParametroProjeto;
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
@Table(name = "diagnostico")
public class Diagnostico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "propriedade_id", nullable = false)
    private Long propriedadeId;

    @Column(name = "projeto_id", nullable = false)
    private Long projetoId;

    @Column(name = "parametro_id", nullable = false)
    private Long parametroId;

    @Column(name = "tecnico_id", nullable = false)
    private Long tecnicoId;

    @Column(name = "data_visita", nullable = false)
    private LocalDate dataVisita;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "revisao", nullable = false)
    private Integer revisao;

    @Column(name = "respostas_json", nullable = false, columnDefinition = "TEXT")
    private String respostasJson;

    @Column(name = "pontuacao", precision = 9, scale = 6)
    private BigDecimal pontuacao;

    @Column(name = "memoria_json", columnDefinition = "TEXT")
    private String memoriaJson;

    @Column(name = "motivo_correcao", columnDefinition = "TEXT")
    private String motivoCorrecao;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "concluido_em", columnDefinition = "DATETIME(6)")
    private LocalDateTime concluidoEm;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "criado_por", nullable = false)
    private Long criadoPor;

    public Diagnostico() {}

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

    public Long getProjetoId() {
        return projetoId;
    }

    public void setProjetoId(Long value) {
        this.projetoId = value;
    }

    public Long getParametroId() {
        return parametroId;
    }

    public void setParametroId(Long value) {
        this.parametroId = value;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long value) {
        this.tecnicoId = value;
    }

    public LocalDate getDataVisita() {
        return dataVisita;
    }

    public void setDataVisita(LocalDate value) {
        this.dataVisita = value;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String value) {
        this.status = value;
    }

    public Integer getRevisao() {
        return revisao;
    }

    public void setRevisao(Integer value) {
        this.revisao = value;
    }

    public String getRespostasJson() {
        return respostasJson;
    }

    public void setRespostasJson(String value) {
        this.respostasJson = value;
    }

    public BigDecimal getPontuacao() {
        return pontuacao;
    }

    public void setPontuacao(BigDecimal value) {
        this.pontuacao = value;
    }

    public String getMemoriaJson() {
        return memoriaJson;
    }

    public void setMemoriaJson(String value) {
        this.memoriaJson = value;
    }

    public String getMotivoCorrecao() {
        return motivoCorrecao;
    }

    public void setMotivoCorrecao(String value) {
        this.motivoCorrecao = value;
    }

    public LocalDateTime getConcluidoEm() {
        return concluidoEm;
    }

    public void setConcluidoEm(LocalDateTime value) {
        this.concluidoEm = value;
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
        out.put("projeto_id", projetoId);
        out.put("parametro_id", parametroId);
        out.put("tecnico_id", tecnicoId);
        out.put("data_visita", dataVisita == null ? null : Date.valueOf(dataVisita));
        out.put("status", status);
        out.put("revisao", revisao);
        out.put("respostas_json", respostasJson);
        out.put("pontuacao", pontuacao);
        out.put("memoria_json", memoriaJson);
        out.put("motivo_correcao", motivoCorrecao);
        out.put("concluido_em", concluidoEm == null ? null : Timestamp.valueOf(concluidoEm));
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "projeto_id", insertable = false, updatable = false)
    private Projeto projeto;

    public Projeto getProjeto() {
        return projeto;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parametro_id", insertable = false, updatable = false)
    private ParametroProjeto parametro;

    public ParametroProjeto getParametro() {
        return parametro;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id", insertable = false, updatable = false)
    private Usuario tecnico;

    public Usuario getTecnico() {
        return tecnico;
    }
}
