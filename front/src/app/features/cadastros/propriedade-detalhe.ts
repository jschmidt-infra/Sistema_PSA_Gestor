import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { AuthService } from '../../core/auth/auth.service';
import { formatarArea, formatarInstante } from '../../shared/formatos';
import { idObrigatorio } from '../../shared/rotas';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { NOMES_CAR, Produtor, Propriedade } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';
import { FontesHidricas } from './fontes-hidricas';
import { Visitas } from './visitas';

/** Visão da propriedade: dados cadastrais, fontes hídricas e visitas técnicas. */
@Component({
  selector: 'app-propriedade-detalhe',
  imports: [RouterLink, MensagemErro, FontesHidricas, Visitas],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <div class="migalhas"><a routerLink="/propriedades">Propriedades</a> ›</div>
        <h1>{{ propriedade()?.nome ?? 'Propriedade' }}</h1>
        @if (produtor(); as pr) {
          <p>
            Produtor: <a [routerLink]="['/produtores', pr.id]">{{ pr.nome }}</a>
          </p>
        }
      </div>
      @if (propriedade() && podeEditarCadastro) {
        <a [routerLink]="['/propriedades', id(), 'editar']" class="botao secundario"
          >Editar dados</a
        >
      }
    </div>

    @if (mensagem()) {
      <div class="aviso sucesso" role="status">{{ mensagem() }}</div>
    }
    <app-mensagem-erro [falha]="falha()" />

    @if (propriedade(); as p) {
      <div class="cartao">
        <h2>Dados da propriedade</h2>
        <dl class="detalhes">
          <div>
            <dt>Área total</dt>
            <dd>{{ formatarArea(p.areaTotal) }}</dd>
          </div>
          <div>
            <dt>Localidade</dt>
            <dd>{{ nomeLocalidade() }}</dd>
          </div>
          <div>
            <dt>Sub-bacia</dt>
            <dd>{{ nomeSubBacia() }}</dd>
          </div>
          <div>
            <dt>CAR</dt>
            <dd>{{ nomesCar[p.carSituacao] }}{{ p.carNumero ? ' — ' + p.carNumero : '' }}</dd>
          </div>
          <div>
            <dt>Cadastrada em</dt>
            <dd>{{ formatarInstante(p.criadoEm) }}</dd>
          </div>
          <div>
            <dt>Última atualização</dt>
            <dd>{{ formatarInstante(p.atualizadoEm) }}</dd>
          </div>
        </dl>
      </div>

      <app-fontes-hidricas
        [propriedadeId]="p.id"
        [situacao]="p.fontesSituacao"
        [podeEditar]="ehTecnico"
        (alterado)="recarregarPropriedade()"
      />

      <div class="cartao">
        <h2>Diagnóstico, croqui e contrato</h2>
        <p class="ajuda">
          O diagnóstico ambiental, o cálculo do PSA e o envio do croqui desta propriedade estarão
          disponíveis nas próximas etapas do frontend.
        </p>
      </div>

      <app-visitas [propriedadeId]="p.id" [podeEditar]="ehTecnico" />
    } @else if (!falha()) {
      <p class="vazio">Carregando…</p>
    }
  `,
})
export class PropriedadeDetalhe {
  readonly id = input.required({ transform: idObrigatorio });

  private readonly service = inject(CadastrosService);
  private readonly auth = inject(AuthService);

  protected readonly formatarArea = formatarArea;
  protected readonly formatarInstante = formatarInstante;
  protected readonly nomesCar = NOMES_CAR;
  protected readonly ehTecnico = this.auth.temPerfil('TECNICO');
  protected readonly podeEditarCadastro = this.auth.temPerfil('TECNICO', 'GESTOR');

  protected readonly propriedade = signal<Propriedade | null>(null);
  protected readonly produtor = signal<Produtor | null>(null);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly mensagem = signal<string | null>(null);

  private readonly localidades = toSignal(
    this.service.localidades().pipe(catchError(() => of([]))),
    {
      initialValue: [],
    },
  );
  private readonly subBacias = toSignal(this.service.subBacias().pipe(catchError(() => of([]))), {
    initialValue: [],
  });

  protected readonly nomeLocalidade = computed(() => {
    const p = this.propriedade();
    const l = this.localidades().find((x) => x.id === p?.localidadeId);
    return l ? `${l.nome} — ${l.municipio}/${l.uf}` : p ? `Localidade ${p.localidadeId}` : '—';
  });
  protected readonly nomeSubBacia = computed(() => {
    const id = this.propriedade()?.subBaciaId;
    if (!id) return 'Não informada';
    return this.subBacias().find((s) => s.id === id)?.nome ?? `Sub-bacia ${id}`;
  });

  constructor() {
    const estado = inject(Router).currentNavigation()?.extras.state;
    if (estado?.['mensagem']) this.mensagem.set(String(estado['mensagem']));

    effect(() => {
      const id = this.id();
      untracked(() => void this.carregar(id));
    });
  }

  private async carregar(id: number): Promise<void> {
    this.falha.set(null);
    try {
      const p = await firstValueFrom(this.service.consultarPropriedade(id));
      this.propriedade.set(p);
      this.produtor.set(await firstValueFrom(this.service.consultarProdutor(p.produtorId)));
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }

  protected async recarregarPropriedade(): Promise<void> {
    try {
      this.propriedade.set(await firstValueFrom(this.service.consultarPropriedade(this.id())));
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }
}
