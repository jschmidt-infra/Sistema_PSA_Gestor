import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { AuthService } from '../../core/auth/auth.service';
import { formatarArea, somenteDigitos, vazioParaNulo } from '../../shared/formatos';
import { idOpcional } from '../../shared/rotas';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { Propriedade } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

/** CPF/CNPJ com ou sem máscara; a máscara é removida antes do envio. */
const DOCUMENTO = /^(\d{11}|\d{14}|\d{3}\.\d{3}\.\d{3}-\d{2}|\d{2}\.\d{3}\.\d{3}\/\d{4}-\d{2})$/;

/**
 * UC01 — cadastro e edição do produtor, com as propriedades vinculadas.
 * Documento e endereço podem faltar no início, mas são exigidos para o contrato.
 */
@Component({
  selector: 'app-produtor-form',
  imports: [ReactiveFormsModule, RouterLink, MensagemErro],
  templateUrl: './produtor-form.html',
})
export class ProdutorForm {
  /** Vem da rota (withComponentInputBinding); ausente em /produtores/novo. */
  readonly id = input(undefined, { transform: idOpcional });

  private readonly service = inject(CadastrosService);
  private readonly router = inject(Router);

  protected readonly formatarArea = formatarArea;
  protected readonly ehNovo = computed(() => this.id() === undefined);
  protected readonly podeCriarPropriedade = inject(AuthService).temPerfil('TECNICO');

  protected readonly form = inject(NonNullableFormBuilder).group({
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    documento: ['', Validators.pattern(DOCUMENTO)],
    endereco: ['', Validators.maxLength(255)],
    telefone: ['', Validators.maxLength(30)],
    email: ['', [Validators.email, Validators.maxLength(254)]],
  });

  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly mensagem = signal<string | null>(null);
  protected readonly propriedades = signal<Propriedade[]>([]);
  protected readonly semContato = signal(false);

  constructor() {
    const estado = this.router.currentNavigation()?.extras.state;
    if (estado?.['mensagem']) this.mensagem.set(String(estado['mensagem']));

    // A mesma instância é reaproveitada ao navegar entre produtores.
    effect(() => {
      const id = this.id();
      if (id !== undefined) untracked(() => void this.carregar(id));
    });
  }

  private async carregar(id: number): Promise<void> {
    this.carregando.set(true);
    this.falha.set(null);
    try {
      const [produtor, props] = await Promise.all([
        firstValueFrom(this.service.consultarProdutor(id)),
        firstValueFrom(this.service.listarPropriedades(0, 100, id)),
      ]);
      this.form.reset({
        nome: produtor.nome,
        documento: produtor.documento ?? '',
        endereco: produtor.endereco ?? '',
        telefone: produtor.telefone ?? '',
        email: produtor.email ?? '',
      });
      this.propriedades.set(props.items);
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.carregando.set(false);
    }
  }

  protected async salvar(): Promise<void> {
    const v = this.form.getRawValue();
    // Regra da API: telefone ou e-mail é obrigatório.
    this.semContato.set(!vazioParaNulo(v.telefone) && !vazioParaNulo(v.email));
    if (this.form.invalid || this.semContato()) {
      this.form.markAllAsTouched();
      return;
    }
    const entrada = {
      nome: v.nome.trim(),
      documento: somenteDigitos(v.documento),
      endereco: vazioParaNulo(v.endereco),
      telefone: vazioParaNulo(v.telefone),
      email: vazioParaNulo(v.email),
    };
    this.salvando.set(true);
    this.falha.set(null);
    this.mensagem.set(null);
    try {
      const id = this.id();
      if (id === undefined) {
        const criado = await firstValueFrom(this.service.criarProdutor(entrada));
        await this.router.navigate(['/produtores', criado.id], {
          state: { mensagem: 'Produtor cadastrado.' },
        });
      } else {
        await firstValueFrom(this.service.editarProdutor(id, entrada));
        this.mensagem.set('Dados do produtor atualizados.');
        this.form.markAsPristine();
      }
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.salvando.set(false);
    }
  }
}
