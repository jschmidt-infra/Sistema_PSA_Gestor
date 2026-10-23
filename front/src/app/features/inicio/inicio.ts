import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { NOMES_PERFIS } from '../../core/auth/perfis';
import { menuDoPerfil } from '../../core/layout/menu';

/** Página inicial: atalhos para as funcionalidades do perfil autenticado. */
@Component({
  selector: 'app-inicio',
  imports: [RouterLink],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <h1>Olá, {{ primeiroNome() }}</h1>
        <p>Você está conectado como {{ nomePerfil() }}.</p>
      </div>
    </div>

    <div class="atalhos">
      @for (item of atalhos(); track item.rota) {
        <a class="cartao atalho" [routerLink]="item.rota">
          <strong>{{ item.rotulo }}</strong>
          <span>{{ item.grupo }}</span>
          @if (item.emConstrucao) {
            <span class="etiqueta">em breve</span>
          }
        </a>
      }
    </div>
  `,
  styles: `
    .atalhos {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(14rem, 1fr));
      gap: 1rem;
    }
    .cartao.atalho + .cartao.atalho {
      margin-top: 0;
    }
    .atalho {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
      text-decoration: none;
      color: var(--texto);
      transition: border-color 0.15s;
      &:hover {
        border-color: var(--primaria);
      }
      span {
        font-size: 0.85rem;
        color: var(--texto-suave);
      }
    }
    .etiqueta {
      align-self: flex-start;
      font-size: 0.7rem;
      padding: 0 0.4rem;
      border: 1px solid var(--borda);
      border-radius: 999px;
    }
  `,
})
export class Inicio {
  private readonly auth = inject(AuthService);

  protected readonly primeiroNome = computed(() => this.auth.usuario()?.nome.split(' ')[0] ?? '');
  protected readonly nomePerfil = computed(() => {
    const p = this.auth.perfil();
    return p ? NOMES_PERFIS[p].toLowerCase() : '';
  });
  protected readonly atalhos = computed(() =>
    menuDoPerfil(this.auth.perfil())
      .filter((g) => g.titulo !== 'Geral')
      .flatMap((g) => g.itens.map((i) => ({ ...i, grupo: g.titulo }))),
  );
}
