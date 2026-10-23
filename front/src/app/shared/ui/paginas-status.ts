import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

/** Tela das funcionalidades previstas no menu cuja interface ainda será desenvolvida. */
@Component({
  selector: 'app-em-construcao',
  imports: [RouterLink],
  template: `
    <div class="cabecalho-pagina">
      <div>
        <h1>{{ titulo }}</h1>
        <p>Funcionalidade disponível na API; a tela está em desenvolvimento.</p>
      </div>
    </div>
    <div class="cartao">
      <p>
        Esta tela fará parte das próximas etapas do frontend. As rotas da API correspondentes já
        estão documentadas em <code>api/docs/API.md</code>.
      </p>
      <a routerLink="/inicio" class="botao secundario">Voltar ao início</a>
    </div>
  `,
})
export class EmConstrucao {
  protected readonly titulo: string =
    inject(ActivatedRoute).snapshot.data['titulo'] ?? 'Em construção';
}

@Component({
  selector: 'app-acesso-negado',
  imports: [RouterLink],
  template: `
    <div class="cartao">
      <h1>Acesso não permitido</h1>
      <p>Seu perfil não tem acesso a esta tela.</p>
      <a routerLink="/inicio" class="botao secundario">Voltar ao início</a>
    </div>
  `,
})
export class AcessoNegado {}

@Component({
  selector: 'app-nao-encontrada',
  imports: [RouterLink],
  template: `
    <div class="cartao">
      <h1>Página não encontrada</h1>
      <p>O endereço acessado não existe.</p>
      <a routerLink="/inicio" class="botao secundario">Voltar ao início</a>
    </div>
  `,
})
export class NaoEncontrada {}

/** Exibida quando a API não responde; não significa que a sessão terminou. */
@Component({
  selector: 'app-indisponivel',
  template: `
    <main class="centralizado">
      <div class="cartao">
        <h1>Serviço indisponível</h1>
        <p>
          Não foi possível contatar o servidor do PSA Gestor. Verifique a conexão e tente novamente.
        </p>
        <button type="button" class="botao" (click)="recarregar()">Tentar novamente</button>
      </div>
    </main>
  `,
  styles: `
    .centralizado {
      display: grid;
      place-items: center;
      min-height: 100dvh;
      padding: 1rem;
    }
    .cartao {
      max-width: 28rem;
    }
  `,
})
export class Indisponivel {
  protected recarregar(): void {
    location.assign('/');
  }
}
