package br.unisc.psagestor.diagnostico.repository;

import br.unisc.psagestor.cadastros.repository.PropriedadeRepository;
import br.unisc.psagestor.calculo.repository.CalculoPsaRepository;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.repository.Paginas;
import br.unisc.psagestor.diagnostico.model.Diagnostico;
import org.springframework.stereotype.Repository;

@Repository
public class DiagnosticoConsultaRepository {
    private final PropriedadeRepository propriedadeRepo;
    private final DiagnosticoRepository diagnosticoRepo;
    private final CalculoPsaRepository calculoPsaRepo;

    public DiagnosticoConsultaRepository(
            PropriedadeRepository propriedadeRepo,
            DiagnosticoRepository diagnosticoRepo,
            CalculoPsaRepository calculoPsaRepo) {
        this.propriedadeRepo = propriedadeRepo;
        this.diagnosticoRepo = diagnosticoRepo;
        this.calculoPsaRepo = calculoPsaRepo;
    }

    public Object listar(long propriedade, int page, int size) {
        propriedadeRepo.obterMapa(propriedade);
        return Paginas.saida(
                diagnosticoRepo.findAll(
                        (r, q, b) -> b.equal(r.get("propriedadeId"), propriedade),
                        Paginas.pedido(page, size)),
                Diagnostico::dados);
    }

    public Object buscar(long id) {
        return Dados.saida(diagnosticoRepo.obterMapa(id));
    }

    public Object calculo(long id) {
        return Dados.saida(calculoPsaRepo.obterMapa(id));
    }
}
