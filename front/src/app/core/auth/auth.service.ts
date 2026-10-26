import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, effect, inject, signal, untracked } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { CsrfService } from '../api/csrf.service';
import { Perfil, Usuario } from '../api/modelos';
import { SessaoExpirada } from './sessao-expirada';

/**
 * Sessão do usuário no navegador.
 *
 * O cookie PSA_SESSION é HttpOnly e gerenciado pelo navegador. Aqui ficam só os
 * dados de apresentação, em memória, confirmados por /auth/me ao recarregar.
 * Nada é gravado em localStorage, e o perfil daqui não concede acesso: a API autoriza.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly csrf = inject(CsrfService);
  private readonly router = inject(Router);

  private readonly _usuario = signal<Usuario | null>(null);
  private restauracao: Promise<Usuario | null> | null = null;

  readonly usuario = this._usuario.asReadonly();
  readonly autenticado = computed(() => this._usuario() !== null);
  readonly perfil = computed(() => this._usuario()?.perfil ?? null);

  constructor() {
    const sessao = inject(SessaoExpirada);
    effect(() => {
      if (sessao.ocorrencias() === 0) return;
      untracked(() => this.encerrarLocalmente(true));
    });
  }

  temPerfil(...perfis: Perfil[]): boolean {
    const atual = this.perfil();
    return atual !== null && perfis.includes(atual);
  }

  /**
   * Restaura a sessão uma única vez por carregamento da aplicação.
   * 401 significa sem sessão; falha de rede ou 503 é propagada, sem simular logout.
   */
  restaurar(): Promise<Usuario | null> {
    if (!this.restauracao) {
      this.restauracao = firstValueFrom(this.http.get<Usuario>('/api/v1/auth/me'))
        .then((u) => {
          this._usuario.set(u);
          return u as Usuario | null;
        })
        .catch((erro: unknown) => {
          if (erro instanceof HttpErrorResponse && erro.status === 401) {
            this._usuario.set(null);
            return null;
          }
          this.restauracao = null;
          throw erro;
        });
    }
    return this.restauracao;
  }

  async entrar(email: string, senha: string): Promise<Usuario> {
    // O token de login é buscado na hora; o login troca a sessão e invalida o token.
    await this.csrf.renovar();
    const usuario = await firstValueFrom(
      this.http.post<Usuario>('/api/v1/auth/login', { email, senha }),
    );
    this.csrf.descartar();
    this._usuario.set(usuario);
    this.restauracao = Promise.resolve(usuario);
    return usuario;
  }

  async sair(): Promise<void> {
    try {
      await firstValueFrom(this.http.post<void>('/api/v1/auth/logout', null));
    } finally {
      this.encerrarLocalmente(false);
    }
  }

  /** A API encerra a sessão após a troca de senha; é preciso entrar novamente. */
  async alterarSenha(senhaAtual: string, novaSenha: string): Promise<void> {
    await firstValueFrom(this.http.put<void>('/api/v1/auth/senha', { senhaAtual, novaSenha }));
    this.csrf.descartar();
    this._usuario.set(null);
    this.restauracao = Promise.resolve(null);
  }

  private encerrarLocalmente(expirada: boolean): void {
    this.csrf.descartar();
    this._usuario.set(null);
    this.restauracao = Promise.resolve(null);
    const retorno = this.router.url;
    void this.router.navigate(['/login'], {
      queryParams: expirada && !retorno.startsWith('/login') ? { expirada: 1, retorno } : {},
    });
  }
}
