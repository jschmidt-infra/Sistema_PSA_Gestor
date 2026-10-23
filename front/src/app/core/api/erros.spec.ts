import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { interpretarErro } from './erros';

describe('interpretarErro', () => {
  it('usa a mensagem e os campos do formato ErroApi', () => {
    const erro = new HttpErrorResponse({
      status: 422,
      error: {
        codigo: 'DADOS_INVALIDOS',
        mensagem: 'Dados inválidos.',
        campos: [{ campo: 'nome', mensagem: 'obrigatório' }],
        requisicaoId: 'req-1',
      },
    });
    const f = interpretarErro(erro);
    expect(f.mensagem).toBe('Dados inválidos.');
    expect(f.campos).toHaveLength(1);
    expect(f.requisicaoId).toBe('req-1');
  });

  it('diferencia indisponibilidade (503) e falha de rede (0) de sessão expirada', () => {
    const indisponivel = interpretarErro(new HttpErrorResponse({ status: 503 }));
    const rede = interpretarErro(new HttpErrorResponse({ status: 0 }));
    const sessao = interpretarErro(new HttpErrorResponse({ status: 401 }));
    expect(indisponivel.mensagem).toContain('indisponível');
    expect(rede.mensagem).toContain('servidor');
    expect(sessao.mensagem).toContain('sessão');
  });

  it('usa o X-Request-ID do cabeçalho quando o corpo não é ErroApi', () => {
    const erro = new HttpErrorResponse({
      status: 500,
      headers: new HttpHeaders({ 'X-Request-ID': 'abc' }),
    });
    expect(interpretarErro(erro).requisicaoId).toBe('abc');
  });
});
