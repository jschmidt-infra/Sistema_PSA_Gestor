import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { NOMES_PERFIS } from '../auth/perfis';
import { menuDoPerfil } from './menu';

/** Estrutura das telas autenticadas: cabeçalho, menu lateral por perfil e conteúdo. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
})
export class Shell {
  private readonly auth = inject(AuthService);

  protected readonly usuario = this.auth.usuario;
  protected readonly nomePerfil = computed(() => {
    const p = this.auth.perfil();
    return p ? NOMES_PERFIS[p] : '';
  });
  protected readonly grupos = computed(() => menuDoPerfil(this.auth.perfil()));
  protected readonly menuAberto = signal(false);
  protected readonly saindo = signal(false);

  constructor() {
    // Em telas estreitas, o menu fecha ao navegar.
    inject(Router)
      .events.pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.menuAberto.set(false));
  }

  protected alternarMenu(): void {
    this.menuAberto.update((v) => !v);
  }

  protected async sair(): Promise<void> {
    this.saindo.set(true);
    try {
      await this.auth.sair();
    } finally {
      this.saindo.set(false);
    }
  }
}
