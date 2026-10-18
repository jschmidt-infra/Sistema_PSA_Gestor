package br.unisc.psagestor.acesso.service;

import br.unisc.psagestor.acesso.repository.TentativaLoginRepository;
import br.unisc.psagestor.acesso.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Estado durável por e-mail; o bloqueio de linha serializa tentativas concorrentes. */
@Service
public class LimiteLogin {
    private final UsuarioRepository usuarioRepo;
    private final TentativaLoginRepository tentativaLoginRepo;
    private final AuditoriaService auditoria;

    public record Estado(int tentativasSemSucesso, boolean bloqueado, long segundosRestantes) {}

    private record Resultado(Authentication autenticacao, RuntimeException erro) {}

    public static class Bloqueado extends RuntimeException {
        private final long segundos;

        public Bloqueado(long segundos) {
            super(
                    "Muitas tentativas sem sucesso. Aguarde o desbloqueio ou procure um administrador.");
            this.segundos = segundos;
        }

        public long segundos() {
            return segundos;
        }
    }

    private final AuthenticationManager manager;
    private final TransactionTemplate tx;

    public LimiteLogin(
            AuthenticationManager manager,
            PlatformTransactionManager transactions,
            UsuarioRepository usuarioRepo,
            TentativaLoginRepository tentativaLoginRepo,
            AuditoriaService auditoria) {
        this.usuarioRepo = usuarioRepo;
        this.tentativaLoginRepo = tentativaLoginRepo;
        this.auditoria = auditoria;

        this.manager = manager;
        this.tx = new TransactionTemplate(transactions);
    }

    private String normalizar(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private String chave(String email) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(normalizar(email).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private Estado bloquearELer(String chave) {
        tentativaLoginRepo.garantir(chave);
        tentativaLoginRepo.bloquearChave(chave);
        // Relógio do banco avaliado após adquirir o bloqueio, inclusive após espera concorrente.
        tentativaLoginRepo.expirar(chave);
        return estado(chave);
    }

    private Estado estado(String chave) {
        var rows = tentativaLoginRepo.estado(chave);
        if (rows.isEmpty()) return new Estado(0, false, 0);
        var row = rows.getFirst();
        long segundos = ((Number) row.get("segundos")).longValue();
        int falhas = ((Number) row.get("falhas")).intValue();
        return new Estado(falhas == 5 && segundos == 0 ? 0 : falhas, segundos > 0, segundos);
    }

    public Authentication autenticar(String email, String senha) {
        String key = chave(email);
        Resultado resultado =
                tx.execute(
                        status -> {
                            Estado estado = bloquearELer(key);
                            if (estado.bloqueado())
                                return new Resultado(
                                        null, new Bloqueado(estado.segundosRestantes()));
                            try {
                                var auth =
                                        manager.authenticate(
                                                UsernamePasswordAuthenticationToken.unauthenticated(
                                                        normalizar(email), senha));
                                tentativaLoginRepo.zerar(key);
                                return new Resultado(auth, null);
                            } catch (AuthenticationException erro) {
                                // Falhas de infraestrutura não consomem tentativas de credenciais.
                                if (!(erro instanceof BadCredentialsException
                                        || erro instanceof AccountStatusException)) throw erro;
                                int falhas = estado.tentativasSemSucesso() + 1;
                                tentativaLoginRepo.registrarFalha(falhas, key);
                                return new Resultado(
                                        null,
                                        falhas == 5
                                                ? new Bloqueado(3600)
                                                : new BadCredentialsException(
                                                        "Credenciais não aceitas."));
                            }
                        });
        // Lançar somente depois do commit preserva o contador das tentativas recusadas.
        if (resultado.erro() != null) throw resultado.erro();
        return resultado.autenticacao();
    }

    public Estado consultar(long usuarioId) {
        return estado(chave((String) usuarioRepo.obterMapa(usuarioId).get("email")));
    }

    public void desbloquear(long usuarioId) {
        tx.executeWithoutResult(
                status -> {
                    String key = chave((String) usuarioRepo.obterMapa(usuarioId).get("email"));
                    bloquearELer(key);
                    tentativaLoginRepo.zerar(key);
                    auditoria.registrar("DESBLOQUEAR_LOGIN", "usuario", usuarioId);
                });
    }
}
