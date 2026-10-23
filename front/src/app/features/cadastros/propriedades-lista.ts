import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { catchError, firstValueFrom, of } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { Pagina } from '../../core/api/modelos';
import { AuthService } from '../../core/auth/auth.service';
import { formatarArea } from '../../shared/formatos';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { Paginacao } from '../../shared/ui/paginacao';
import { NOMES_CAR, NOMES_FONTES_SITUACAO, Propriedade } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

@Component({
  selector: 'app-propriedades-lista',
  imports: [RouterLink, MensagemErro, Paginacao],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <h1>Propriedades</h1>
        <p>Propriedades rurais cadastradas no programa.</p>
      </div>
      @if (podeCriar) {
        <a routerLink="/propriedades/nova" class="botao">Nova propriedade</a>
      }
    </div>

    <app-mensagem-erro [falha]="falha()" />
    <div class="cartao">
      @if (pagina(); as p) {
        @if (p.items.length === 0) {
          <p class="vazio">Nenhuma propriedade cadastrada.</p>
        } @else {
          <div class="tabela-envoltorio">
            <table class="tabela">
              <thead>
                <tr>
                  <th>Propriedade</th>
                  <th>Produtor</th>
                  <th>Localidade</th>
                  <th class="numero">Área total</th>
                  <th>CAR</th>
                  <th>Fontes hídricas</th>
                </tr>
              </thead>
              <tbody>
                @for (pr of p.items; track pr.id) {
                  <tr>
                    <td data-rotulo="Propriedade">
                      <a [routerLink]="['/propriedades', pr.id]">{{ pr.nome }}</a>
                    </td>
                    <td data-rotulo="Produtor">
                      <a [routerLink]="['/produtores', pr.produtorId]"
                        >Código {{ pr.produtorId }}</a
                      >
                    </td>
                    <td data-rotulo="Localidade">{{ nomeLocalidade(pr.localidadeId) }}</td>
                    <td data-rotulo="Área total" class="numero">
                      {{ formatarArea(pr.areaTotal) }}
                    </td>
                    <td data-rotulo="CAR">{{ nomesCar[pr.carSituacao] }}</td>
                    <td data-rotulo="Fontes hídricas">
                      <span class="selo" [class.alerta]="pr.fontesSituacao === 'NAO_AVALIADO'">
                        {{ nomesFontes[pr.fontesSituacao] }}
                      </span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <app-paginacao
            [pagina]="p.page"
            [tamanho]="tamanho"
            [total]="p.totalItems"
            (mudar)="carregar($event)"
          />
        }
      } @else if (!falha()) {
        <p class="vazio">Carregando…</p>
      }
    </div>
  `,
})
export class PropriedadesLista {
  private readonly service = inject(CadastrosService);
  protected readonly podeCriar = inject(AuthService).temPerfil('TECNICO');
  protected readonly formatarArea = formatarArea;
  protected readonly nomesCar = NOMES_CAR;
  protected readonly nomesFontes = NOMES_FONTES_SITUACAO;
  protected readonly tamanho = 20;
  protected readonly pagina = signal<Pagina<Propriedade> | null>(null);
  protected readonly falha = signal<FalhaExibivel | null>(null);

  private readonly localidades = toSignal(
    this.service.localidades().pipe(catchError(() => of([]))),
    {
      initialValue: [],
    },
  );
  private readonly mapaLocalidades = computed(
    () => new Map(this.localidades().map((l) => [l.id, `${l.nome} — ${l.municipio}/${l.uf}`])),
  );

  constructor() {
    void this.carregar(0);
  }

  protected nomeLocalidade(id: number): string {
    return this.mapaLocalidades().get(id) ?? `Localidade ${id}`;
  }

  protected async carregar(page: number): Promise<void> {
    this.falha.set(null);
    try {
      this.pagina.set(await firstValueFrom(this.service.listarPropriedades(page, this.tamanho)));
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }
}
