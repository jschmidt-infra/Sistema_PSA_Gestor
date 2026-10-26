import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { Pagina } from '../../core/api/modelos';
import { AuthService } from '../../core/auth/auth.service';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { Paginacao } from '../../shared/ui/paginacao';
import { Produtor } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

@Component({
  selector: 'app-produtores-lista',
  imports: [RouterLink, MensagemErro, Paginacao],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <h1>Produtores</h1>
        <p>Produtores rurais participantes do programa.</p>
      </div>
      @if (podeCriar) {
        <a routerLink="/produtores/novo" class="botao">Novo produtor</a>
      }
    </div>

    <app-mensagem-erro [falha]="falha()" />
    <div class="cartao">
      @if (pagina(); as p) {
        @if (p.items.length === 0) {
          <p class="vazio">Nenhum produtor cadastrado.</p>
        } @else {
          <div class="tabela-envoltorio">
            <table class="tabela">
              <thead>
                <tr>
                  <th>Código</th>
                  <th>Nome</th>
                  <th>Contato</th>
                  <th>Documento</th>
                </tr>
              </thead>
              <tbody>
                @for (pr of p.items; track pr.id) {
                  <tr>
                    <td data-rotulo="Código">{{ pr.id }}</td>
                    <td data-rotulo="Nome">
                      <a [routerLink]="['/produtores', pr.id]">{{ pr.nome }}</a>
                    </td>
                    <td data-rotulo="Contato">{{ pr.telefone || pr.email || '—' }}</td>
                    <td data-rotulo="Documento">
                      @if (pr.documento) {
                        <span class="selo ok">Informado</span>
                      } @else {
                        <span class="selo alerta">Pendente</span>
                      }
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
export class ProdutoresLista {
  private readonly service = inject(CadastrosService);
  protected readonly podeCriar = inject(AuthService).temPerfil('TECNICO');
  protected readonly tamanho = 20;
  protected readonly pagina = signal<Pagina<Produtor> | null>(null);
  protected readonly falha = signal<FalhaExibivel | null>(null);

  constructor() {
    void this.carregar(0);
  }

  protected async carregar(page: number): Promise<void> {
    this.falha.set(null);
    try {
      this.pagina.set(await firstValueFrom(this.service.listarProdutores(page, this.tamanho)));
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    }
  }
}
