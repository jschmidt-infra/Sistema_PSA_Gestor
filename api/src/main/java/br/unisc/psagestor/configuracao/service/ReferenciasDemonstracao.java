package br.unisc.psagestor.configuracao.service;

import br.unisc.psagestor.cadastros.model.Localidade;
import br.unisc.psagestor.cadastros.model.SubBacia;
import br.unisc.psagestor.cadastros.repository.LocalidadeRepository;
import br.unisc.psagestor.cadastros.repository.SubBaciaRepository;
import br.unisc.psagestor.configuracao.model.Projeto;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demonstracao & !prod")
public class ReferenciasDemonstracao implements ApplicationRunner {
    private final LocalidadeRepository localidades;
    private final SubBaciaRepository bacias;
    private final ProjetoRepository projetos;

    public ReferenciasDemonstracao(
            LocalidadeRepository localidades,
            SubBaciaRepository bacias,
            ProjetoRepository projetos) {
        this.localidades = localidades;
        this.bacias = bacias;
        this.projetos = projetos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (localidades.count() == 0) {
            var e = new Localidade();
            e.setNome("Localidade de demonstração");
            e.setMunicipio("Município de demonstração");
            e.setUf("RS");
            localidades.save(e);
        }
        if (bacias.count() == 0) {
            var e = new SubBacia();
            e.setNome("Sub-bacia de demonstração");
            bacias.save(e);
        }
        if (projetos.count() == 0) {
            var e = new Projeto();
            e.setNome("Projeto de demonstração");
            e.setOrigemRecursos("Fundo Municipal - simulação");
            e.setAtivo(true);
            projetos.save(e);
        }
    }
}
