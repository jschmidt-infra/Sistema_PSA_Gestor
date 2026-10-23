import { Data, Decimal, Instante } from '../../core/api/modelos';

/** Campos de auditoria presentes nas saídas dos cadastros. */
interface Auditado {
  criadoEm: Instante;
  criadoPor: number;
  atualizadoEm: Instante | null;
  atualizadoPor: number | null;
}

export interface Produtor extends Auditado {
  id: number;
  nome: string;
  documento: string | null;
  endereco: string | null;
  telefone: string | null;
  email: string | null;
}

export interface ProdutorEntrada {
  nome: string;
  /** CPF (11) ou CNPJ (14), apenas dígitos. Obrigatório para formalizar contrato. */
  documento: string | null;
  endereco: string | null;
  telefone: string | null;
  email: string | null;
}

export type CarSituacao = 'POSSUI' | 'NAO_POSSUI' | 'NAO_SE_APLICA';
export type FontesSituacao = 'NAO_AVALIADO' | 'AUSENTES' | 'PRESENTES';

export interface Propriedade extends Auditado {
  id: number;
  produtorId: number;
  localidadeId: number;
  subBaciaId: number | null;
  nome: string;
  areaTotal: Decimal;
  carSituacao: CarSituacao;
  carNumero: string | null;
  fontesSituacao: FontesSituacao;
}

export interface PropriedadeEntrada {
  produtorId: number;
  localidadeId: number;
  subBaciaId: number | null;
  nome: string;
  areaTotal: Decimal;
  carSituacao: CarSituacao;
  carNumero: string | null;
}

export type TipoFonte = 'NASCENTE' | 'RIO' | 'SANGA' | 'ACUDE';
export type Protecao = 'PROTEGIDA' | 'NAO_PROTEGIDA' | 'NAO_AVALIADA';

export interface FonteHidrica extends Auditado {
  id: number;
  propriedadeId: number;
  tipo: TipoFonte;
  protecao: Protecao;
  ativo: boolean;
  observacoes: string | null;
}

export interface FonteEntrada {
  tipo: TipoFonte;
  protecao: Protecao;
  ativo: boolean;
  observacoes: string | null;
}

export interface Visita extends Auditado {
  id: number;
  propriedadeId: number;
  tecnicoId: number;
  dataVisita: Data;
  observacoes: string;
}

export interface VisitaEntrada {
  dataVisita: Data;
  observacoes: string;
}

export interface Localidade {
  id: number;
  nome: string;
  municipio: string;
  uf: string;
}

export interface SubBacia {
  id: number;
  nome: string;
}

export interface Projeto {
  id: number;
  nome: string;
  origemRecursos: string;
  ativo: boolean;
}

export const NOMES_CAR: Record<CarSituacao, string> = {
  POSSUI: 'Possui CAR',
  NAO_POSSUI: 'Não possui',
  NAO_SE_APLICA: 'Não se aplica',
};

export const NOMES_FONTES_SITUACAO: Record<FontesSituacao, string> = {
  NAO_AVALIADO: 'Não avaliado',
  AUSENTES: 'Sem fontes hídricas',
  PRESENTES: 'Com fontes hídricas',
};

export const NOMES_TIPO_FONTE: Record<TipoFonte, string> = {
  NASCENTE: 'Nascente',
  RIO: 'Rio',
  SANGA: 'Sanga',
  ACUDE: 'Açude',
};

export const NOMES_PROTECAO: Record<Protecao, string> = {
  PROTEGIDA: 'Protegida',
  NAO_PROTEGIDA: 'Não protegida',
  NAO_AVALIADA: 'Não avaliada',
};
