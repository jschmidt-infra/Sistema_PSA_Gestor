package br.unisc.psagestor.acesso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "evento_acesso")
public class EventoAcesso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "evento", nullable = false, length = 30)
    private String evento;

    @Column(name = "motivo", nullable = false, length = 60)
    private String motivo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "requisicao_id", length = 36, columnDefinition = "CHAR(36)")
    private String requisicaoId;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm = LocalDateTime.now();

    public EventoAcesso() {}

    public Long getId() {
        return id;
    }

    public void setId(Long value) {
        this.id = value;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String value) {
        this.evento = value;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String value) {
        this.motivo = value;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long value) {
        this.usuarioId = value;
    }

    public String getRequisicaoId() {
        return requisicaoId;
    }

    public void setRequisicaoId(String value) {
        this.requisicaoId = value;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime value) {
        this.criadoEm = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("evento", evento);
        out.put("motivo", motivo);
        out.put("usuario_id", usuarioId);
        out.put("requisicao_id", requisicaoId);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
