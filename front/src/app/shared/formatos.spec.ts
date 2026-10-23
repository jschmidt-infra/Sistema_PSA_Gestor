import { formatarData, normalizarDecimal, somenteDigitos, vazioParaNulo } from './formatos';

describe('formatos', () => {
  it('normaliza decimais digitados com vírgula sem converter para número', () => {
    expect(normalizarDecimal('10,5')).toBe('10.5');
    expect(normalizarDecimal('1.234,5678')).toBe('1234.5678');
    expect(normalizarDecimal('10.5000')).toBe('10.5000');
    expect(normalizarDecimal('  ')).toBeNull();
  });

  it('exibe datas YYYY-MM-DD sem deslocamento de fuso', () => {
    expect(formatarData('2026-01-01')).toBe('01/01/2026');
    expect(formatarData(null)).toBe('—');
  });

  it('remove máscara de documento e converte vazios em null', () => {
    expect(somenteDigitos('123.456.789-01')).toBe('12345678901');
    expect(somenteDigitos('')).toBeNull();
    expect(vazioParaNulo('  ')).toBeNull();
    expect(vazioParaNulo(' a ')).toBe('a');
  });
});
