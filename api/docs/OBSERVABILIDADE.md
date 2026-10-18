# Observabilidade da API

Atualização: 17/10/2026.

## Acesso administrativo

As métricas e os detalhes de saúde utilizam a mesma sessão da API. Autentique-se como **ADMINISTRADOR**, seguindo [o guia de autenticação](AUTENTICACAO_FRONTEND.md), e preserve o cookie. No navegador, faça o login no mesmo navegador e na mesma origem utilizada para abrir os endpoints. Estas consultas são GET e não exigem o envio de CSRF.

| Endpoint | Acesso e finalidade |
| --- | --- |
| `GET /actuator/metrics` | Administrador; nomes das métricas disponíveis |
| `GET /actuator/metrics/jvm.memory.used` | Administrador; memória utilizada pela JVM |
| `GET /actuator/metrics/http.server.requests` | Administrador; quantidade e duração das requisições observadas |
| `GET /actuator/metrics/psa.cache.redis.disponivel` | Administrador; último estado observado do cache |
| `GET /actuator/health` | Administrador; saúde agregada e detalhes dos componentes |
| `GET /actuator/health/cacheRedis` | Administrador; habilitação, disponibilidade e última verificação do cache |
| `GET /actuator/health/readiness` | Administrador; prontidão da aplicação e disponibilidade do banco |
| `GET /actuator/health/liveness` | Público; somente estado de execução, sem detalhes internos |

Exemplo local: `http://127.0.0.1:8080/actuator/metrics`.

Sem autenticação, os endpoints administrativos retornam 401. Os demais perfis não possuem essa permissão e recebem 403. Endpoints como `env`, `configprops`, `heapdump` e `loggers` não foram expostos. As métricas não são dados públicos para o frontend do produtor ou gestor.

A proteção implementada é por autenticação e perfil na API. Na publicação, mantenha a porta da aplicação privada e aplique também as restrições de rede/proxy previstas para o ambiente. Este trabalho não altera a configuração de uma VPS remota.

## Identificador da requisição e logs

Cada requisição recebe um UUID gerado pela API, devolvido no cabeçalho `X-Request-ID` e incluído no contexto dos logs como `requisicaoId`. Um identificador enviado pelo cliente não substitui o gerado pelo servidor.

O log de conclusão contém método, padrão da rota, status HTTP e duração em milissegundos. Usa o padrão `/api/v1/propriedades/{id}`, por exemplo, sem registrar o identificador concreto do caminho. Requisições bloqueadas antes do roteamento aparecem como `NAO_MAPEADA`.

Não são registrados corpo, query string, senhas, cookies, cabeçalhos de autorização ou conteúdo dos arquivos nesse log operacional. O contexto é removido ao terminar a requisição, evitando atribuir o mesmo identificador a outra chamada. Inicialização, tarefas agendadas e outros eventos sem requisição apresentam `requisicaoId=sem-requisicao`.

Exemplo ilustrativo:

```text
INFO [requisicaoId=...] ... requisicao_http metodo=GET rota=/api/v1/indicadores status=200 duracaoMs=12
WARN [requisicaoId=...] ... cache_redis estado=DEGRADADO fallback=MariaDB novaTentativaSegundos=30
INFO [requisicaoId=...] ... cache_redis estado=RECUPERADO
```

A auditoria de negócio e os [eventos de acesso](EVENTOS_ACESSO.md) continuam separados dos logs operacionais.

## Redis opcional e degradação

O Redis mantém somente cópias temporárias de indicadores não financeiros. Em falha de leitura ou escrita, o erro é tratado e as consultas seguem pelo MariaDB. O timeout configurado para conexão/comandos permanece em 200 ms. Após uma falha, novas operações de cache são suspensas por 30 segundos; a escrita é ignorada quando a leitura já identificou indisponibilidade.

A consulta administrativa de saúde também pode detectar indisponibilidade antes do primeiro uso do cache, por meio de `PING`. Essa sondagem respeita a suspensão e é limitada a uma tentativa a cada 30 segundos. Não há uma tarefa de sondagem contínua: a nova verificação ocorre por uso do cache ou consulta de saúde após o intervalo.

Durante a indisponibilidade, o componente `cacheRedis` e a saúde agregada sinalizam `DEGRADED`, com HTTP 200. A prontidão não depende do Redis; a API continua atendendo. Falhas de componentes obrigatórios, como o banco, continuam resultando em `DOWN` e HTTP 503 na avaliação correspondente.

Uma operação ou sondagem bem-sucedida atualiza o estado para disponível e registra a recuperação. Os logs de degradação e recuperação são emitidos na mudança de estado, sem chaves do cache, valores ou detalhes de exceções. A resposta de saúde informa os horários da última verificação e da última falha. Trata-se do último estado observado, não de uma garantia de disponibilidade futura.

Com `CACHE_ENABLED=false`, o cache é indicado como `DESATIVADO` e não é sondado.

## Métricas do cache

| Nome | Significado |
| --- | --- |
| `psa.cache.redis.habilitado` | 1 quando configurado; 0 quando desativado |
| `psa.cache.redis.disponivel` | 1 após sucesso; 0 após falha; -1 antes de uma verificação ou quando desativado |
| `psa.cache.redis.operacoes` | Contador por `operacao` e `resultado` |

As operações são `leitura`, `gravacao` e `sondagem`. Os resultados incluem `acerto`, `ausente`, `sucesso`, `falha` e `ignorada`, conforme a operação. Não há tags com usuários, e-mails, identificadores de requisição ou chaves do Redis.

Exemplo de consulta do contador de falhas de leitura:

```text
GET /actuator/metrics/psa.cache.redis.operacoes?tag=operacao:leitura&tag=resultado:falha
```

Contadores e estado operacional são locais à instância e reiniciam com a API. Não foi instalado um coletor externo nem armazenamento histórico de métricas.

## Logs na implantação Docker

A API escreve os logs no console, acessível pelos logs do contêiner no Portainer. Para aplicar a rotação prevista na arquitetura, use esta configuração no serviço da API na stack:

```yaml
logging:
  driver: json-file
  options:
    max-size: "10m"
    max-file: "3"
```

A configuração de rotação deve ser aplicada na implantação; não é uma configuração do MariaDB ou do Redis e não exige alteração de tabelas.

## Validação

Em 17/10/2026, a suíte terminou com **54 testes aprovados**, sem falhas ou erros, e dois testes opcionais de Redis/R2 reais não executados. Foram verificados acesso administrativo às métricas, recusa de acessos sem permissão, correlação dos logs, ausência dos dados sensíveis de teste, degradação, prontidão preservada, continuidade das consultas e recuperação após o intervalo.

A indisponibilidade do Redis foi simulada no adaptador, sem interromper o Redis local do usuário. Os fluxos HTTP utilizaram MariaDB descartável. Compilação e empacotamento concluídos com sucesso.
