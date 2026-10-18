# PSA Gestor — API

Backend do PSA Gestor em Java 25 e Spring Boot 4.1.1, com autenticação por sessão e operações financeiras simuladas. A API executa no Windows e utiliza o MariaDB 13.0.2 no Docker Desktop, pela porta local 3306.

## Funcionalidades implementadas

- Login, logout, CSRF, alteração de senha e administração de contas sem cadastro público.
- Registro de sucesso/recusa de login e encerramento de sessão, sem credenciais; veja [Eventos de acesso](docs/EVENTOS_ACESSO.md).
- Bloqueio de novos logins por e-mail após cinco recusas consecutivas, por uma hora, com liberação automática ou antecipada por administrador.
- Perfis administrador, gestor, técnico e produtor, com verificação da titularidade dos registros.
- Produtores, propriedades, CAR, fontes hídricas, visitas e consulta de localidades, sub-bacias e projetos.
- Parâmetros versionados, diagnóstico, respostas, correção, apuração demonstrativa e memória de cálculo preservada.
- Croquis PDF/PNG/JPEG privados, validação do conteúdo e limite de 10 MiB.
- Contratos preliminares em PDF, assinatura simulada e aprovação com reserva do compromisso anual inicial.
- Saldo inicial, aportes, pagamentos parciais, idempotência, extrato do produtor, relatórios e indicadores.
- Conferência do saldo (UC08), com indicação de consolidação incompleta e identificação dos registros financeiros inconsistentes.
- Auditoria transacional, tratamento de erros, adaptador Cloudflare R2 e cache Redis opcional de indicadores não financeiros.
- Métricas protegidas para administradores, logs com identificador de requisição e sinalização de degradação/recuperação do Redis; veja [Observabilidade](docs/OBSERVABILIDADE.md).
- Migrações Flyway V1–V7, testes de integração e Swagger habilitado no ambiente local para administradores.

**Limites atuais:** o questionário oficial, os pesos, a metodologia de pontuação e o texto definitivo do contrato ainda dependem das definições do grupo/programa. Existe uma metodologia explicitamente demonstrativa, restrita aos perfis de execução `demonstracao` e `test`. Não há renovação automática de exercícios, transferência bancária ou assinatura digital. As operações do adaptador foram testadas no bucket R2 real; a integração completa pela VPS/Angular e o MariaDB da VPS ainda precisam de homologação.

## Executar no Windows com Docker Desktop

Siga [MariaDB local — configuração, execução e testes](docs/MARIADB_LOCAL.md).

```powershell
.\scripts\Testar-MariaDB.ps1
.\scripts\Iniciar-ApiLocal.ps1 -CriarAdministrador
```

Nas próximas execuções, use `.\scripts\Iniciar-ApiLocal.ps1`, sem o parâmetro de criação. A conta existente não é sobrescrita. Não há senha padrão. A API fica em `http://127.0.0.1:8080`; os dados ficam no volume do MariaDB gerenciado pelo Portainer, e os arquivos no Cloudflare R2.

## Experimentar a API

Importe `postman/PSA_Gestor.postman_collection.json` e mantenha as senhas apenas em variáveis locais privadas. Faça nesta ordem:

1. Obter CSRF: `GET /api/v1/auth/csrf`.
2. Login: `POST /api/v1/auth/login`, enviando o token CSRF e preservando o cookie.
3. Obter um novo CSRF após o login; o token anterior foi invalidado.
4. Executar as operações do perfil autenticado. Em POST e PUT, enviar o token atualizado.
5. Para trocar de perfil, fazer logout e repetir a autenticação.

Swagger: `http://127.0.0.1:8080/swagger-ui/index.html`, após autenticar **no mesmo navegador** como administrador. Postman e navegador não compartilham cookies. A documentação OpenAPI pode ser obtida por um cliente autenticado em `GET /v3/api-docs`. O contrato de uso e exemplos estão em [docs/API.md](docs/API.md).

## Integração com Angular

**Antes de implementar o login, leia [Autenticação e integração com Angular](docs/AUTENTICACAO_FRONTEND.md).** O guia contém a criação correta do administrador, a sequência de cookies/CSRF, um serviço Angular de exemplo, proxy local, acesso ao Swagger e solução dos erros 401/403. Login não cria uma conta.

Use URLs relativas, por exemplo `/api/v1/propriedades`. No desenvolvimento, configure o proxy do Angular para encaminhar `/api` a `http://127.0.0.1:8080`. No ambiente publicado, o proxy deve preservar a mesma origem para o navegador. A API mantém CSRF habilitado; o frontend deve buscar e enviar o token retornado pela API. Não existe autenticação JWT nesta implementação.

O produtor é identificado pela sessão, não por um identificador escolhido no frontend. Um registro de outro produtor retorna 404. Mudanças de perfil, senha ou estado ativo invalidam as sessões afetadas na próxima requisição. Reiniciar a API encerra as sessões existentes.

