import { Data, Decimal, Instante } from '../core/api/modelos';

/**
 * Formatação para exibição. Os valores decimais continuam como string no estado
 * da aplicação e no envio à API; a conversão para número acontece só aqui.
 */

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const area = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 4 });
const dataHora = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' });

export function formatarMoeda(valor: Decimal | null | undefined): string {
  return valor == null ? '—' : moeda.format(Number(valor));
}

export function formatarArea(valor: Decimal | null | undefined): string {
  return valor == null ? '—' : `${area.format(Number(valor))} ha`;
}

/** Datas YYYY-MM-DD são exibidas sem conversão de fuso, evitando mudar o dia. */
export function formatarData(valor: Data | null | undefined): string {
  if (!valor) return '—';
  const [a, m, d] = valor.split('-');
  return `${d}/${m}/${a}`;
}

export function formatarInstante(valor: Instante | null | undefined): string {
  return valor ? dataHora.format(new Date(valor)) : '—';
}

/** Data de hoje no fuso local, no formato aceito pela API. */
export function hoje(): Data {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, '0');
  const dia = String(d.getDate()).padStart(2, '0');
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Aceita vírgula como separador decimal na digitação e devolve o formato da API. */
export function normalizarDecimal(valor: string | null | undefined): Decimal | null {
  if (valor == null) return null;
  let texto = String(valor).trim();
  // "1.234,56" → "1234.56"; "1234.56" permanece como está.
  if (texto.includes(',')) texto = texto.replace(/\./g, '').replace(',', '.');
  return texto === '' ? null : texto;
}

/** Remove máscara de CPF/CNPJ, mantendo só os dígitos esperados pela API. */
export function somenteDigitos(valor: string | null | undefined): string | null {
  const digitos = (valor ?? '').replace(/\D/g, '');
  return digitos === '' ? null : digitos;
}

/** Converte string vazia em null antes do envio, para campos opcionais. */
export function vazioParaNulo(valor: string | null | undefined): string | null {
  const t = (valor ?? '').trim();
  return t === '' ? null : t;
}
