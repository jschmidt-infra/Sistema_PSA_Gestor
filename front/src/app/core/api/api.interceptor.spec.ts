import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SessaoExpirada } from '../auth/sessao-expirada';
import { apiInterceptor } from './api.interceptor';

describe('apiInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([apiInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  function responderCsrf(token = 'tok-1'): void {
    backend.expectOne('/api/v1/auth/csrf').flush({ token, headerName: 'X-CSRF-TOKEN' });
  }

  async function aguardarRequisicao(): Promise<void> {
    await new Promise((r) => setTimeout(r));
  }

  it('não envia CSRF em leituras', () => {
    http.get('/api/v1/produtores').subscribe();
    const req = backend.expectOne('/api/v1/produtores');
    expect(req.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('acrescenta o cabeçalho informado pela API nas gravações', async () => {
    const resposta = firstValueFrom(http.post('/api/v1/produtores', { nome: 'X' }));
    responderCsrf('abc');
    await aguardarRequisicao();
    const req = backend.expectOne('/api/v1/produtores');
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('abc');
    req.flush({ id: 1 });
    await resposta;
  });

  it('reaproveita o token em memória entre gravações', async () => {
    const primeira = firstValueFrom(http.put('/api/v1/visitas/1', {}));
    responderCsrf('abc');
    await aguardarRequisicao();
    backend.expectOne('/api/v1/visitas/1').flush({});
    await primeira;

    const segunda = firstValueFrom(http.put('/api/v1/visitas/2', {}));
    await aguardarRequisicao();
    backend.expectNone('/api/v1/auth/csrf');
    expect(backend.expectOne('/api/v1/visitas/2').request.headers.get('X-CSRF-TOKEN')).toBe('abc');
    backend.match('/api/v1/visitas/2').forEach((r) => r.flush({}));
  });

  it('descarta o token após 403 e busca outro na gravação seguinte, sem repetir a requisição', async () => {
    const falha = firstValueFrom(http.post('/api/v1/pagamentos', {})).catch((e) => e);
    responderCsrf('antigo');
    await aguardarRequisicao();
    backend
      .expectOne('/api/v1/pagamentos')
      .flush({ codigo: 'ACESSO_NEGADO', mensagem: '' }, { status: 403, statusText: 'Forbidden' });
    expect((await falha).status).toBe(403);
    backend.expectNone('/api/v1/pagamentos');

    const nova = firstValueFrom(http.post('/api/v1/pagamentos', {}));
    responderCsrf('novo');
    await aguardarRequisicao();
    const req = backend.expectOne('/api/v1/pagamentos');
    expect(req.request.headers.get('X-CSRF-TOKEN')).toBe('novo');
    req.flush({});
    await nova;
  });

  it('não envia o token para URLs externas', () => {
    http.post('https://exemplo.test/coleta', {}).subscribe();
    const req = backend.expectOne('https://exemplo.test/coleta');
    expect(req.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    req.flush({});
  });

  it('notifica sessão expirada em 401 fora das rotas de autenticação', async () => {
    const sessao = TestBed.inject(SessaoExpirada);
    const falha = firstValueFrom(http.get('/api/v1/propriedades')).catch(() => null);
    backend
      .expectOne('/api/v1/propriedades')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    await falha;
    expect(sessao.ocorrencias()).toBe(1);
  });

  it('não trata o 401 de /auth/me como expiração', async () => {
    const sessao = TestBed.inject(SessaoExpirada);
    const falha = firstValueFrom(http.get('/api/v1/auth/me')).catch(() => null);
    backend.expectOne('/api/v1/auth/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    await falha;
    expect(sessao.ocorrencias()).toBe(0);
  });
});
