import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

interface Csrf {
  token: string;
  headerName: string;
}

/**
 * Mantém em memória o token CSRF da sessão.
 *
 * O backend usa HttpSessionCsrfTokenRepository e entrega o token em JSON, por isso
 * o suporte XSRF padrão do Angular (baseado em cookie) não é utilizado.
 */
@Injectable({ providedIn: 'root' })
export class CsrfService {
  // HttpBackend evita que a busca do token passe pelo próprio interceptor.
  private readonly http = new HttpClient(inject(HttpBackend));
  private atual: Promise<Csrf> | null = null;

  obter(): Promise<Csrf> {
    if (!this.atual) {
      this.atual = firstValueFrom(this.http.get<Csrf>('/api/v1/auth/csrf'));
      this.atual.catch(() => (this.atual = null));
    }
    return this.atual;
  }

  /** Login, logout, troca de senha e 403 tornam o token anterior inválido. */
  descartar(): void {
    this.atual = null;
  }

  async renovar(): Promise<Csrf> {
    this.descartar();
    return this.obter();
  }
}
