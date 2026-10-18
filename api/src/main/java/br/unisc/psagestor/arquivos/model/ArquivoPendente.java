package br.unisc.psagestor.arquivos.model;

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
@Table(name = "arquivo_pendente")
public class ArquivoPendente {
    @Id
    @Column(name = "chave", length = 255)
    private String chave;

    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.LOCAL_DATE_TIME)
    @Column(name = "criado_em", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime criadoEm;

    public ArquivoPendente() {}

    public String getChave() {
        return chave;
    }

    public void setChave(String value) {
        this.chave = value;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime value) {
        this.criadoEm = value;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("chave", chave);
        out.put("criado_em", criadoEm == null ? null : Timestamp.valueOf(criadoEm));
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.

}
