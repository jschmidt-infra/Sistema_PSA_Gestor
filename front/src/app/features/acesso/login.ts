import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { AuthService } from '../../core/auth/auth.service';
import { MensagemErro } from '../../shared/ui/mensagem-erro';

/** UC09 — Autenticar usuário. Não há cadastro público: contas são criadas pelo administrador. */
@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, MensagemErro],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly rota = inject(ActivatedRoute);

  protected readonly form = inject(NonNullableFormBuilder).group({
    email: ['', [Validators.required, Validators.email]],
    senha: ['', Validators.required],
  });
  protected readonly enviando = signal(false);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly expirada = this.rota.snapshot.queryParamMap.has('expirada');
  protected readonly senhaAlterada = this.rota.snapshot.queryParamMap.has('senhaAlterada');

  protected async entrar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.falha.set(null);
    const { email, senha } = this.form.getRawValue();
    try {
      await this.auth.entrar(email.trim(), senha);
      const retorno = this.rota.snapshot.queryParamMap.get('retorno');
      // Só aceita retorno interno, evitando redirecionamento para outro site.
      await this.router.navigateByUrl(
        retorno?.startsWith('/') && !retorno.startsWith('//') ? retorno : '/',
      );
    } catch (erro) {
      this.falha.set(this.mensagemLogin(erro));
    } finally {
      this.form.controls.senha.reset();
      this.enviando.set(false);
    }
  }

  private mensagemLogin(erro: unknown): FalhaExibivel {
    const falha = interpretarErro(erro);
    if (falha.status === 401) {
      return { ...falha, mensagem: 'E-mail ou senha não aceitos, ou conta inativa.' };
    }
    if (falha.status === 429 && erro instanceof HttpErrorResponse) {
      const segundos = Number(erro.headers.get('Retry-After'));
      const minutos = Number.isFinite(segundos) && segundos > 0 ? Math.ceil(segundos / 60) : 60;
      return {
        ...falha,
        mensagem: `Login temporariamente bloqueado após várias tentativas recusadas. Tente novamente em cerca de ${minutos} min ou peça liberação a um administrador.`,
      };
    }
    return falha;
  }
}
