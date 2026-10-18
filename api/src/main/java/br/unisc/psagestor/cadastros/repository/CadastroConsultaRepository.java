package br.unisc.psagestor.cadastros.repository;

import br.unisc.psagestor.cadastros.model.FonteHidrica;
import br.unisc.psagestor.cadastros.model.Localidade;
import br.unisc.psagestor.cadastros.model.Produtor;
import br.unisc.psagestor.cadastros.model.Propriedade;
import br.unisc.psagestor.cadastros.model.SubBacia;
import br.unisc.psagestor.cadastros.model.Visita;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.repository.Paginas;
import br.unisc.psagestor.configuracao.model.Projeto;
import br.unisc.psagestor.configuracao.repository.ProjetoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CadastroConsultaRepository {
    private final VisitaRepository visitaRepo;
    private final PropriedadeRepository propriedadeRepo;
    private final SubBaciaRepository subBaciaRepo;
    private final ProjetoRepository projetoRepo;
    private final LocalidadeRepository localidadeRepo;
    private final FonteHidricaRepository fonteHidricaRepo;
    private final ProdutorRepository produtorRepo;

    public CadastroConsultaRepository(
            VisitaRepository visitaRepo,
            PropriedadeRepository propriedadeRepo,
            SubBaciaRepository subBaciaRepo,
            ProjetoRepository projetoRepo,
            LocalidadeRepository localidadeRepo,
            FonteHidricaRepository fonteHidricaRepo,
            ProdutorRepository produtorRepo) {
        this.visitaRepo = visitaRepo;
        this.propriedadeRepo = propriedadeRepo;
        this.subBaciaRepo = subBaciaRepo;
        this.projetoRepo = projetoRepo;
        this.localidadeRepo = localidadeRepo;
        this.fonteHidricaRepo = fonteHidricaRepo;
        this.produtorRepo = produtorRepo;
    }

    public Object produtores(int page, int size) {
        return Paginas.saida(produtorRepo.findAll(Paginas.pedido(page, size)), Produtor::dados);
    }

    public Object produtor(long id) {
        return Dados.saida(produtorRepo.obterMapa(id));
    }

    public Object propriedades(Long produtor, int page, int size) {
        return produtor == null
                ? Paginas.saida(
                        propriedadeRepo.findAll(Paginas.pedido(page, size)), Propriedade::dados)
                : Paginas.saida(
                        propriedadeRepo.findAll(
                                (r, q, b) -> b.equal(r.get("produtorId"), produtor),
                                Paginas.pedido(page, size)),
                        Propriedade::dados);
    }

    public Object fontes(long id, int page, int size) {
        return Paginas.saida(
                fonteHidricaRepo.findAll(
                        (r, q, b) -> b.equal(r.get("propriedadeId"), id),
                        Paginas.pedido(page, size)),
                FonteHidrica::dados);
    }

    public Object visitas(long id, int page, int size) {
        return Paginas.saida(
                visitaRepo.findAll(
                        (r, q, b) -> b.equal(r.get("propriedadeId"), id),
                        Paginas.pedido(page, size)),
                Visita::dados);
    }

    public Object localidades(int page, int size) {
        return Paginas.saida(localidadeRepo.findAll(Paginas.pedido(page, size)), Localidade::dados);
    }

    public Object subBacias(int page, int size) {
        return Paginas.saida(subBaciaRepo.findAll(Paginas.pedido(page, size)), SubBacia::dados);
    }

    public Object projetos(int page, int size) {
        return Paginas.saida(
                projetoRepo.findAll(
                        (r, q, b) -> b.equal(r.get("ativo"), true), Paginas.pedido(page, size)),
                Projeto::dados);
    }
}
