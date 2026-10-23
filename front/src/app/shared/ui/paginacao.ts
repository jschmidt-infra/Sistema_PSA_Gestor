import { Component, computed, input, output } from '@angular/core';

/** Navegação entre páginas das coleções {items, page, size, totalItems}. */
@Component({
  selector: 'app-paginacao',
  template: `
    @if (totalPaginas() > 1) {
      <nav class="paginacao" aria-label="Paginação">
        <button
          type="button"
          class="botao secundario pequeno"
          [disabled]="pagina() === 0"
          (click)="mudar.emit(pagina() - 1)"
        >
          Anterior
        </button>
        <span>Página {{ pagina() + 1 }} de {{ totalPaginas() }} · {{ total() }} registros</span>
        <button
          type="button"
          class="botao secundario pequeno"
          [disabled]="pagina() + 1 >= totalPaginas()"
          (click)="mudar.emit(pagina() + 1)"
        >
          Próxima
        </button>
      </nav>
    }
  `,
  styles: `
    .paginacao {
      display: flex;
      flex-wrap: wrap;
      align-items: center;
      justify-content: center;
      gap: 0.75rem;
      margin-top: 1rem;
      font-size: 0.875rem;
      color: var(--texto-suave);
    }
  `,
})
export class Paginacao {
  readonly pagina = input.required<number>();
  readonly tamanho = input.required<number>();
  readonly total = input.required<number>();
  readonly mudar = output<number>();

  protected readonly totalPaginas = computed(() =>
    Math.max(1, Math.ceil(this.total() / this.tamanho())),
  );
}
