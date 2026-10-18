# Autenticação do PSA Gestor — guia para a equipe de frontend

Este guia descreve a API implementada em 16/10/2026. Leia as etapas na ordem: primeiro disponibilizar contas, depois autenticar, depois chamar as funcionalidades. Os exemplos Angular são orientações de integração; não existe projeto Angular nesta pasta para compilá-los aqui.

## 1. O que o frontend precisa saber

| Item | Comportamento da API |
| --- | --- |
| Identificação no login | E-mail e senha de uma conta previamente criada |
| Cadastro público | Não existe |
| Administrador inicial | Criado pelo backend durante o provisionamento |
| Demais contas | Criadas por um administrador autenticado |
| Autenticação | Sessão HTTP, com cookie `PSA_SESSION` |
| JWT / Bearer / Basic | Não utilizados |
| CSRF | Obrigatório nas operações de gravação, inclusive login e logout |
| Perfil e usuário atuais | Obtidos em `/api/v1/auth/me` |
| Duração da sessão | 30 minutos de inatividade; reiniciar a API também encerra as sessões |
| Cookie | `HttpOnly`; `Secure` em produção, desativado apenas no ambiente HTTP de desenvolvimento |
| Permissões | `ADMINISTRADOR`, `GESTOR`, `TECNICO`, `PRODUTOR` |

Não salve senha ou cookie de sessão em `localStorage`. O navegador gerencia o cookie; o frontend guarda os dados de apresentação do usuário em memória e os confirma pelo endpoint `/auth/me` ao recarregar. Uma informação de perfil manipulada no navegador não concede acesso no backend.

## 2. Criar o primeiro administrador — responsabilidade de quem inicia a API

**Esta etapa não é feita na tela de login.** Informar um e-mail/senha em `/auth/login` apenas tenta autenticar uma conta existente.

No PowerShell, na raiz do projeto, execute:

```powershell
.\scripts\Iniciar-ApiLocal.ps1 -CriarAdministrador
```

Informe nome, e-mail e senha quando solicitado. Não existe senha padrão. A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8. O provisionamento não redefine a senha de uma conta existente. Após criar a conta, encerre com Ctrl+C e execute novamente sem `-CriarAdministrador`. O script remove a senha inicial do ambiente ao encerrar.

Nas próximas execuções, use `.\scripts\Iniciar-ApiLocal.ps1`. As contas ficam no MariaDB `psa_gestor`; não apague o banco para corrigir login. As contas do banco antigo não foram migradas. Veja os requisitos e a compilação em [MariaDB local](MARIADB_LOCAL.md).

## 3. Criar contas para testar os perfis

Depois de entrar como administrador, envie `POST /api/v1/usuarios` com cookie de sessão e CSRF atualizado:

```json
{
  "nome": "Técnico de demonstração",
  "email": "tecnico@example.test",
  "perfil": "TECNICO",
  "ativo": true,
  "senha": "substitua-por-uma-senha-privada"
}
```

O retorno esperado é 201. Crie outra conta com perfil `GESTOR` para testar contratos e financeiro. Para `PRODUTOR`, primeiro um técnico cadastra o produtor; depois o administrador cria a conta com `perfil: "PRODUTOR"` e `produtorId` correspondente. Não informe `produtorId` para os demais perfis.

