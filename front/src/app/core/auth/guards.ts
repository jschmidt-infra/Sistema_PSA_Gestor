import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Perfil } from '../api/modelos';
import { AuthService } from './auth.service';

/**
 * Os guards apenas organizam a navegação. A autorização definitiva de cada
 * operação continua no backend (@PreAuthorize e verificação de titularidade).
 */

export const autenticadoGuard: CanActivateFn = async (_rota, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try {
    const usuario = await auth.restaurar();
    return usuario
      ? true
      : router.createUrlTree(['/login'], { queryParams: { retorno: estado.url } });
  } catch {
    return router.createUrlTree(['/indisponivel']);
  }
};

export const anonimoGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  try {
    return (await auth.restaurar()) ? router.createUrlTree(['/']) : true;
  } catch {
    // Sem resposta da API, a tela de login ainda pode ser exibida.
    return true;
  }
};

export function perfilGuard(...perfis: Perfil[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.temPerfil(...perfis) ? true : inject(Router).createUrlTree(['/acesso-negado']);
  };
}
