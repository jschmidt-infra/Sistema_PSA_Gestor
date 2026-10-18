package br.unisc.psagestor.contratos.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class ContratoConsultaRepository extends br.unisc.psagestor.comum.repository.ProjecaoNativa {
    private final br.unisc.psagestor.financeiro.repository.CompromissoAnualRepository
            compromissoAnualRepo;

    public ContratoConsultaRepository(
            br.unisc.psagestor.financeiro.repository.CompromissoAnualRepository
                    compromissoAnualRepo) {
        this.compromissoAnualRepo = compromissoAnualRepo;
    }

    public br.unisc.psagestor.comum.api.Pagina<java.util.Map<String, Object>> listar(
            Long projetoId,
            String status,
            LocalDate vencimentoAte,
            br.unisc.psagestor.contratos.model.FiltroVencimento filtro,
            int page,
            int size) {
        String sql =
                "SELECT id,numero,produtor_id,propriedade_id,projeto_id,calculo_id,croqui_id,inicio,fim,status,aprovado_em FROM contrato WHERE 1=1";
        var args = new ArrayList<Object>();
        if (projetoId != null) {
            sql += " AND projeto_id=?";
            args.add(projetoId);
        }
        if (status != null) {
            sql += " AND status=?";
            args.add(status);
        }
        if (vencimentoAte != null) {
            sql += " AND fim<=?";
            args.add(vencimentoAte);
        }
        if (filtro != null) {
            sql += " AND status='APROVADO'";
            if (filtro.de() != null) {
                sql += " AND fim>=?";
                args.add(filtro.de());
            }
            if (filtro.ate() != null) {
                sql += " AND fim<=?";
                args.add(filtro.ate());
            }
        }
        return pagina(
                List.of(
                        campo("id"),
                        campo("numero"),
                        campo("produtor_id"),
                        campo("propriedade_id"),
                        campo("projeto_id"),
                        campo("calculo_id"),
                        campo("croqui_id"),
                        campo("inicio"),
                        campo("fim"),
                        campo("status"),
                        dataHora("aprovado_em")),
                sql,
                page,
                size,
                args.toArray());
    }

    public Object compromissos(long id, int page, int size) {
        return br.unisc.psagestor.comum.repository.Paginas.saida(
                compromissoAnualRepo.findAll(
                        (r, q, b) -> b.equal(r.get("contratoId"), id),
                        br.unisc.psagestor.comum.repository.Paginas.pedido(page, size)),
                br.unisc.psagestor.financeiro.model.CompromissoAnual::dados);
    }
}
