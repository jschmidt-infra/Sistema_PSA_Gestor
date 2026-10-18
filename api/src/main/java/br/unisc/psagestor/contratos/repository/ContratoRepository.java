package br.unisc.psagestor.contratos.repository;

import br.unisc.psagestor.comum.repository.EntidadeRepository;
import br.unisc.psagestor.contratos.model.Contrato;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

public interface ContratoRepository extends EntidadeRepository<Contrato, Long> {
    default Map<String, Object> obterMapa(Long id) {
        return obter(id).dados();
    }

    default Map<String, Object> bloquearMapa(Long id) {
        return bloquear(id).dados();
    }

    @Transactional
    default long criar(
            String numero,
            long produtorId,
            long propriedadeId,
            long projetoId,
            long calculoId,
            Long croquiId,
            LocalDate inicio,
            LocalDate fim,
            long criadoPor) {
        var entity = new Contrato();
        entity.setNumero(numero);
        entity.setProdutorId(produtorId);
        entity.setPropriedadeId(propriedadeId);
        entity.setProjetoId(projetoId);
        entity.setCalculoId(calculoId);
        entity.setCroquiId(croquiId);
        entity.setInicio(inicio);
        entity.setFim(fim);
        entity.setStatus("RASCUNHO");
        entity.setCriadoEm(LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setCriadoPor(criadoPor);
        return salvarEAtualizar(entity).getId();
    }

    @Transactional
    default void registrarAssinatura(LocalDate assinaturaEm, Long id) {
        var entity = obter(id);
        entity.setAssinaturaEm(assinaturaEm);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void registrarDocumento(String documentoChave, String documentoSnapshot, Long id) {
        var entity = obter(id);
        entity.setDocumentoChave(documentoChave);
        entity.setDocumentoSnapshot(documentoSnapshot);
        entity.setModeloVersao("PRELIMINAR-1");
        entity.setAssinaturaEm(null);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void atualizarRascunho(
            long calculoId, Long croquiId, LocalDate inicio, LocalDate fim, Long id) {
        var entity = obter(id);
        entity.setCalculoId(calculoId);
        entity.setCroquiId(croquiId);
        entity.setInicio(inicio);
        entity.setFim(fim);
        entity.setDocumentoChave(null);
        entity.setDocumentoSnapshot(null);
        entity.setAssinaturaEm(null);
        salvarEAtualizar(entity);
    }

    @Transactional
    default void registrarAprovacao(Long aprovadoPor, Long id) {
        var entity = obter(id);
        entity.setStatus("APROVADO");
        entity.setAprovadoEm(
                LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
        entity.setAprovadoPor(aprovadoPor);
        salvarEAtualizar(entity);
    }

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COUNT(*) FROM contrato WHERE propriedade_id=?1 AND status='APROVADO'",
            nativeQuery = true)
    Long quantidadeAprovadosNaPropriedade(Long id);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT COUNT(*) FROM contrato WHERE croqui_id=?1 AND status='APROVADO'",
            nativeQuery = true)
    Long quantidadeAprovadosComCroqui(Long id);

    long countByDocumentoChave(String chave);
}
