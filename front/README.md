# PSA Gestor — Frontend

Interface web responsiva do PSA Gestor, em Angular 21. Ela consome a API REST em `/api/v1` na **mesma origem**: o proxy do Angular no desenvolvimento e o Nginx Proxy Manager na VPS (Arquitetura 2.0, seções 4 e 12). O frontend não acessa o banco nem o bucket R2.

## Estado atual

| Área | Telas | Situação |
| --- | --- | --- |
| Núcleo | Sessão por cookie, CSRF, guards por perfil, tratamento de 401/403/429/503, layout responsivo | Pronto |
| `acesso` | Login, alterar a própria senha, administração de usuários (criar, editar, redefinir senha, liberar bloqueio de login) | Pronto |
| `cadastros` | Produtores, propriedades (área, CAR), fontes hídricas com registro explícito de ausência (UC02/A2), visitas técnicas | Pronto |
| `configuracao` | Parâmetros e questionário por projeto, antecedência dos avisos de vencimento | A fazer |
| `diagnostico` / `calculo` | Questionário por grupos, salvamento progressivo, conclusão, correção, cálculo do PSA | A fazer |
| `arquivos` | Envio e download de croquis | A fazer |
| `contratos` | Rascunho, PDF, assinatura simulada, aprovação, alertas de vencimento | A fazer |
| `financeiro` | Aportes, pagamentos (com `chaveOperacao`), saldo e conferência (UC08) | A fazer |
| `consultas` | Relatório de aplicação, indicadores, histórico do produtor | A fazer |

Os itens “a fazer” já aparecem no menu com a marca **em breve** e têm rota protegida por perfil.

## Pré-requisitos

- Node.js 24 LTS (inclui o npm). No Windows: `winget install OpenJS.NodeJS.LTS`.
- A API em execução em `http://127.0.0.1:8080`. Veja `api/README.md` e `api/docs/MARIADB_LOCAL.md`.

## Executar

```powershell
npm install
npm start
```

Abra `http://127.0.0.1:4200`. O `angular.json` já aponta o servidor de desenvolvimento para `proxy.conf.json`, que encaminha `/api` para a API local. Use sempre `127.0.0.1` no navegador: os cookies de `localhost` e de `127.0.0.1` não são compartilhados.

O login usa contas existentes. O primeiro administrador é criado no backend com `.\scripts\Iniciar-ApiLocal.ps1 -CriarAdministrador`, e ele cria as demais contas pela tela **Usuários**.

## Build e testes

```powershell
npm run build      # gera dist/front/browser, para o Nginx estático
npm run test:ci    # testes unitários (Vitest)
```

Compile fora da VPS e publique só o conteúdo de `dist/front/browser` (Arquitetura 2.0, seção 11.1). Na publicação, o Nginx precisa devolver `index.html` para as rotas do Angular (`try_files $uri /index.html`), revalidar o `index.html` e manter cache longo nos arquivos com hash.

## Organização

A estrutura espelha os módulos do backend (`br.unisc.psagestor.*`):

```text
src/app/
├── core/                  # Infraestrutura transversal, carregada uma vez
│   ├── api/               # Tipos do contrato, CSRF, interceptor HTTP, interpretação de erros
│   ├── auth/              # AuthService (sessão), guards por perfil, nomes dos perfis
│   └── layout/            # Shell (cabeçalho + menu lateral) e menu por perfil
├── shared/                # Componentes e funções reutilizáveis (paginação, erros, formatos)
└── features/              # Um diretório por módulo de negócio, carregado sob demanda
    ├── acesso/            # login, senha, usuários
    ├── cadastros/         # produtores, propriedades, fontes hídricas, visitas
    └── inicio/            # atalhos do perfil
```

Convenções:

- Componentes standalone, `signal`/`computed` para o estado e controle de fluxo nativo (`@if`, `@for`). A aplicação é zoneless (padrão do Angular 21).
- Cada módulo tem seu serviço HTTP (`*.service.ts`) e seus tipos (`*.modelos.ts`). Os componentes não montam URLs.
- **Decimais ficam como string**, exatamente como a API os envia. A conversão para número acontece só na formatação de exibição (`shared/formatos.ts`). Datas `YYYY-MM-DD` são exibidas sem conversão de fuso.
- As rotas recebem parâmetros como `input()` (`withComponentInputBinding`).
- Os textos da interface são em português, assim como os nomes de domínio usados no backend.

## Autenticação e segurança

Segue `api/docs/AUTENTICACAO_FRONTEND.md`:

- O cookie `PSA_SESSION` é `HttpOnly` e gerenciado pelo navegador. O frontend guarda apenas os dados de apresentação do usuário, em memória, e os confirma em `GET /auth/me` ao recarregar. Nada vai para `localStorage`.
- O token CSRF vem de `GET /auth/csrf`, em JSON. O suporte XSRF padrão do Angular, baseado em cookie, **não** é usado. O `apiInterceptor` anexa o cabeçalho informado pela API (`headerName`) em POST, PUT, PATCH e DELETE, e só para caminhos `/api/`.
- O token é descartado após login, logout, troca de senha e qualquer 403 em gravação; a próxima gravação busca outro.
- **Nenhuma gravação é repetida automaticamente.** Em pagamentos, a tela deve preservar a mesma `chaveOperacao` ao reenviar.
- 401 fora das rotas de autenticação leva à tela de login com o aviso de sessão expirada e o endereço de retorno. 503 e falha de rede são exibidos como indisponibilidade, não como logout.
- 429 no login informa o tempo de espera a partir do `Retry-After`.
- Menus e guards apenas organizam a navegação. A autorização definitiva é sempre da API; o administrador não herda telas de gestor ou técnico.

## Próximos passos sugeridos

1. Parâmetros do projeto e questionário por grupos (administrador).
2. Diagnóstico com salvamento progressivo e cálculo do PSA (técnico), e envio de croqui.
3. Contratos: geração do PDF, assinatura simulada, aprovação e alertas de vencimento.
4. Financeiro: aportes, pagamentos idempotentes e conferência do saldo.
5. Consultas: relatório, indicadores e histórico do produtor.
6. Dockerfile do Nginx estático e configuração do proxy na VPS.
