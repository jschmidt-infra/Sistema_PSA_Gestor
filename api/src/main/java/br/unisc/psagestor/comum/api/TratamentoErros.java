package br.unisc.psagestor.comum.api;

import br.unisc.psagestor.acesso.service.LimiteLogin.Bloqueado;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class TratamentoErros {
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ErroApi> rota(Exception ex, HttpServletRequest r) {
        return ResponseEntity.status(404)
                .body(ErroApi.de("NAO_ENCONTRADO", "Rota não encontrada.", id(r)));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ErroApi> metodo(Exception ex, HttpServletRequest r) {
        return ResponseEntity.status(405)
                .body(
                        ErroApi.de(
                                "METODO_NAO_PERMITIDO",
                                "Método não permitido para esta rota.",
                                id(r)));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ErroApi> parte(Exception ex, HttpServletRequest r) {
        return ResponseEntity.badRequest()
                .body(
                        ErroApi.de(
                                "ARQUIVO_OBRIGATORIO",
                                "Informe a parte arquivo da requisição multipart.",
                                id(r)));
    }

    @ExceptionHandler(FalhaApi.class)
    ResponseEntity<ErroApi> falha(FalhaApi ex, HttpServletRequest r) {
        return ResponseEntity.status(ex.status())
                .body(ErroApi.de(ex.codigo(), ex.getMessage(), id(r)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErroApi> integridade(Exception ex, HttpServletRequest r) {
        return ResponseEntity.status(409)
                .body(ErroApi.de("CONFLITO", "Registro duplicado ou vínculo incompatível.", id(r)));
    }

    @ExceptionHandler({
        MethodArgumentTypeMismatchException.class,
        MissingServletRequestParameterException.class
    })
    ResponseEntity<ErroApi> parametro(Exception ex, HttpServletRequest r) {
        return ResponseEntity.badRequest()
                .body(ErroApi.de("PARAMETRO_INVALIDO", "Parâmetro inválido ou ausente.", id(r)));
    }

    private static final Logger LOG = LoggerFactory.getLogger(TratamentoErros.class);

    private String id(HttpServletRequest r) {
        return (String) r.getAttribute(RequisicaoIdFilter.ATRIBUTO);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErroApi> validacao(MethodArgumentNotValidException ex, HttpServletRequest r) {
        var campos =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(e -> new ErroApi.ErroCampo(e.getField(), e.getDefaultMessage()))
                        .toList();
        return ResponseEntity.unprocessableContent()
                .body(
                        new ErroApi(
                                "DADOS_INVALIDOS", "Revise os campos informados.", campos, id(r)));
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ErroApi> regra(RegraNegocioException ex, HttpServletRequest r) {
        return ResponseEntity.unprocessableContent()
                .body(ErroApi.de(ex.getCodigo(), ex.getMessage(), id(r)));
    }

    @ExceptionHandler(Bloqueado.class)
    ResponseEntity<ErroApi> loginBloqueado(Bloqueado ex, HttpServletRequest r) {
        return ResponseEntity.status(429)
                .header("Retry-After", Long.toString(ex.segundos()))
                .body(ErroApi.de("LOGIN_TEMPORARIAMENTE_BLOQUEADO", ex.getMessage(), id(r)));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErroApi> autenticacao(AuthenticationException ex, HttpServletRequest r) {
        return ResponseEntity.status(401)
                .body(
                        ErroApi.de(
                                "NAO_AUTENTICADO",
                                "Credenciais não aceitas ou sessão inválida.",
                                id(r)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErroApi> proibido(AccessDeniedException ex, HttpServletRequest r) {
        return ResponseEntity.status(403)
                .body(ErroApi.de("ACESSO_NEGADO", "Operação não permitida.", id(r)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErroApi> json(HttpMessageNotReadableException ex, HttpServletRequest r) {
        return ResponseEntity.badRequest()
                .body(ErroApi.de("JSON_INVALIDO", "Corpo da requisição inválido.", id(r)));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ErroApi> tamanho(MaxUploadSizeExceededException ex, HttpServletRequest r) {
        return ResponseEntity.status(413)
                .body(
                        ErroApi.de(
                                "ARQUIVO_MUITO_GRANDE",
                                "O arquivo excede o limite permitido.",
                                id(r)));
    }

    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<ErroApi> banco(DataAccessException ex, HttpServletRequest r) {
        LOG.warn(
                "Falha de persistência requisicaoId={} tipo={}",
                id(r),
                ex.getClass().getSimpleName());
        return ResponseEntity.status(503)
                .body(
                        ErroApi.de(
                                "SERVICO_INDISPONIVEL",
                                "Persistência temporariamente indisponível. Em pagamentos, consulte a chave de operação e reutilize a mesma chave ao tentar novamente.",
                                id(r)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErroApi> inesperado(Exception ex, HttpServletRequest r) {
        LOG.error("Falha inesperada requisicaoId={} tipo={}", id(r), ex.getClass().getName());
        return ResponseEntity.internalServerError()
                .body(ErroApi.de("ERRO_INTERNO", "Não foi possível concluir a operação.", id(r)));
    }
}
