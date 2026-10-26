import { Component, effect, inject, input, signal, untracked } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { formatarData, hoje } from '../../shared/formatos';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { Visita } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

/** RF17 / US08 — visitas técnicas de acompanhamento da propriedade. */
@Component({
  selector: 'app-visitas',
  imports: [ReactiveFormsModule, MensagemErro],
  template: `
    <div class="cartao">
      <div class="cartao-cabecalho">
        <h2>Visitas técnicas</h2>
        @if (podeEditar() && emEdicao() === null) {
          <button type="button" class="botao secundario pequeno" (click)="nova()">
            Registrar visita
          </button>
        }
      </div>
      <app-mensagem-erro [falha]="falha()" />

      @if (emEdicao() !== null) {
        <form [formGroup]="form" (ngSubmit)="salvar()" class="painel-form" novalidate>
          <div class="formulario">
            <div class="campo">
              <label for="dataVisita" class="obrigatorio">Data da visita</label>
              <input id="dataVisita" type="date" formControlName="dataVisita" [max]="hoje" />
            </div>
            <div class="campo largo">
              <label for="observacoesVisita" class="obrigatorio">Observações</label>
              <textarea
                id="observacoesVisita"
                formControlName="observacoes"
                maxlength="5000"
              ></textarea>
              @if (form.controls.observacoes.touched && form.controls.observacoes.invalid) {
                <span class="erro-campo">Descreva o que foi observado.</span>
              }
            </div>
          </div>
          <div class="acoes">
            <button type="submit" class="botao" [disabled]="salvando()">Salvar visita</button>
            <button type="button" class="botao secundario" (click)="emEdicao.set(null)">
              Cancelar
            </button>
          </div>
        </form>
      }

      @if (visitas().length === 0) {
        <p class="vazio">Nenhuma visita registrada.</p>
      } @else {
        <ul class="linha-tempo">
          @for (v of visitas(); track v.id) {
            <li>
              <div class="linha-tempo-cabecalho">
                <strong>{{ formatarData(v.dataVisita) }}</strong>
                @if (podeEditar()) {
                  <button type="button" class="botao secundario pequeno" (click)="editar(v)">
                    Editar
                  </button>
                }
              </div>
              <p>{{ v.observacoes }}</p>
            </li>
          }
        </ul>
      }
    </div>
  `,
  styles: `
    .linha-tempo {
      list-style: none;
      margin: 0;
      padding: 0;
      li {
        border-left: 3px solid var(--primaria-clara);
        padding: 0.25rem 0 0.75rem 1rem;
      }
      p {
        margin: 0.25rem 0 0;
        white-space: pre-line;
      }
    }
    .linha-tempo-cabecalho {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 0.5rem;
    }
  `,
})
export class Visitas {
  readonly propriedadeId = input.required<number>();
  readonly podeEditar = input(false);

  private readonly service = inject(CadastrosService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly formatarData = formatarData;
  protected readonly hoje = hoje();
  protected readonly visitas = signal<Visita[]>([]);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly salvando = signal(false);
  protected readonly emEdicao = signal<number | null>(null);

  protected readonly form = this.fb.group({
    dataVisita: [hoje(), Validators.required],
    observacoes: ['', [Validators.required, Validators.maxLength(5000)]],
  });

  constructor() {
    effect(() => {
      const id = this.propriedadeId();
      untracked(() => void this.carregar(id));
    });
  }

  private async carregar(id: number): Promise<void> {
    try {
      const p = await firstValueFrom(this.service.listarVisitas(id, 0, 100));
      // Mais recentes primeiro.
      this.visitas.set([...p.items].sort((a, b) => b.dataVisita.localeCompare(a.dataVisita)));
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }

  protected nova(): void {
    this.form.reset({ dataVisita: hoje(), observacoes: '' });
    this.emEdicao.set(0);
  }

  protected editar(v: Visita): void {
    this.form.reset({ dataVisita: v.dataVisita, observacoes: v.observacoes });
    this.emEdicao.set(v.id);
  }

  protected async salvar(): Promise<void> {
    const id = this.emEdicao();
    if (id === null) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const entrada = { dataVisita: v.dataVisita, observacoes: v.observacoes.trim() };
    this.salvando.set(true);
    this.falha.set(null);
    try {
      if (id === 0) await firstValueFrom(this.service.criarVisita(this.propriedadeId(), entrada));
      else await firstValueFrom(this.service.editarVisita(id, entrada));
      this.emEdicao.set(null);
      await this.carregar(this.propriedadeId());
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.salvando.set(false);
    }
  }
}
