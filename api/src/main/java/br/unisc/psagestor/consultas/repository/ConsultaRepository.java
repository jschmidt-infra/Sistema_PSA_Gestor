package br.unisc.psagestor.consultas.repository;

import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.api.Pagina;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultaRepository extends br.unisc.psagestor.comum.repository.ProjecaoNativa {

    public ConsultaRepository() {}

    public record Totais(long quantidade, BigDecimal valor) {}

    public record Area(long produtores, BigDecimal area) {}

    private record Filtro(String sql, List<Object> args) {}

    private Filtro filtro(Long projeto, Long subBacia, LocalDate inicio, LocalDate fim) {
        String sql =
                " FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id JOIN contrato c ON c.id=co.contrato_id JOIN propriedade pr ON pr.id=c.propriedade_id WHERE 1=1";
        var args = new ArrayList<Object>();
        if (projeto != null) {
            sql += " AND c.projeto_id=?";
            args.add(projeto);
        }
        if (subBacia != null) {
            sql += " AND pr.sub_bacia_id=?";
            args.add(subBacia);
        }
        if (inicio != null) {
            sql += " AND p.data_pagamento>=?";
            args.add(inicio);
        }
        if (fim != null) {
            sql += " AND p.data_pagamento<=?";
            args.add(fim);
        }
        return new Filtro(sql, args);
    }

    public Pagina<Map<String, Object>> pagamentos(
            Long projetoId,
            Long subBaciaId,
            LocalDate dataInicio,
            LocalDate dataFim,
            int page,
            int size) {
        var f = filtro(projetoId, subBaciaId, dataInicio, dataFim);
        return pagina(
                "SELECT p.id,p.data_pagamento,p.valor,p.referencia,c.id contrato_id,c.projeto_id,c.produtor_id"
                        + f.sql(),
                page,
                size,
                f.args().toArray());
    }

    public Totais totais(Long projetoId, Long subBaciaId, LocalDate dataInicio, LocalDate dataFim) {
        var f = filtro(projetoId, subBaciaId, dataInicio, dataFim);
        var row =
                um(
                        "SELECT COUNT(*) quantidade,COALESCE(SUM(p.valor),0) total" + f.sql(),
                        f.args().toArray());
        return new Totais(Dados.numero(row, "quantidade"), Dados.decimal(row, "total"));
    }

    public Area area(Long projetoId, Long subBaciaId, LocalDate data) {
        String sql =
                "SELECT COUNT(DISTINCT c.produtor_id) produtores,COALESCE(SUM(ca.area_contratada),0) area FROM contrato c JOIN calculo_psa ca ON ca.id=c.calculo_id JOIN propriedade pr ON pr.id=c.propriedade_id WHERE c.status='APROVADO' AND c.inicio<=? AND c.fim>=?";
        var args = new ArrayList<Object>(List.of(data, data));
        if (projetoId != null) {
            sql += " AND c.projeto_id=?";
            args.add(projetoId);
        }
        if (subBaciaId != null) {
            sql += " AND pr.sub_bacia_id=?";
            args.add(subBaciaId);
        }
        var row = um(sql, args.toArray());
        return new Area(Dados.numero(row, "produtores"), Dados.decimal(row, "area"));
    }

    public br.unisc.psagestor.comum.api.Pagina<java.util.Map<String, Object>> historico(
            long produtorId,
            br.unisc.psagestor.contratos.model.FiltroVencimento filtro,
            int page,
            int size) {
        String sql =
                "SELECT c.id,c.numero,c.status,c.inicio,c.fim,ca.valor_anual,ca.pontuacao,ca.area_contratada,(SELECT COALESCE(SUM(p.valor),0) FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id WHERE co.contrato_id=c.id) total_pago FROM contrato c JOIN calculo_psa ca ON ca.id=c.calculo_id WHERE c.produtor_id=?";
        var args = new java.util.ArrayList<Object>();
        args.add(produtorId);
        if (filtro != null) {
            sql += " AND c.status='APROVADO'";
            if (filtro.de() != null) {
                sql += " AND c.fim>=?";
                args.add(filtro.de());
            }
            if (filtro.ate() != null) {
                sql += " AND c.fim<=?";
                args.add(filtro.ate());
            }
        }
        return pagina(sql, page, size, args.toArray());
    }

    public Object extrato(long produtorId, int page, int size) {
        return pagina(
                "SELECT p.id,p.data_pagamento,p.valor,p.referencia,p.observacao,c.id contrato_id,c.numero,c.projeto_id FROM pagamento p JOIN compromisso_anual co ON co.id=p.compromisso_id JOIN contrato c ON c.id=co.contrato_id WHERE c.produtor_id=?",
                page,
                size,
                produtorId);
    }
}
