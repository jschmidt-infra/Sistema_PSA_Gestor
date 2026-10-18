package br.unisc.psagestor.financeiro.repository;

import br.unisc.psagestor.comum.api.Dados;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class FinanceiroConsultaRepository
        extends br.unisc.psagestor.comum.repository.ProjecaoNativa {
    private final br.unisc.psagestor.financeiro.repository.PagamentoRepository pagamentoRepo;
    private final br.unisc.psagestor.financeiro.repository.AporteRepository aporteRepo;

    public FinanceiroConsultaRepository(
            br.unisc.psagestor.financeiro.repository.PagamentoRepository pagamentoRepo,
            br.unisc.psagestor.financeiro.repository.AporteRepository aporteRepo) {
        this.pagamentoRepo = pagamentoRepo;
        this.aporteRepo = aporteRepo;
    }

    public Object aporte(long id) {
        return Dados.saida(aporteRepo.obterMapa(id));
    }

    public Object aportes(long id, int page, int size) {
        return br.unisc.psagestor.comum.repository.Paginas.saida(
                aporteRepo.findAll(
                        (r, q, b) -> b.equal(r.get("projetoId"), id),
                        br.unisc.psagestor.comum.repository.Paginas.pedido(page, size)),
                br.unisc.psagestor.financeiro.model.Aporte::dados);
    }

    public Map<String, Object> pagamento(long id) {
        return pagamentoRepo.obterMapa(id);
    }

    public long produtorCompromisso(long id) {
        return Dados.numero(
                um(
                        "SELECT c.produtor_id FROM contrato c JOIN compromisso_anual co ON co.contrato_id=c.id WHERE co.id=?",
                        id),
                "produtor_id");
    }

    public Object pagamentos(
            Long projetoId,
            Long contratoId,
            LocalDate dataInicio,
            LocalDate dataFim,
            String chaveOperacao,
            int page,
            int size) {
        String sql =
                "SELECT p.id,p.compromisso_id,p.valor,p.data_pagamento,p.referencia,p.chave_operacao,p.observacao,p.criado_em,p.criado_por,c.id contrato_id,c.projeto_id,c.produtor_id FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id JOIN contrato c ON c.id=co.contrato_id WHERE 1=1";
        var args = new ArrayList<Object>();
        if (projetoId != null) {
            sql += " AND c.projeto_id=?";
            args.add(projetoId);
        }
        if (contratoId != null) {
            sql += " AND c.id=?";
            args.add(contratoId);
        }
        if (dataInicio != null) {
            sql += " AND p.data_pagamento>=?";
            args.add(dataInicio);
        }
        if (dataFim != null) {
            sql += " AND p.data_pagamento<=?";
            args.add(dataFim);
        }
        if (chaveOperacao != null) {
            sql += " AND p.chave_operacao=?";
            args.add(chaveOperacao);
        }
        return pagina(
                List.of(
                        campo("id"),
                        campo("compromisso_id"),
                        campo("valor"),
                        campo("data_pagamento"),
                        campo("referencia"),
                        campo("chave_operacao"),
                        campo("observacao"),
                        dataHora("criado_em"),
                        campo("criado_por"),
                        campo("contrato_id"),
                        campo("projeto_id"),
                        campo("produtor_id")),
                sql,
                page,
                size,
                args.toArray());
    }

    public Map<String, Object> totais(Long projeto) {
        // Uma única instrução fornece uma visão consistente de recursos, reservas e pagamentos.
        String filtro = projeto == null ? "" : " WHERE a.projeto_id=?";
        String fc = projeto == null ? "" : " WHERE c.projeto_id=?";
        String sql =
                "SELECT (SELECT COALESCE(SUM(a.valor),0) FROM aporte a"
                        + filtro
                        + ") recursos, (SELECT COALESCE(SUM(co.valor),0) FROM compromisso_anual co JOIN contrato c ON c.id=co.contrato_id"
                        + fc
                        + ") reservas, (SELECT COALESCE(SUM(p.valor),0) FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id JOIN contrato c ON c.id=co.contrato_id"
                        + fc
                        + ") pagos";
        return projeto == null ? um(sql) : um(sql, projeto, projeto, projeto);
    }
}
