# Redis local — configuração e teste

A API no Windows acessa o Redis do Docker Desktop por `127.0.0.1:6379`. A conexão usa a senha configurada na stack do Portainer. O endereço `172.30.50.133` é interno à rede Docker e não é usado pela API no Windows.

O arquivo privado `config/application-local.yml` contém `spring.data.redis.host`, `port`, `password` e `app.cache.enabled: true`. Ele está ignorado pelo Git. O modelo sem segredos está em `config/application-local.example.yml`.

## Iniciar

Mantenha o Docker Desktop e o container Redis em execução e inicie a API normalmente:

```powershell
.\scripts\Iniciar-ApiLocal.ps1
```

Se a API já estiver aberta, encerre com Ctrl+C e inicie novamente para carregar a configuração.

## O que utiliza cache

O endpoint `GET /api/v1/indicadores`, disponível ao perfil `GESTOR`, armazena no Redis somente quantidade de produtores participantes, área protegida e instante de apuração desses indicadores. Cada chave separa usuário, perfil, projeto, sub-bacia e data de referência.

O prazo de expiração é de 60 segundos. Alterações nesses indicadores podem levar até esse prazo para aparecer. Consultas repetidas com os mesmos filtros podem manter `apuradoEm` enquanto o cache estiver válido. Os valores financeiros são consultados no MariaDB em cada requisição: saldos, reservas e pagamentos não usam esse cache. Sessões e permissões também não são armazenadas nesse cache.

Quando o Redis falha, a consulta continua pelo MariaDB; o adaptador suspende tentativas de cache por 30 segundos. A prontidão da API não depende do Redis, pois ele é opcional. Uma API iniciada não comprova, sozinha, uma conexão Redis bem-sucedida.

## Teste real do adaptador

Na raiz do projeto:

```powershell
$env:PSA_REDIS_LIVE_TEST = 'true'
try {
    .\mvnw.cmd '-Dmaven.repo.local=tmp/api-bootstrap/repository' '-Dtest=RedisLiveTest,RedisIndicadoresTest' test
} finally {
    Remove-Item Env:PSA_REDIS_LIVE_TEST -ErrorAction SilentlyContinue
}
```

O teste real autentica, verifica `PONG`, seleciona o adaptador Redis, grava e lê indicadores e verifica o TTL de até 60 segundos. Usa uma chave aleatória em `psa:testes:redis:` e remove somente essa chave ao terminar. Não limpa o Redis e não acessa o MariaDB. Os testes unitários verificam também o comportamento de fallback quando o Redis está indisponível. Sem a variável de ativação, o teste real é ignorado.

Não compartilhe o arquivo privado nem coloque a senha no frontend. Para restringir o acesso ao próprio Windows, a publicação da stack pode ser `127.0.0.1:6379:6379`.