Cada integrante deve utilizar uma conta própria. O administrador não é um superusuário: ele não pode cadastrar propriedades ou registrar pagamentos somente por ser administrador. Consulte a [matriz de rotas](API.md#rotas).

## 4. Sequência HTTP completa de autenticação

### 4.1 Obter CSRF antes do login

```http
GET /api/v1/auth/csrf
```

Resposta 200, com valores ilustrativos:

```json
{"token":"valor-retornado-pela-api","headerName":"X-CSRF-TOKEN"}
```

Preserve o cookie enviado pelo servidor nessa etapa. Não misture o token de um navegador com o cookie de outro. Utilize o campo `headerName` da resposta, em vez de presumir o nome do cabeçalho.

### 4.2 Enviar e-mail e senha

```http
POST /api/v1/auth/login
Content-Type: application/json
X-CSRF-TOKEN: valor-retornado-pela-api
```

```json
{"email":"tecnico@example.test","senha":"senha-definida-para-a-conta"}
```

Resposta 200:

```json
{"id":2,"nome":"Técnico de demonstração","email":"tecnico@example.test","perfil":"TECNICO","ativo":true,"produtorId":null}
```

Não há `accessToken` ou `refreshToken` nessa resposta. O servidor troca o identificador da sessão e o navegador atualiza o cookie automaticamente. Não monte manualmente um cabeçalho `Cookie` no JavaScript do navegador.

### 4.3 Renovar o CSRF após o login

Execute novamente `GET /api/v1/auth/csrf` e utilize o novo token. **O token usado para o login não deve ser reutilizado para a primeira gravação após autenticar.** Um token antigo provoca 403.

### 4.4 Consultar e gravar dados

GETs autenticados precisam do cookie de sessão. POST, PUT e demais métodos de alteração precisam também do CSRF. Uploads `multipart/form-data` seguem a mesma regra; ao usar `FormData`, deixe o navegador definir `Content-Type` e o separador multipart.

Para consultar a identidade após recarregar a página:

```http
GET /api/v1/auth/me
```

Se retornar 200, restaure o usuário na interface. Se retornar 401, apresente a tela de login. Uma falha de rede ou 503 não prova que a senha está errada: trate como indisponibilidade.

### 4.5 Sair e alterar a senha

`POST /api/v1/auth/logout`, sem corpo, com cookie e CSRF: retorna 204 e encerra a sessão. Limpe os dados do usuário e o CSRF mantidos pelo frontend. O próximo login começa por obter outro CSRF.

`PUT /api/v1/auth/senha`, com cookie e CSRF:

```json
{"senhaAtual":"senha-atual","novaSenha":"nova-senha-com-pelo-menos-12-caracteres"}
```

O retorno é 204 e a sessão é encerrada. Peça login novamente. Alterações administrativas de senha, perfil, vínculo ou estado ativo também podem invalidar a sessão na próxima requisição.

## 5. Angular: configurar o proxy de desenvolvimento

Na pasta do frontend, crie `proxy.conf.json`:

```json
{
  "/api/**": {
    "target": "http://127.0.0.1:8080",
    "changeOrigin": true
  }
}
```

Inicie o frontend com:

```shell
ng serve --proxy-config proxy.conf.json
```

Use `/api/v1/...` no código Angular. O navegador acessa a origem do frontend, e o proxy encaminha a chamada para a API. Não altere para uma URL absoluta de outro domínio presumindo que `withCredentials` resolverá CORS. A configuração atual foi feita para chamadas de mesma origem; acesso direto entre origens exigiria outra configuração no backend.

Em produção, mantenha o caminho `/api` na origem do frontend encaminhado pelo proxy para a API. O proxy de desenvolvimento do Angular não existe após o build. Use sempre o mesmo nome de host durante os testes: cookies de `localhost` não são compartilhados com `127.0.0.1`.

A sintaxe do proxy segue a [documentação oficial do Angular CLI](https://angular.dev/tools/cli/serve).

## 6. Angular: exemplo de serviço de autenticação

Registre `provideHttpClient()` nos providers existentes de `app.config.ts`, preservando os providers de rotas e outros já configurados:

```typescript
import { provideHttpClient } from '@angular/common/http';

// Dentro de providers: [...]
provideHttpClient()
```

Exemplo de `auth.service.ts`, para execução no navegador:

```typescript
import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

export interface Usuario {
  id: number;
  nome: string;
  email: string;
  perfil: 'ADMINISTRADOR' | 'GESTOR' | 'TECNICO' | 'PRODUTOR';
  ativo: boolean;
  produtorId: number | null;
}

interface Csrf {
  token: string;
  headerName: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  readonly usuario = signal<Usuario | null>(null);

  private obterCsrf(): Promise<Csrf> {
    return firstValueFrom(this.http.get<Csrf>('/api/v1/auth/csrf', {
      withCredentials: true
    }));
  }

  async entrar(email: string, senha: string): Promise<Usuario> {
    const csrf = await this.obterCsrf();
    const usuario = await firstValueFrom(this.http.post<Usuario>(
      '/api/v1/auth/login',
      { email, senha },
      {
        withCredentials: true,
        headers: { [csrf.headerName]: csrf.token }
      }
    ));

    // O login invalidou o token anterior.
    await this.obterCsrf();
    this.usuario.set(usuario);
    return usuario;
  }

  async restaurarSessao(): Promise<Usuario | null> {
    try {
      const usuario = await firstValueFrom(this.http.get<Usuario>(
        '/api/v1/auth/me', { withCredentials: true }
      ));
      this.usuario.set(usuario);
      return usuario;
    } catch (erro) {
      if (erro instanceof HttpErrorResponse && erro.status === 401) {
        this.usuario.set(null);
        return null;
      }
      throw erro; // Não mascarar falhas de rede ou do servidor como logout.
    }
  }

  async gravar<T>(
    metodo: 'POST' | 'PUT' | 'PATCH' | 'DELETE',
    caminho: string,
    corpo?: unknown
  ): Promise<T> {
    // Não enviar o token a URLs externas.
    if (!caminho.startsWith('/api/v1/')) {
      throw new Error('Use um caminho relativo da API.');
    }
    // Exemplo simples: consulta um token válido antes de cada gravação.
    const csrf = await this.obterCsrf();
    return firstValueFrom(this.http.request<T>(metodo, caminho, {
      body: corpo,
      withCredentials: true,
      headers: { [csrf.headerName]: csrf.token }
    }));
  }

  async sair(): Promise<void> {
    await this.gravar<void>('POST', '/api/v1/auth/logout');
    this.usuario.set(null);
  }

  async alterarSenha(senhaAtual: string, novaSenha: string): Promise<void> {
    await this.gravar<void>('PUT', '/api/v1/auth/senha', {
      senhaAtual, novaSenha
    });
    this.usuario.set(null); // A API encerrou a sessão.
  }
}
```

O exemplo busca CSRF antes de cada gravação para deixar o fluxo explícito, com uma consulta adicional por operação. É possível centralizar o token em memória e usar um interceptor depois, mantendo a renovação após login e a recuperação após expiração. Não implemente simultaneamente dois mecanismos diferentes para acrescentar CSRF.

O suporte XSRF padrão do Angular busca um cookie próprio. **Este backend utiliza `HttpSessionCsrfTokenRepository` e retorna o token em JSON; `provideHttpClient()` sozinho não envia esse token.** Por isso o exemplo acrescenta o cabeçalho retornado pela API explicitamente. Consulte a [configuração oficial de HttpClient](https://angular.dev/guide/http/setup).

Exemplos de uso em componentes/serviços Angular:

```typescript
// Obtenha auth via inject(AuthService) no componente ou serviço.
await auth.entrar(emailDoFormulario, senhaDoFormulario);

await auth.gravar('POST', '/api/v1/produtores', {
  nome: 'Produtor de demonstração', telefone: '51999999999'
});

const form = new FormData();
form.append('arquivo', arquivoSelecionado);
form.append('areaPsa', '1.0000');
await auth.gravar('POST', `/api/v1/propriedades/${propriedadeId}/croquis`, form);

await auth.sair();
```

O cadastro de produtor e upload exigem conta de técnico. Desabilite o botão enquanto a operação está em andamento e não execute login/logout ao mesmo tempo que gravações. O componente deve tratar as exceções e limpar a senha do formulário após uso. Quando login retornar 200, mas a consulta subsequente de CSRF falhar, consulte `/auth/me` antes de presumir que a autenticação falhou.

## 7. Testar pelo Postman

Importe [a coleção da API](../postman/PSA_Gestor.postman_collection.json). Defina `baseUrl`, `email` e `senha` em variáveis privadas do seu ambiente. Deixe o gerenciamento de cookies habilitado.

Execute manualmente: **Obter CSRF → Login → Renovar CSRF após login → Minha conta → operações do perfil**. Os scripts da coleção guardam `csrfHeader` e `csrfToken`. Para trocar de perfil, saia, altere as credenciais privadas e repita a sequência. Não execute toda a coleção automaticamente: pastas diferentes exigem contas e dados diferentes.

## 8. Acessar o Swagger no navegador

O Swagger local fica em `http://127.0.0.1:8080/swagger-ui/index.html`, mas exige administrador autenticado **no mesmo navegador e host**. Login no Postman não autentica o navegador. Não há uma tela de login servida pelo backend nem fluxo Bearer no botão Authorize.

Quando a tela Angular de login estiver pronta, autentique como administrador. Para aproveitar essa sessão no Swagger, as requisições devem chegar à mesma instância da API e usar o mesmo host do cookie. O caminho mais direto para um teste independente do Angular é abrir `http://127.0.0.1:8080/api/v1/auth/csrf`, pressionar F12 e executar o exemplo abaixo no Console. Ele monta um formulário temporário com senha oculta; **não cria conta nem adiciona uma tela permanente à API**.

```javascript
(() => {
  const form = document.createElement('form');
  form.innerHTML = `
    <h2>Entrar no Swagger — conta de administrador existente</h2>
    <label>E-mail <input name="email" type="email" autocomplete="username" required></label><br>
    <label>Senha <input name="senha" type="password" autocomplete="current-password" required></label><br>
    <button type="submit">Entrar</button>
    <p role="status"></p>`;
  document.body.replaceChildren(form);
  form.onsubmit = async event => {
    event.preventDefault();
    const button = form.querySelector('button');
    const status = form.querySelector('[role="status"]');
    button.disabled = true;
    try {
      const dados = new FormData(form);
      const respostaCsrf = await fetch('/api/v1/auth/csrf');
      if (!respostaCsrf.ok) throw new Error('Não foi possível obter CSRF.');
      const csrf = await respostaCsrf.json();
      const resposta = await fetch('/api/v1/auth/login', {
        method: 'POST', credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
        body: JSON.stringify({ email: dados.get('email'), senha: dados.get('senha') })
      });
      form.querySelector('[name="senha"]').value = '';
      if (!resposta.ok) throw new Error(`Login recusado: HTTP ${resposta.status}.`);
      const usuario = await resposta.json();
      if (usuario.perfil !== 'ADMINISTRADOR') {
        status.textContent = 'Login válido, mas o Swagger exige administrador.';
        return;
      }
      location.assign('/swagger-ui/index.html');
    } catch (erro) {
      status.textContent = erro instanceof Error ? erro.message : 'Falha na autenticação.';
    } finally {
      button.disabled = false;
    }
  };
})();
```

Isso libera a leitura da documentação. A instalação atual do Swagger não está configurada para obter e anexar CSRF automaticamente no “Try it out”. Gravações sem esse cabeçalho continuam retornando 403. Para testar gravações, use a coleção Postman ou o frontend com o fluxo de CSRF acima. Mesmo autenticado como administrador, não será possível executar operações reservadas ao gestor/técnico.

## 9. Erros comuns e ação esperada

| Situação | Causa possível | O que fazer |
| --- | --- | --- |
| Login retorna 401 | Conta não criada, senha incorreta ou conta inativa | Confirmar o provisionamento e as credenciais; login não cadastra usuário |
| Inicialização termina com “Configure nome e email válidos” | Variável vazia/inválida; e-mail colocado no texto do `Read-Host` | Usar o bloco da seção 2 e digitar somente a senha quando solicitado |
| Alterar senha nas variáveis não muda o login | Administrador já existia | Provisionamento não redefine senha; usar a alteração autenticada ou recuperação administrativa |
| Login/gravação retorna 403 | CSRF ausente, antigo ou associado a outro cookie; perfil sem permissão | Conferir token, cookies e permissão da rota; não considerar todo 403 como senha errada |
| Consulta autenticada retorna 401 | Sessão expirada, API reiniciada ou credenciais/perfil alterados | Mostrar login e iniciar novamente por `/auth/csrf` |
| Cookies não acompanham a chamada | Host diferente, URL absoluta entre origens, cookie Secure em HTTP | Usar proxy e URLs relativas; no teste HTTP, usar `local` |
| GET funciona, POST falha | Cookie está presente, mas falta CSRF | Enviar o cabeçalho retornado por `/auth/csrf` |
| Swagger não abre após login no Postman | Clientes têm cookies separados | Autenticar no navegador como administrador |
| 404 para produtor | Registro não existe ou pertence a outro produtor | Não tentar trocar `produtorId` para contornar o filtro |
| HTTP 0 / erro de rede | API parada, proxy ou rede | Verificar conectividade antes de pedir outra senha |
| 503 | Banco/armazenamento temporariamente indisponível | Exibir indisponibilidade; não afirmar que houve logout ou senha incorreta |

Não repita automaticamente pagamentos ou outras gravações em um interceptor genérico. Em pagamentos, preserve a mesma `chaveOperacao`, consulte o resultado e só reenvie conforme o contrato de idempotência.

## 10. Checklist de integração do frontend

### Bloqueio temporário e liberação administrativa

O limite é por e-mail normalizado em minúsculas, não por IP. A quinta tentativa consecutiva recusada bloqueia novos logins por uma hora. As quatro primeiras retornam 401; a quinta e as tentativas durante o bloqueio retornam 429, código `LOGIN_TEMPORARIAMENTE_BLOQUEADO`, com cabeçalho `Retry-After` em segundos. Mesmo a senha correta não permite entrar durante o bloqueio. Tentativas bloqueadas não prorrogam o prazo.

Um login bem-sucedido antes do bloqueio zera o contador. Ao terminar a hora, a próxima tentativa é liberada automaticamente com contador zerado. Erros de validação/CSRF não contam como tentativa de senha. E-mails inexistentes seguem o mesmo limite e a mesma mensagem. Uma conta inativa continua sem acesso, mesmo após a liberação do bloqueio.

O controle fica no MariaDB (migração V5), independente do cache Redis. Reiniciar a API ou limpar o Redis não remove o bloqueio. O estado utiliza uma chave derivada do e-mail, sem guardar a senha. A alteração de e-mail corresponde a outra chave de controle. Sessões já autenticadas não são encerradas por este bloqueio de novos logins.

Na tela administrativa de usuários, integrar:

| Método e rota | Resultado |
| --- | --- |
| `GET /api/v1/usuarios/{id}/bloqueio-login` | `tentativasSemSucesso`, `bloqueado` e `segundosRestantes`. |
| `DELETE /api/v1/usuarios/{id}/bloqueio-login` | HTTP 204: remove bloqueio e zera tentativas, com auditoria. Exige CSRF. |

Ambas as rotas são exclusivas de administrador autenticado. Um usuário inexistente retorna 404. O desbloqueio não muda a senha, o perfil nem o estado ativo. A ação também pode zerar tentativas antes de chegar ao limite. Não há rota pública de desbloqueio. Se o único administrador estiver bloqueado e não possuir sessão aberta, deverá aguardar a hora; outro administrador autenticado pode liberá-lo antes.

No frontend, trate 429 separadamente de 401 e 403: informe a espera, respeite `Retry-After` e não repita o login automaticamente. O botão de desbloqueio administrativo deve chamar a API; alterar apenas um indicador na tela não libera a conta.

- [ ] Conta criada antes do primeiro login; contas distintas para testar cada perfil.
- [ ] Requisições usam `/api/v1/...` com proxy configurado.
- [ ] CSRF obtido antes do login e renovado depois dele.
- [ ] Cookie gerenciado pelo navegador; nenhuma tentativa de ler `PSA_SESSION` por JavaScript.
- [ ] CSRF anexado nas gravações, uploads, alteração de senha e logout.
- [ ] Sessão restaurada por `/auth/me` ao abrir/recarregar o frontend.
- [ ] 401, 403 e 503 tratados de formas diferentes.
- [ ] 429 tratado com mensagem de bloqueio e prazo de espera, sem repetição automática.
- [ ] Administrador consegue consultar e remover o bloqueio pelo endpoint protegido.
- [ ] Logout e alteração de senha limpam o usuário da interface após confirmação da API.
- [ ] Menus respeitam o perfil; autorização definitiva permanece no backend.
- [ ] Senhas fora de repositórios, logs e exportações compartilhadas do Postman.

## Auditoria de acesso

A API registra os resultados de login e os encerramentos de sessão automaticamente. O frontend não precisa enviar credenciais adicionais nem chamar outra rota de auditoria. Não registre senhas, cookies ou tokens no console do navegador. Para detalhes, consulte [Eventos de acesso](EVENTOS_ACESSO.md).


## Alertas de vencimento

A configuração administrativa, os campos retornados e os filtros estão em [Alertas de vencimento](ALERTAS_VENCIMENTO.md). O prazo padrão é 30 dias e pode ser alterado pelo administrador via API.
