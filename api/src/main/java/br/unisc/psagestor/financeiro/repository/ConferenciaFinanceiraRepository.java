package br.unisc.psagestor.financeiro.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** Conferência somente de leitura; executada no mesmo snapshot da consolidação. */
@Repository
public class ConferenciaFinanceiraRepository
        extends br.unisc.psagestor.comum.repository.ProjecaoNativa {
    public record Inconsistencia(
            String codigo,
            String mensagem,
            String recurso,
            long registroId,
            Long projetoId,
            Long contratoId,
            Long compromissoId) {}

    public List<Inconsistencia> verificar(Long projetoId) {
        var erros = new ArrayList<Inconsistencia>();
        conferir(
                erros,
                projetoId,
                "APORTE_INVALIDO",
                "Aporte com valor inválido, origem ausente ou projeto inexistente.",
                "aporte",
                """
            SELECT a.id, a.projeto_id, NULL contrato_id, NULL compromisso_id
            FROM aporte a LEFT JOIN projeto pr ON pr.id=a.projeto_id
            WHERE a.valor<=0 OR pr.id IS NULL OR TRIM(a.descricao)='' OR TRIM(pr.origem_recursos)=''
            """);
        conferir(
                erros,
                projetoId,
                "SALDO_INICIAL_DUPLICADO",
                "Há mais de um saldo inicial para o projeto; revisar os lançamentos identificados.",
                "aporte",
                """
            SELECT a.id, a.projeto_id, NULL contrato_id, NULL compromisso_id FROM aporte a
            WHERE a.tipo='SALDO_INICIAL' AND
            (SELECT COUNT(*) FROM aporte outro WHERE outro.projeto_id=a.projeto_id AND outro.tipo='SALDO_INICIAL')>1
            """);
        conferir(
                erros,
                projetoId,
                "CONTRATO_VINCULO_INVALIDO",
                "Contrato com projeto, produtor, propriedade ou cálculo ausente/incompatível.",
                "contrato",
                """
            SELECT c.id, c.projeto_id, c.id contrato_id, NULL compromisso_id
            FROM contrato c LEFT JOIN projeto pr ON pr.id=c.projeto_id
            LEFT JOIN produtor p ON p.id=c.produtor_id LEFT JOIN propriedade prop ON prop.id=c.propriedade_id
            LEFT JOIN calculo_psa ca ON ca.id=c.calculo_id LEFT JOIN diagnostico d ON d.id=ca.diagnostico_id
            WHERE c.status='APROVADO' AND (pr.id IS NULL OR p.id IS NULL OR prop.id IS NULL
              OR ca.id IS NULL OR d.id IS NULL OR prop.produtor_id<>c.produtor_id
              OR d.projeto_id<>c.projeto_id OR d.propriedade_id<>c.propriedade_id)
            """);
        conferir(
                erros,
                projetoId,
                "CONTRATO_SEM_COMPROMISSO",
                "Contrato aprovado sem compromisso para o exercício inicial.",
                "contrato",
                """
            SELECT c.id, c.projeto_id, c.id contrato_id, NULL compromisso_id FROM contrato c
            WHERE c.status='APROVADO' AND NOT EXISTS
            (SELECT 1 FROM compromisso_anual co WHERE co.contrato_id=c.id AND co.exercicio=YEAR(c.inicio))
            """);
        conferir(
                erros,
                projetoId,
                "COMPROMISSO_INVALIDO",
                "Compromisso sem contrato aprovado, com valor divergente do cálculo ou exercício incompatível.",
                "compromisso_anual",
                """
            SELECT co.id, c.projeto_id, co.contrato_id, co.id compromisso_id
            FROM compromisso_anual co LEFT JOIN contrato c ON c.id=co.contrato_id
            LEFT JOIN calculo_psa ca ON ca.id=c.calculo_id
            WHERE c.id IS NULL OR c.status<>'APROVADO' OR ca.id IS NULL OR co.valor<=0
               OR co.valor<>ca.valor_anual OR co.exercicio<>YEAR(c.inicio)
            """);
        conferir(
                erros,
                projetoId,
                "PAGAMENTO_INVALIDO",
                "Pagamento com valor inválido, referência ausente ou sem vínculo a contrato aprovado.",
                "pagamento",
                """
            SELECT p.id, c.projeto_id, co.contrato_id, p.compromisso_id FROM pagamento p
            LEFT JOIN compromisso_anual co ON co.id=p.compromisso_id
            LEFT JOIN contrato c ON c.id=co.contrato_id
            WHERE p.valor<=0 OR TRIM(p.referencia)='' OR co.id IS NULL OR c.id IS NULL OR c.status<>'APROVADO'
            """);
        conferir(
                erros,
                projetoId,
                "PAGAMENTOS_EXCEDEM_COMPROMISSO",
                "A soma dos pagamentos excede o compromisso; revisar os pagamentos identificados.",
                "pagamento",
                """
            SELECT p.id, c.projeto_id, co.contrato_id, p.compromisso_id FROM pagamento p
            JOIN compromisso_anual co ON co.id=p.compromisso_id JOIN contrato c ON c.id=co.contrato_id
            WHERE (SELECT SUM(outro.valor) FROM pagamento outro WHERE outro.compromisso_id=co.id)>co.valor
            """);
        // Conferir por projeto impede que sobra de um projeto oculte déficit de outro no total
        // geral.
        conferir(
                erros,
                projetoId,
                "RECURSOS_INSUFICIENTES",
                "Compromissos ou pagamentos excedem os recursos registrados neste projeto.",
                "projeto",
                """
            SELECT pr.id, pr.id projeto_id, NULL contrato_id, NULL compromisso_id FROM projeto pr
            WHERE (SELECT COALESCE(SUM(a.valor),0) FROM aporte a WHERE a.projeto_id=pr.id)
              < GREATEST(
                (SELECT COALESCE(SUM(co.valor),0) FROM compromisso_anual co JOIN contrato c ON c.id=co.contrato_id WHERE c.projeto_id=pr.id),
                (SELECT COALESCE(SUM(p.valor),0) FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id
                  JOIN contrato c ON c.id=co.contrato_id WHERE c.projeto_id=pr.id))
            """);
        return List.copyOf(erros);
    }

    private void conferir(
            List<Inconsistencia> erros,
            Long projetoId,
            String codigo,
            String mensagem,
            String recurso,
            String consulta) {
        String sql =
                "SELECT * FROM ("
                        + consulta
                        + ") ocorrencias"
                        + (projetoId == null ? "" : " WHERE projeto_id=?")
                        + " ORDER BY id";
        Object[] args = projetoId == null ? new Object[0] : new Object[] {projetoId};
        for (var row : linhas(sql, args))
            erros.add(
                    new Inconsistencia(
                            codigo,
                            mensagem,
                            recurso,
                            ((Number) row.get("id")).longValue(),
                            numero(row.get("projeto_id")),
                            numero(row.get("contrato_id")),
                            numero(row.get("compromisso_id"))));
    }

    private Long numero(Object valor) {
        return valor == null ? null : ((Number) valor).longValue();
    }
}