## Tecnologias

| Componente | Versão/uso |
| --- | --- |
| Java | 25; ambiente utilizado: 25.0.4.1 |
| Spring Boot | 4.1.1 |
| Maven Wrapper | 3.9.11 |
| Spring Security | Versão gerenciada pelo Spring Boot |
| Persistência | Spring Data JPA/Hibernate, com 21 entidades e repositórios por entidade; consultas específicas para relatórios e conferência |
| Banco local | MariaDB 13.0.2; container separado e descartável nos testes |
| Banco previsto na VPS | MariaDB 13.0.2 |
| Migrações | Flyway; Hibernate em `validate` |
| PDF | Apache PDFBox 3.0.8 |
| OpenAPI | Springdoc 3.1.1 |
| R2/S3 | AWS SDK Java 2.56.0 |
| Cache | Spring Data Redis/Lettuce gerenciados pelo Spring Boot |

## Regras financeiras

```text
Valor anual = (valor da terra por hectare × percentual) × pontuação × área contratada
Recursos = soma dos aportes
Reservas = soma dos compromissos anuais aprovados
Saldo em caixa = recursos − pagamentos
Saldo comprometido pendente = reservas − pagamentos
Saldo disponível para novos contratos = recursos − reservas
```

Percentual e pontuação são frações de 0 a 1. A área usa hectares. Valores monetários usam `BigDecimal`, com arredondamento final para duas casas decimais. Na API, valores decimais de saída são strings.

A aprovação e a reserva são uma operação atômica. Pagamentos não podem exceder o compromisso; não há edição ou exclusão de repasses registrados. Reenvios devem preservar `chaveOperacao`: dados iguais retornam o registro anterior; dados diferentes retornam 409. Em falha de persistência, consulte pela chave e reutilize a mesma chave, inclusive quando não houver certeza sobre a confirmação anterior.

## Arquivos e cache

Os arquivos são armazenados no Cloudflare R2, com bucket privado e referências no MariaDB. Downloads passam pela API após autorização. Somente os testes automatizados usam armazenamento local temporário. Consulte [Cloudflare R2](docs/R2.md).

Configure `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY` e `R2_SECRET_KEY` para a homologação. O endpoint usa a URL S3 da conta R2. Sem configuração, as operações de arquivo retornam 503. Um diário no banco permite limpar uploads órfãos e objetos substituídos: a limpeza verifica referências após uma hora e tenta novamente a cada dez minutos se o serviço estiver indisponível.

O Redis está ativado na configuração local privada, em `127.0.0.1:6379`, com autenticação. Nos demais ambientes, o padrão permanece desativado: `CACHE_ENABLED=false`. Configure `REDIS_HOST`, `REDIS_PORT` e `REDIS_PASSWORD` ao ativá-lo. Apenas quantidade de produtores e área de contratos vigentes usam cache, com TTL de 60 segundos. Saldo, reserva, pagamentos e totais financeiros sempre são consultados no banco. Falha no Redis mantém as consultas funcionando sem cache.

## Testes e implantação posterior

As [convenções de desenvolvimento](docs/BOAS_PRATICAS.md) descrevem responsabilidades, comentários, recursos e validação. Execute `./mvnw.cmd spotless:apply` para formatar o Java; `verify` confere automaticamente o padrão, sem modificar os arquivos.

A aceitação está em andamento. Consulte o [plano de testes de aceitação](docs/ACEITACAO.md) e a [matriz com resultados e pendências](docs/ACEITACAO_RESULTADOS.md). Após executar a suíte, `python scripts/Relatorio-Aceitacao.py` atualiza a rastreabilidade com os resultados reais. A medição local de desempenho não substitui a validação no navegador e na VPS.

`.\scripts\Testar-MariaDB.ps1` executa os testes em MariaDB isolado e gera `target/psa-gestor-api.jar`. Os testes cobrem autenticação, RBAC, titularidade, versionamento, fórmula, contrato PDF, reserva concorrente, pagamento idempotente, relatórios e comportamento dos adaptadores. A suíte padrão usa um servidor S3 HTTP local. Existe também um teste externo opt-in, já executado com sucesso no R2 real, descrito em `docs/R2.md`.

A VPS não foi acessada nesta implementação. Antes da mudança, execute as migrações e os testes em um MariaDB isolado da mesma versão, valide R2/Redis, faça backup e revise as variáveis do perfil `prod`. Não misture `prod` com `demonstracao`/`test`; a aplicação rejeita essa combinação. Consulte [docs/IMPLEMENTACAO.md](docs/IMPLEMENTACAO.md) para os detalhes e limites da implementação.


## Avisos de vencimento

A API classifica o vencimento dos contratos aprovados e permite ao administrador ajustar a antecedência global, inicialmente de 30 dias. Consulte [configuração e integração das telas](docs/ALERTAS_VENCIMENTO.md). A migração V7 prepara a configuração automaticamente na inicialização.
