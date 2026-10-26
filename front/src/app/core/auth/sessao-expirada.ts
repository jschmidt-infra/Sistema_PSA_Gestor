import { Injectable, signal } from '@angular/core';

/**
 * Canal entre o interceptor HTTP e o AuthService.
 * Evita dependência circular: o AuthService usa HttpClient, que usa o interceptor.
 */
@Injectable({ providedIn: 'root' })
export class SessaoExpirada {
  readonly ocorrencias = signal(0);

  notificar(): void {
    this.ocorrencias.update((n) => n + 1);
  }
}
