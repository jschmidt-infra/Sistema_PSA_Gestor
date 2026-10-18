package br.unisc.psagestor.acesso.repository;

import br.unisc.psagestor.acesso.api.UsuariosEntradas.Entrada;
import br.unisc.psagestor.acesso.service.AuditoriaService;
import br.unisc.psagestor.cadastros.repository.ProdutorRepository;
import br.unisc.psagestor.comum.api.Dados;
import br.unisc.psagestor.comum.repository.Paginas;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Repository;

@Repository
public class AdministracaoUsuarioRepository {
    private final UsuarioRepository usuarioRepo;
    private final ProdutorRepository produtorRepo;
    private final AuditoriaService auditoria;
    private static final String COLUNAS = "id,nome,email,perfil,ativo,produtor_id";

    public AdministracaoUsuarioRepository(
            UsuarioRepository usuarioRepo,
            ProdutorRepository produtorRepo,
            AuditoriaService auditoria) {
        this.usuarioRepo = usuarioRepo;
        this.produtorRepo = produtorRepo;
        this.auditoria = auditoria;
    }

    public Object listar(int page, int size) {
        return Paginas.saida(
                usuarioRepo.findAll(Paginas.pedido(page, size)), u -> publico(u.dados()));
    }

    public Map<String, Object> consultar(long id) {
        return Dados.saida(publico(usuarioRepo.obterMapa(id)));
    }

    public void exigirProdutor(long id) {
        produtorRepo.obterMapa(id);
    }

    public void bloquearAdministradores() {
        usuarioRepo.bloquearAdministradores();
    }

    public Map<String, Object> bloquear(long id) {
        return usuarioRepo.bloquearMapa(id);
    }

    public long outrosAdministradoresAtivos(long id) {
        return usuarioRepo.outrosAdministradoresAtivos(id);
    }

    public long inserir(Entrada e, String hash, long autor) {
        return usuarioRepo.criar(
                e.nome(), e.email(), hash, e.perfil().name(), e.ativo(), e.produtorId(), autor);
    }

    public void editar(long id, Entrada e, long autor) {
        usuarioRepo.atualizarCadastro(e.nome(), e.email(), e.perfil().name(), e.ativo(), autor, id);
    }

    public void senha(long id, String hash, long autor) {
        usuarioRepo.redefinirSenha(hash, autor, id);
    }

    public void auditar(String operacao, long id) {
        auditoria.registrar(operacao, "usuario", id);
    }

    private Map<String, Object> publico(Map<String, Object> dados) {
        dados.keySet().retainAll(Set.of("id", "nome", "email", "perfil", "ativo", "produtor_id"));
        return dados;
    }
}
