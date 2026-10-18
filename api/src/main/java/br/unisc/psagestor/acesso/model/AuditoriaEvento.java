package br.unisc.psagestor.acesso.model;

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
@Table(name = "auditoria_evento")
public class AuditoriaEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "operacao", nullable = false, length = 60)
    private String operacao;

    @Column(name = "recurso", nullable = false, length = 60)
    private String recurso;

    @Column(name = "recurso_id", nullable = false)
    private Long recursoId;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    @Column(name = "requisicao_id", length = 64)
    private String requisicaoId;

    public AuditoriaEvento() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long value) {
        this.usuarioId = value;
    }

    public String getOperacao() {
        return operacao;
    }

    public void setOperacao(String value) {
        this.operacao = value;
    }

    public String getRecurso() {
        return recurso;
    }

    public void setRecurso(String value) {
        this.recurso = value;
    }

    public Long getRecursoId() {
        return recursoId;
    }

    public void setRecursoId(Long value) {
        this.recursoId = value;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime value) {
        this.criadoEm = value;
    }

    public String getRequisicaoId() {
        return requisicaoId;
    }

    public void setRequisicaoId(String value) {
        this.requisicaoId = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("usuario_id", usuarioId);
        out.put("operacao", operacao);
        out.put("recurso", recurso);
        out.put("recurso_id", recursoId);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        out.put("requisicao_id", requisicaoId);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    public Usuario getUsuario() {
        return usuario;
    }
}
