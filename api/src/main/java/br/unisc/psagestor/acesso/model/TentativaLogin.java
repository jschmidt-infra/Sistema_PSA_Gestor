package br.unisc.psagestor.acesso.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "tentativa_login")
public class TentativaLogin {
    @Id
    @Column(name = "email_chave", length = 64, columnDefinition = "CHAR(64)")
    private String emailChave;

    @Column(name = "falhas", nullable = false)
    private Integer falhas = 0;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "bloqueado_ate", columnDefinition = "DATETIME(6)")
    private LocalDateTime bloqueadoAte;

    public TentativaLogin() {}

    public String getEmailChave() {
        return emailChave;
    }

    public void setEmailChave(String value) {
        this.emailChave = value;
    }

    public Integer getFalhas() {
        return falhas;
    }

    public void setFalhas(Integer value) {
        this.falhas = value;
    }

    public LocalDateTime getBloqueadoAte() {
        return bloqueadoAte;
    }

    public void setBloqueadoAte(LocalDateTime value) {
        this.bloqueadoAte = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("email_chave", emailChave);
        out.put("falhas", falhas);
        out.put("bloqueado_ate", bloqueadoAte == null ? null : Timestamp.valueOf(bloqueadoAte));
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
