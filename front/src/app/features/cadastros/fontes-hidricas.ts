import {
  Component,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked,
} from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { vazioParaNulo } from '../../shared/formatos';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import {
  FonteHidrica,
  FontesSituacao,
  NOMES_FONTES_SITUACAO,
  NOMES_PROTECAO,
  NOMES_TIPO_FONTE,
  Protecao,
  TipoFonte,
} from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

/**
 * UC02 — fontes hídricas da propriedade e seu estado de proteção.
 * A1: várias fontes; A2: ausência registrada explicitamente.
 */
@Component({
  selector: 'app-fontes-hidricas',
  imports: [ReactiveFormsModule, MensagemErro],
  templateUrl: './fontes-hidricas.html',
})
export class FontesHidricas {
  readonly propriedadeId = input.required<number>();
  readonly situacao = input.required<FontesSituacao>();
  readonly podeEditar = input(false);
  /** Avisa a tela da propriedade de que a situação hídrica pode ter mudado. */
  readonly alterado = output<void>();

  private readonly service = inject(CadastrosService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly nomesTipo = NOMES_TIPO_FONTE;
  protected readonly nomesProtecao = NOMES_PROTECAO;
  protected readonly nomesSituacao = NOMES_FONTES_SITUACAO;
  protected readonly tipos = Object.keys(NOMES_TIPO_FONTE) as TipoFonte[];
  protected readonly protecoes = Object.keys(NOMES_PROTECAO) as Protecao[];

  protected readonly fontes = signal<FonteHidrica[]>([]);
  protected readonly ativas = computed(() => this.fontes().filter((f) => f.ativo).length);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly salvando = signal(false);
  /** null: formulário fechado; 0: nova fonte; > 0: edição. */
  protected readonly emEdicao = signal<number | null>(null);

  protected readonly form = this.fb.group({
    tipo: this.fb.control<TipoFonte>('NASCENTE', Validators.required),
    protecao: this.fb.control<Protecao>('NAO_AVALIADA', Validators.required),
    ativo: [true],
    observacoes: ['', Validators.maxLength(5000)],
  });

  constructor() {
    effect(() => {
      const id = this.propriedadeId();
      untracked(() => void this.carregar(id));
    });
  }

  private async carregar(id: number): Promise<void> {
    try {
      const p = await firstValueFrom(this.service.listarFontes(id, 0, 100));
      this.fontes.set(p.items);
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }

  protected nova(): void {
    this.form.reset({ tipo: 'NASCENTE', protecao: 'NAO_AVALIADA', ativo: true, observacoes: '' });
    this.falha.set(null);
    this.emEdicao.set(0);
  }

  protected editar(f: FonteHidrica): void {
    this.form.reset({
      tipo: f.tipo,
      protecao: f.protecao,
      ativo: f.ativo,
      observacoes: f.observacoes ?? '',
    });
    this.falha.set(null);
    this.emEdicao.set(f.id);
  }

  protected cancelar(): void {
    this.emEdicao.set(null);
  }

  protected async salvar(): Promise<void> {
    const id = this.emEdicao();
    if (id === null || this.form.invalid) return;
    const v = this.form.getRawValue();
    const entrada = { ...v, observacoes: vazioParaNulo(v.observacoes) };
    await this.executar(async () => {
      if (id === 0) await firstValueFrom(this.service.criarFonte(this.propriedadeId(), entrada));
      else await firstValueFrom(this.service.editarFonte(id, entrada));
      this.emEdicao.set(null);
    });
  }

  protected async registrarAusencia(): Promise<void> {
    await this.executar(() =>
      firstValueFrom(this.service.registrarLevantamento(this.propriedadeId(), 'AUSENTES')),
    );
  }

  private async executar(acao: () => Promise<unknown>): Promise<void> {
    this.salvando.set(true);
    this.falha.set(null);
    try {
      await acao();
      await this.carregar(this.propriedadeId());
      this.alterado.emit();
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.salvando.set(false);
    }
  }
}
