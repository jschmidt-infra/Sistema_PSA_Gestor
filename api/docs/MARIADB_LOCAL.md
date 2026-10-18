# MariaDB local no Docker Desktop

A API executa diretamente no Windows e utiliza o container `mariadb` do Portainer, imagem `mariadb:13.0.2`, banco `psa_gestor` e usuário `psa_app`. A conexão é `jdbc:mariadb://127.0.0.1:3306/psa_gestor`.

A stack publica `3306:3306`. Para permitir acesso somente pelo próprio computador, pode-se restringir para `127.0.0.1:3306:3306`. O endereço interno `172.30.50.130` continua sendo utilizado pela rede Docker, mas não é necessário para a API no Windows.

## Configuração e execução

Requisitos: Docker Desktop ativo, stack MariaDB iniciada e rede `psa_net` existente. As credenciais do banco ficam em `config/application-local.yml`, ignorado pelo Git; o modelo compartilhável é `config/application-local.example.yml`. O arquivo privado do R2 continua em `config/application-r2-local.yml`. O Spring Boot lê os arquivos privados a partir da pasta do projeto; eles não entram no JAR nem na imagem.

Na raiz do projeto, execute no PowerShell:

```powershell
# Testa em um MariaDB descartável e gera target/psa-gestor-api.jar.
.\scripts\Testar-MariaDB.ps1

# Primeira execução: solicita nome, e-mail e senha do administrador.
.\scripts\Iniciar-ApiLocal.ps1 -CriarAdministrador

# Execuções seguintes:
.\scripts\Iniciar-ApiLocal.ps1
```

O provisionamento não altera a senha de uma conta já existente. A senha deve conter pelo menos 12 caracteres e no máximo 72 bytes UTF-8. Depois de criar a conta, encerre com Ctrl+C e execute novamente sem `-CriarAdministrador`. O script remove a senha de provisionamento do ambiente ao encerrar.

- API: `http://127.0.0.1:8080`
- Prontidão: `http://127.0.0.1:8080/actuator/health/readiness`, exige sessão de administrador.
- Verificação pública de execução: `http://127.0.0.1:8080/actuator/health/liveness`
- Swagger: `http://127.0.0.1:8080/swagger-ui/index.html`, depois de autenticar no navegador como administrador.

Os logs aparecem no terminal. Para encerrar a API, pressione Ctrl+C. O banco e seu volume continuam sob gerenciamento do Portainer. Reiniciar ou reconstruir a API preserva os dados do MariaDB, mas encerra as sessões HTTP.

## Dados e perfis

O banco H2 anterior foi descontinuado; suas contas não são migradas. Crie o administrador no MariaDB. O Flyway cria e versiona as tabelas; o Hibernate somente valida o esquema.

- `local`: HTTP local, Swagger e migrações habilitados.
- `demonstracao`: referências fictícias e metodologia demonstrativa, enquanto questionário e pesos oficiais estão pendentes.
- `r2,r2-local`: arquivos privados no Cloudflare R2; referências no MariaDB.
- `test`: somente testes automatizados, com arquivos locais temporários e banco isolado.

O Redis local está ativado em `127.0.0.1:6379`, com senha no arquivo privado `config/application-local.yml`. Nenhum serviço da VPS é acessado. Consulte [Redis local](REDIS_LOCAL.md) para testar e entender o cache.

## Isolamento dos testes

`Testar-MariaDB.ps1` cria um container temporário MariaDB 13.0.2, com porta aleatória restrita ao Windows local, senha aleatória e dois bancos exclusivos: `psa_auth_test` e `psa_fluxo_test`. Ao terminar, remove esse container e seu volume anônimo. Os testes não usam o banco `psa_gestor` nem as credenciais da aplicação.

O Maven executado diretamente precisa das variáveis `TEST_DB_URL`, `TEST_FLOW_DB_URL`, `TEST_DB_USERNAME` e `TEST_DB_PASSWORD`. Não aponte essas variáveis para um banco com dados que devam ser preservados: os testes de integração limpam as tabelas de suas próprias bases.

## Portainer e futura VPS

O Portainer gerencia o MariaDB local; a API é iniciada pelo script PowerShell ou pela configuração Java do VS Code. Na futura VPS, a API poderá ser empacotada com o Dockerfile existente. Utilize o perfil `prod`, HTTPS e as configurações de proxy; o perfil local não é configuração de produção.

## Validação realizada

As quatro migrações foram aplicadas ao banco local `psa_gestor` e a API iniciou com sucesso. A suíte automatizada executou 30 testes com sucesso em MariaDB isolado; o teste externo opcional do R2 não foi repetido nesta alteração.

O Flyway incluído no projeto emite um aviso de que MariaDB 13.0 é mais recente que sua última versão verificada (11.7). As migrações e os testes passaram neste ambiente; isso não equivale a uma certificação geral de compatibilidade pelo fornecedor.
