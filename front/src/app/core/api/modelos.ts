/** Tipos compartilhados do contrato HTTP descrito em api/docs/API.md. */

export type Perfil = 'ADMINISTRADOR' | 'GESTOR' | 'TECNICO' | 'PRODUTOR';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: Perfil;
  ativo: boolean;
  produtorId: number | null;
}

/** Coleções paginadas: página inicial 0 e tamanho entre 1 e 100. */
export interface Pagina<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
}

export interface ErroCampo {
  campo: string;
  mensagem: string;
}

export interface ErroApi {
  codigo: string;
  mensagem: string;
  campos: ErroCampo[];
  requisicaoId: string | null;
}

/** Resposta das criações (201), que devolvem apenas o identificador. */
export interface Criado {
  id: number;
}

/**
 * Decimais chegam como string para não perder precisão no JavaScript.
 * O frontend os mantém como string e só os converte para exibição.
 */
export type Decimal = string;

/** Datas no formato YYYY-MM-DD. */
export type Data = string;

/** Instantes ISO 8601 em UTC. */
export type Instante = string;
