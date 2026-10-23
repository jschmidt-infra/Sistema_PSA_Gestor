import { HttpErrorResponse, HttpEvent, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap, tap } from 'rxjs';
import { SessaoExpirada } from '../auth/sessao-expirada';
import { CsrfService } from './csrf.service';

const METODOS_DE_GRAVACAO = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

/** Rotas cujo 401 faz parte do fluxo de autenticação e não indica sessão expirada. */
const ROTAS_DE_AUTENTICACAO = ['/api/v1/auth/login', '/api/v1/auth/me', '/api/v1/auth/csrf'];

/**
 * Acrescenta o CSRF às gravações da API e reage a sessão expirada.
 *
 * Gravações nunca são repetidas automaticamente: um pagamento reenviado precisa
 * preservar a mesma chaveOperacao, decisão que cabe à tela.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  // O token só é enviado a caminhos relativos da própria API.
  if (!req.url.startsWith('/api/')) {
    return next(req);
  }

  const csrf = inject(CsrfService);
  const sessao = inject(SessaoExpirada);

  const tratarErro = tap<HttpEvent<unknown>>({
    error: (erro: unknown) => {
      if (!(erro instanceof HttpErrorResponse)) return;
      if (erro.status === 403 && METODOS_DE_GRAVACAO.has(req.method)) {
        // 403 pode ser perfil sem permissão ou token CSRF antigo; a próxima tentativa busca outro.
        csrf.descartar();
      }
      if (erro.status === 401 && !ROTAS_DE_AUTENTICACAO.some((r) => req.url.startsWith(r))) {
        csrf.descartar();
        sessao.notificar();
      }
    },
  });

  if (!METODOS_DE_GRAVACAO.has(req.method)) {
    return next(req).pipe(tratarErro);
  }

  return from(csrf.obter()).pipe(
    switchMap((token) => next(req.clone({ setHeaders: { [token.headerName]: token.token } }))),
    tratarErro,
  );
};
