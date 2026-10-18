package br.unisc.psagestor.acesso.model;

import br.unisc.psagestor.cadastros.model.Produtor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.hibernate.type.SqlTypes;

@org.hibernate.annotations.DynamicUpdate
@Entity
@Table(name = "usuario")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Perfil perfil;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "produtor_id")
    private Long produtorId;

    @Column(name = "versao_credenciais", nullable = false)
    private long versaoCredenciais;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "criado_por")
    private Long criadoPor;

    @Column(name = "atualizado_em")
    private Instant atualizadoEm;

    @Column(name = "atualizado_por")
    private Long atualizadoPor;

    public Usuario() {}

    public Usuario(
            String nome,
            String email,
            String senhaHash,
            Perfil perfil,
            Long produtorId,
            Long criadoPor) {
        if ((perfil == Perfil.PRODUTOR) != (produtorId != null)) {
            throw new IllegalArgumentException(
                    "Somente conta de produtor deve possuir produtorId.");
        }
        this.nome = nome;
        this.email = email.strip().toLowerCase(Locale.ROOT);
        this.senhaHash = senhaHash;
        this.perfil = perfil;
        this.produtorId = produtorId;
        this.criadoPor = criadoPor;
        this.ativo = true;
        this.criadoEm = Instant.now();
    }

    public void alterarSenha(String hash, Long autor) {
        senhaHash = hash;
        versaoCredenciais++;
        atualizadoEm = Instant.now();
        atualizadoPor = autor;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public Perfil getPerfil() {
        return perfil;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public Long getProdutorId() {
        return produtorId;
    }

    public long getVersaoCredenciais() {
        return versaoCredenciais;
    }

    public void setId(Long value) {
        id = value;
    }

    public void setNome(String value) {
        nome = value;
    }

    public void setEmail(String value) {
        email = value;
    }

    public void setSenhaHash(String value) {
        senhaHash = value;
    }

    public void setPerfil(String value) {
        perfil = Perfil.valueOf(value);
    }

    public void setAtivo(Boolean value) {
        ativo = value;
    }

    public void setProdutorId(Long value) {
        produtorId = value;
    }

    public void setVersaoCredenciais(Long value) {
        versaoCredenciais = value;
    }

    public void setCriadoEm(Instant value) {
        criadoEm = value;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoPor(Long value) {
        criadoPor = value;
    }

    public Long getCriadoPor() {
        return criadoPor;
    }

    public void setAtualizadoEm(Instant value) {
        atualizadoEm = value;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoPor(Long value) {
        atualizadoPor = value;
    }

    public Long getAtualizadoPor() {
        return atualizadoPor;
    }

    public Map<String, Object> dados() {
        var out = new LinkedHashMap<String, Object>();
        out.put("id", id);
        out.put("nome", nome);
        out.put("email", email);
        out.put("senha_hash", senhaHash);
        out.put("perfil", perfil.name());
        out.put("ativo", ativo);
        out.put("produtor_id", produtorId);
        out.put("versao_credenciais", versaoCredenciais);
        out.put("criado_em", criadoEm == null ? null : Timestamp.from(criadoEm));
        out.put("criado_por", criadoPor);
        out.put("atualizado_em", atualizadoEm == null ? null : Timestamp.from(atualizadoEm));
        out.put("atualizado_por", atualizadoPor);
        return out;
    }

    // IDs são gravados explicitamente; navegação lazy não propaga exclusões.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produtor_id", insertable = false, updatable = false)
    private Produtor produtor;

    public Produtor getProdutor() {
        return produtor;
    }
}
