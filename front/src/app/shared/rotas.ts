/** Converte parâmetros de rota em identificador numérico; ausente ou inválido vira undefined. */
export function idOpcional(valor: unknown): number | undefined {
  if (valor == null || valor === '') return undefined;
  const n = Number(valor);
  return Number.isInteger(n) && n > 0 ? n : undefined;
}

export function idObrigatorio(valor: unknown): number {
  return idOpcional(valor) ?? 0;
}
