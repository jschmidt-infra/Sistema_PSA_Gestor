import { Component, input } from '@angular/core';
import { FalhaExibivel } from '../../core/api/erros';

/** Apresenta a falha da API, os campos rejeitados e o identificador da requisição. */
@Component({
  selector: 'app-mensagem-erro',
  template: `
    @if (falha(); as f) {
      <div class="aviso erro" role="alert">
        <strong>{{ f.mensagem }}</strong>
        @if (f.campos.length) {
          <ul>
            @for (c of f.campos; track c.campo) {
              <li>{{ c.campo }}: {{ c.mensagem }}</li>
            }
          </ul>
        }
        @if (f.requisicaoId) {
          <div class="codigo">Código para suporte: {{ f.requisicaoId }}</div>
        }
      </div>
    }
  `,
  styles: `
    .codigo {
      font-size: 0.75rem;
      color: var(--texto-suave);
      margin-top: 0.25rem;
    }
  `,
})
export class MensagemErro {
  readonly falha = input<FalhaExibivel | null>(null);
}
