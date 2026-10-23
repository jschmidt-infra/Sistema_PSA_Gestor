import { Component, computed, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { Pagina, Perfil, Usuario } from '../../core/api/modelos';
import { NOMES_PERFIS, PERFIS } from '../../core/auth/perfis';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { Paginacao } from '../../shared/ui/paginacao';
import { validarSenha } from './senha';
import { BloqueioLogin, UsuariosService } from './usuarios.service';

type Modo = { tipo: 'lista' } | { tipo: 'novo' } | { tipo: 'editar'; usuario: Usuario };

/**
 * Administração de contas: criação, edição, redefinição de senha e liberação
 * antecipada do bloqueio de login. Não existe cadastro público.
 */
@Component({
  selector: 'app-usuarios',
  imports: [ReactiveFormsModule, MensagemErro, Paginacao],
  templateUrl: './usuarios.html',
})
export class Usuarios {
  private readonly service = inject(UsuariosService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly nomesPerfis = NOMES_PERFIS;
  protected readonly perfis = PERFIS;
  protected readonly tamanho = 20;

  protected readonly pagina = signal<Pagina<Usuario> | null>(null);
  protected readonly carregando = signal(false);
  protected readonly falhaLista = signal<FalhaExibivel | null>(null);

  protected readonly modo = signal<Modo>({ tipo: 'lista' });
  protected readonly salvando = signal(false);
  protected readonly falhaForm = signal<FalhaExibivel | null>(null);
  protected readonly mensagem = signal<string | null>(null);

  protected readonly bloqueio = signal<BloqueioLogin | null>(null);
  protected readonly novaSenha = this.fb.control('', [Validators.required, validarSenha]);

  protected readonly form = this.fb.group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
    perfil: this.fb.control<Perfil>('TECNICO', Validators.required),
    ativo: [true],
    produtorId: this.fb.control<number | null>(null),
    senha: ['', [validarSenha]],
  });

  protected readonly ehProdutor = signal(false);
  protected readonly editando = computed(() => {
    const m = this.modo();
    return m.tipo === 'editar' ? m.usuario : null;
  });

  constructor() {
    this.form.controls.perfil.valueChanges.subscribe((p) => this.ehProdutor.set(p === 'PRODUTOR'));
    void this.carregar(0);
  }

  protected async carregar(page: number): Promise<void> {
    this.carregando.set(true);
    this.falhaLista.set(null);
    try {
      this.pagina.set(await firstValueFrom(this.service.listar(page, this.tamanho)));
    } catch (erro) {
      this.falhaLista.set(interpretarErro(erro));
    } finally {
      this.carregando.set(false);
    }
  }

  protected novo(): void {
    this.form.reset({
      nome: '',
      email: '',
      perfil: 'TECNICO',
      ativo: true,
      produtorId: null,
      senha: '',
    });
    this.form.controls.senha.setValidators([Validators.required, validarSenha]);
    this.form.controls.senha.updateValueAndValidity();
    this.prepararForm({ tipo: 'novo' });
  }

  protected editar(usuario: Usuario): void {
    this.form.reset({ ...usuario, senha: '' });
    this.form.controls.senha.setValidators([]);
    this.form.controls.senha.updateValueAndValidity();
    this.novaSenha.reset('');
    this.prepararForm({ tipo: 'editar', usuario });
    void this.consultarBloqueio(usuario.id);
  }

  protected cancelar(): void {
    this.modo.set({ tipo: 'lista' });
  }

  protected async salvar(): Promise<void> {
    if (this.form.invalid || (this.ehProdutor() && !this.form.controls.produtorId.value)) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const entrada = {
      nome: v.nome.trim(),
      email: v.email.trim(),
      perfil: v.perfil,
      ativo: v.ativo,
      produtorId: v.perfil === 'PRODUTOR' ? v.produtorId : null,
    };
    this.salvando.set(true);
    this.falhaForm.set(null);
    try {
      const atual = this.editando();
      if (atual) {
        await firstValueFrom(this.service.editar(atual.id, entrada));
        this.mensagem.set(`Conta de ${entrada.nome} atualizada.`);
      } else {
        await firstValueFrom(this.service.criar({ ...entrada, senha: v.senha }));
        this.mensagem.set(`Conta de ${entrada.nome} criada.`);
      }
      this.modo.set({ tipo: 'lista' });
      await this.carregar(this.pagina()?.page ?? 0);
    } catch (erro) {
      this.falhaForm.set(interpretarErro(erro));
    } finally {
      this.form.controls.senha.reset('');
      this.salvando.set(false);
    }
  }

  protected async redefinirSenha(): Promise<void> {
    const usuario = this.editando();
    if (!usuario || this.novaSenha.invalid) {
      this.novaSenha.markAsTouched();
      return;
    }
    this.salvando.set(true);
    this.falhaForm.set(null);
    try {
      await firstValueFrom(this.service.redefinirSenha(usuario.id, this.novaSenha.value));
      this.mensagem.set(
        `Senha de ${usuario.nome} redefinida. As sessões abertas dessa conta foram encerradas.`,
      );
      this.modo.set({ tipo: 'lista' });
    } catch (erro) {
      this.falhaForm.set(interpretarErro(erro));
    } finally {
      this.novaSenha.reset('');
      this.salvando.set(false);
    }
  }

  protected async desbloquear(): Promise<void> {
    const usuario = this.editando();
    if (!usuario) return;
    this.salvando.set(true);
    this.falhaForm.set(null);
    try {
      await firstValueFrom(this.service.desbloquear(usuario.id));
      await this.consultarBloqueio(usuario.id);
    } catch (erro) {
      this.falhaForm.set(interpretarErro(erro));
    } finally {
      this.salvando.set(false);
    }
  }

  protected minutos(segundos: number): number {
    return Math.max(1, Math.ceil(segundos / 60));
  }

  private prepararForm(modo: Modo): void {
    this.falhaForm.set(null);
    this.mensagem.set(null);
    this.bloqueio.set(null);
    this.ehProdutor.set(this.form.controls.perfil.value === 'PRODUTOR');
    this.modo.set(modo);
  }

  private async consultarBloqueio(id: number): Promise<void> {
    try {
      this.bloqueio.set(await firstValueFrom(this.service.consultarBloqueio(id)));
    } catch {
      this.bloqueio.set(null);
    }
  }
}
