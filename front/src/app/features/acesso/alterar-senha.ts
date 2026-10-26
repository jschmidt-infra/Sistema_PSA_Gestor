import { Component, inject, signal } from '@angular/core';
import {
  AbstractControl,
  NonNullableFormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router } from '@angular/router';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { AuthService } from '../../core/auth/auth.service';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { validarSenha } from './senha';

function confirmacaoIgual(grupo: AbstractControl): ValidationErrors | null {
  const nova = grupo.get('novaSenha')?.value;
  const confirmacao = grupo.get('confirmacao')?.value;
  return nova && confirmacao && nova !== confirmacao ? { confirmacao: true } : null;
}

/** Troca da própria senha. A API encerra a sessão em seguida. */
@Component({
  selector: 'app-alterar-senha',
  imports: [ReactiveFormsModule, MensagemErro],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <h1>Alterar senha</h1>
        <p>Após a alteração, será preciso entrar novamente.</p>
      </div>
    </div>

    <div class="cartao limite">
      <app-mensagem-erro [falha]="falha()" />
      <form [formGroup]="form" (ngSubmit)="salvar()" novalidate>
        <div class="campo">
          <label for="senhaAtual" class="obrigatorio">Senha atual</label>
          <input
            id="senhaAtual"
            type="password"
            formControlName="senhaAtual"
            autocomplete="current-password"
          />
        </div>
        <div class="campo">
          <label for="novaSenha" class="obrigatorio">Nova senha</label>
          <input
            id="novaSenha"
            type="password"
            formControlName="novaSenha"
            autocomplete="new-password"
          />
          <span class="ajuda">Mínimo de 12 caracteres.</span>
          @if (form.controls.novaSenha.touched && form.controls.novaSenha.errors; as e) {
            <span class="erro-campo">{{ e['senha'] ?? 'Informe a nova senha.' }}</span>
          }
        </div>
        <div class="campo">
          <label for="confirmacao" class="obrigatorio">Confirme a nova senha</label>
          <input
            id="confirmacao"
            type="password"
            formControlName="confirmacao"
            autocomplete="new-password"
          />
          @if (form.controls.confirmacao.touched && form.hasError('confirmacao')) {
            <span class="erro-campo">As senhas não coincidem.</span>
          }
        </div>
        <div class="acoes">
          <button type="submit" class="botao" [disabled]="enviando()">Alterar senha</button>
        </div>
      </form>
    </div>
  `,
  styles: `
    .limite {
      max-width: 28rem;
    }
    form {
      display: grid;
      gap: 1rem;
    }
  `,
})
export class AlterarSenha {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly form = inject(NonNullableFormBuilder).group(
    {
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, validarSenha]],
      confirmacao: ['', Validators.required],
    },
    { validators: confirmacaoIgual },
  );
  protected readonly enviando = signal(false);
  protected readonly falha = signal<FalhaExibivel | null>(null);

  protected async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.falha.set(null);
    const { senhaAtual, novaSenha } = this.form.getRawValue();
    try {
      await this.auth.alterarSenha(senhaAtual, novaSenha);
      await this.router.navigate(['/login'], { queryParams: { senhaAlterada: 1 } });
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.form.reset();
      this.enviando.set(false);
    }
  }
}
