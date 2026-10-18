package br.unisc.psagestor.acesso.repository;

import br.unisc.psagestor.acesso.model.TentativaLogin;
import br.unisc.psagestor.comum.repository.EntidadeRepository;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TentativaLoginRepository extends EntidadeRepository<TentativaLogin, String> {
    @Modifying
    @Query(
            value =
                    "INSERT INTO tentativa_login(email_chave,falhas) VALUES (?1,0) ON DUPLICATE KEY UPDATE email_chave=VALUES(email_chave)",
            nativeQuery = true)
    void garantir(String chave);

    @Query(
            value = "SELECT email_chave FROM tentativa_login WHERE email_chave=?1 FOR UPDATE",
            nativeQuery = true)
    List<String> bloquearChave(String chave);

    @Modifying
    @Query(
            value =
                    "UPDATE tentativa_login SET falhas=0,bloqueado_ate=NULL WHERE email_chave=?1 AND bloqueado_ate<=CURRENT_TIMESTAMP(6)",
            nativeQuery = true)
    void expirar(String chave);

    @Query(
            value =
                    "SELECT falhas, GREATEST(0,COALESCE(CEIL(TIMESTAMPDIFF(MICROSECOND,CURRENT_TIMESTAMP(6),bloqueado_ate)/1000000.0),0)) segundos FROM tentativa_login WHERE email_chave=?1",
            nativeQuery = true)
    List<Map<String, Object>> estado(String chave);

    @Modifying
    @Query(
            value = "UPDATE tentativa_login SET falhas=0,bloqueado_ate=NULL WHERE email_chave=?1",
            nativeQuery = true)
    void zerar(String chave);

    @Modifying
    @Query(
            value =
                    "UPDATE tentativa_login SET falhas=?1,bloqueado_ate=CASE WHEN ?1=5 THEN TIMESTAMPADD(HOUR,1,CURRENT_TIMESTAMP(6)) ELSE NULL END WHERE email_chave=?2",
            nativeQuery = true)
    void registrarFalha(int falhas, String chave);
}
