# Persistência JPA

Atualização: 18/10/2026.

A API utiliza entidades Hibernate e repositórios Spring Data JPA por entidade, conforme a separação de persistência prevista no documento de arquitetura. O antigo `RegistroRepository`, que recebia SQL e nomes de tabelas, foi removido do código de produção.

## Entidades e repositórios

Cada entidade abaixo possui seu próprio `NomeDaEntidadeRepository`, no pacote `repository` do módulo correspondente.

| Módulo | Entidades |
| --- | --- |
| Acesso | Usuario, TentativaLogin, EventoAcesso, AuditoriaEvento |
| Cadastros | Produtor, Propriedade, FonteHidrica, Visita, Localidade, SubBacia |
| Configuração | Projeto, ParametroProjeto, ConfiguracaoVencimento |
| Diagnóstico | Diagnostico |
| Cálculo | CalculoPsa |
| Contratos | Contrato |
| Financeiro | Aporte, CompromissoAnual, Pagamento |
| Arquivos | Croqui, ArquivoPendente |

Os mapeamentos respeitam as tabelas, colunas, escalas decimais e identificadores existentes. As relações de negócio permitem navegação com carregamento sob demanda (`LAZY`), sem exclusões em cascata. Os identificadores das relações continuam sendo atribuídos explicitamente; as associações são mapeadas para leitura. Isso preserva os contratos HTTP e evita carregar grafos de entidades para montar as respostas.

As colunas `DATETIME` dos fluxos anteriormente atendidos por JDBC usam `LocalDateTime`, com leitura explícita desse tipo também nas projeções. A conversão de saída preserva a representação anterior, inclusive os microssegundos, sem reinterpretar os valores armazenados como instantes UTC. O mapeamento `Instant` de `Usuario`, que já utilizava JPA, foi mantido.

Entidades não são devolvidas diretamente pelos controllers. A conversão para as respostas existentes mantém os campos e a representação decimal usados pelo frontend. `Dados` é apenas um conversor de valores, sem acesso ao banco.

## Consultas e operações

- Inclusões e alterações de negócio usam entidades gerenciadas e os repositórios correspondentes.
- Listagens simples usam paginação e filtros JPA, executados no banco.
- Relatórios, extratos e a conferência financeira usam projeções nativas parametrizadas por meio do JPA. A conferência mantém os `LEFT JOIN` necessários para identificar referências ausentes, sem depender da navegação das entidades.
- O controle de tentativas de login mantém o upsert atômico, o bloqueio por e-mail e o relógio do MariaDB em consultas nativas declaradas no seu repositório JPA. Isso preserva a contagem concorrente e a duração de uma hora.
- O diário de arquivos mantém o registro em transação independente e seu upsert atômico. A limpeza utiliza repositórios de arquivos, croquis e contratos.
- A auditoria de negócio permanece na transação da operação; os eventos de acesso mantêm a transação independente e o tratamento de indisponibilidade.

`EntidadeRepository` é uma interface-base tipada e sem instância própria (`@NoRepositoryBean`), compartilhando apenas operações ORM. Não recebe SQL ou nomes de tabelas. `ProjecaoNativa` é um auxiliar protegido, sem bean próprio e sem operações de escrita, utilizado pelos repositórios de consultas para materializar projeções. O código SQL específico fica nesses repositórios, não nos serviços ou controllers.

## Transações e concorrência

A ordem de bloqueios financeiros permanece: projeto, contrato e compromisso, além dos registros necessários para validar o documento contratual. Aprovação e pagamento continuam com isolamento `READ_COMMITTED`; consolidações mantêm o isolamento definido pelos respectivos serviços.

O bloqueio de entidade utiliza `PESSIMISTIC_WRITE` com atualização do estado pelo banco. Essa atualização é necessária quando a entidade já foi carregada no contexto JPA antes de outra transação alterar o registro. As gravações são sincronizadas antes de gerar respostas ou snapshots, preservando as escalas e os valores efetivamente armazenados.

O contexto JPA não se estende à renderização HTTP (`open-in-view=false`). Os limites de transação continuam explícitos, e as atualizações dinâmicas evitam gravar colunas que não foram alteradas.

## Banco existente e execução

As migrações V1–V6 foram preservadas. A V7 acrescenta a configuração global de antecedência dos avisos de vencimento, inicializada em 30 dias. Não é necessário apagar tabelas ou recriar o banco. O Flyway continua responsável pelo esquema; o Hibernate permanece em `validate`.

Reinicie a API para carregar a implementação atualizada. Nenhum banco da VPS é utilizado na validação automatizada.

## Verificação

Resultado em 17/10/2026: compilação e empacotamento aprovados; 51 testes aprovados, sem falhas ou erros. Os dois testes opcionais contra Redis e R2 reais não foram executados nesta validação. O MariaDB utilizado pela suíte foi descartável.

A suíte executada por `scripts/Testar-MariaDB.ps1` cria um MariaDB descartável e verifica os fluxos HTTP, autenticação, auditoria, bloqueios, idempotência, consistência financeira, arquivos e rollback. Testes adicionais verificam os 21 mapeamentos, a navegação de relacionamentos, a compatibilidade dos valores lidos, a paginação e a atualização de uma entidade após alteração concorrente.

O acesso direto por `JdbcTemplate` permanece apenas nos testes, para preparar dados e inspecionar o resultado independentemente do JPA. Os testes de arquitetura impedem a reintrodução do repositório SQL genérico ou de `JdbcTemplate` no código de produção.
