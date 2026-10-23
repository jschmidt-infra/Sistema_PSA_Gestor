import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { Pagina, Perfil, Usuario } from '../../core/api/modelos';

export interface UsuarioEntrada {
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  /** Obrigatório só para PRODUTOR; omitido nos demais perfis. */
  produtorId: number | null;
}

export interface BloqueioLogin {
  tentativasSemSucesso: number;
  bloqueado: boolean;
  segundosRestantes: number;
}

/** Administração de contas (exclusiva do administrador). */
@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/v1/usuarios';

  listar(page: number, size: number): Observable<Pagina<Usuario>> {
    return this.http.get<Pagina<Usuario>>(this.base, { params: { page, size } });
  }

  criar(entrada: UsuarioEntrada & { senha: string }): Observable<Usuario> {
    return this.http.post<Usuario>(this.base, entrada);
  }

  editar(id: number, entrada: UsuarioEntrada): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.base}/${id}`, entrada);
  }

  redefinirSenha(id: number, novaSenha: string): Observable<void> {
    return this.http.put<void>(`${this.base}/${id}/senha`, { novaSenha });
  }

  consultarBloqueio(id: number): Observable<BloqueioLogin> {
    return this.http.get<BloqueioLogin>(`${this.base}/${id}/bloqueio-login`);
  }

  desbloquear(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}/bloqueio-login`);
  }
}
