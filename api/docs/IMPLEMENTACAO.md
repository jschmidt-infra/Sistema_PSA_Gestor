# Implementação da API — estado e decisões

Atualização: 17/10/2026. Esta página descreve o código implementado; não altera os documentos acadêmicos aprovados.

## Organização técnica

Os módulos preservam a divisão de responsabilidades: acesso, cadastros, diagnóstico/cálculo, contratos, financeiro, arquivos, cache e consultas. A persistência utiliza Spring Data JPA/Hibernate, com 21 entidades e um repositório tipado por entidade. O `RegistroRepository` foi removido e não há uso direto de `JdbcTemplate` no código de produção. As transações continuam coordenadas pelos serviços. O detalhamento está em [Persistência JPA](PERSISTENCIA_JPA.md).

`FinanceiroService` coordena aprovação e reserva. O serviço de contratos não chama o financeiro; o controller encaminha a operação. Os valores financeiros não dependem do Redis. Não há chamadas à VPS nos testes.

### Separação entre controllers, serviços e repositórios

Os oito controllers ficam nos pacotes `api` e tratam rotas, validação das entradas, permissões por perfil e respostas HTTP. Não executam SQL, não acessam repositórios de dados e não declaram transações. O controller de autenticação mantém a integração HTTP com a sessão e o contexto do Spring Security; a autenticação das credenciais é delegada ao serviço.

Os serviços concentram as regras de negócio, as verificações de titularidade, a validação de filtros, a coordenação de arquivos, o cache de indicadores e as transações. As consultas extraídas dos controllers ficam nos repositórios dos módulos de cadastros, diagnóstico, contratos, arquivos, financeiro, acesso e consultas. A administração de usuários também delega suas operações de persistência a um repositório próprio.

A refatoração preserva os contratos HTTP e não exige migração do banco. A separação dos controllers e a substituição da persistência SQL genérica por repositórios JPA por entidade estão implementadas. Consultas nativas parametrizadas permanecem encapsuladas nos repositórios para projeções de relatórios, conferência de inconsistências e operações atômicas específicas do MariaDB.

Os testes de arquitetura verificam a ausência de acesso direto à persistência nos controllers, de dependências de controllers ou respostas HTTP nos serviços e repositórios e do antigo repositório SQL genérico no código de produção. Os testes de integração utilizam MariaDB descartável e incluem mapeamentos JPA, relacionamentos, paginação, atualização de entidades após bloqueio, rollback e concorrência financeira.

Validação da migração JPA em 17/10/2026: 51 testes aprovados, sem falhas ou erros; dois testes opcionais de serviços externos não executados. Compilação e empacotamento concluídos com sucesso.

## Segurança

Cada controller declara as permissões por método/perfil. Administrador não é superusuário do negócio. Produtor acessa somente seus próprios contratos, propriedades, croquis, visitas e pagamentos; a identidade vem da sessão. O banco é consultado a cada requisição autenticada para verificar perfil, vínculo, estado ativo e versão de credenciais.

Não é permitido desativar o último administrador ativo. Vínculos de conta com produtor não são transferidos. Senhas usam BCrypt, mínimo de 12 caracteres e máximo de 72 bytes UTF-8. Dados sensíveis não são incluídos na auditoria ou nos logs de erros. Os eventos de negócio registram autor, recurso, operação, instante e identificador da requisição.

Novos logins são limitados por e-mail a cinco recusas consecutivas; a quinta inicia bloqueio de uma hora. O contador é persistido no MariaDB pela migração V5, com serialização de tentativas por bloqueio de linha e prazo calculado no relógio do banco. A resposta 429 inclui `Retry-After`. Sucesso zera tentativas; expiração libera automaticamente; tentativas durante o bloqueio não o prolongam. O administrador pode consultar e liberar antecipadamente, com auditoria `DESBLOQUEAR_LOGIN`. A auditoria de acesso é implementada separadamente pela migração V6 e pelo serviço de eventos; veja `docs/EVENTOS_ACESSO.md`.

## Parâmetros e diagnóstico

- Cada alteração de parâmetros cria outra versão; versões anteriores não são sobrescritas.
- `PENDENTE` permite configurar valor da terra e percentual sem inventar questionário. Nesse estado, `grupos` deve ser uma lista vazia e a apuração fica bloqueada.
- `DEMONSTRACAO_SOMA_PONDERADA` só pode ser cadastrada/apurada em `demonstracao`/`test`, sem `prod`. As perguntas têm identificadores próprios; pesos somam 1, alternativas têm valor entre 0 e 1 e respostas opcionais ausentes contribuem zero. Essa regra é demonstrativa, não uma metodologia oficial atribuída ao professor.
- O diagnóstico preserva a versão do questionário existente na criação. Alterar a configuração afeta novos diagnósticos. Não se reinterpretam respostas antigas usando um questionário diferente.
- Novo cálculo utiliza valor da terra e percentual da versão mais recente do projeto e preserva a memória da pontuação do diagnóstico. O cálculo registra as duas referências: versão monetária em `parametro_id` e versão de apuração dentro da memória JSON.
- Correção exige motivo, incrementa a revisão e invalida a pontuação. Cálculos anteriores permanecem imutáveis. Contrato ainda em rascunho precisa de cálculo da revisão concluída; contrato aprovado permanece com seus valores históricos.

## Contratos e arquivos

O contrato preliminar em PDF usa dados do produtor, propriedade, localidade, projeto, técnico, diagnóstico, cálculo e croqui. Partes institucionais e condições ainda não definidas aparecem explicitamente como pendentes, sem inventar obrigações. A geração registra a versão e os dados utilizados.

A aprovação compara os dados atuais com os dados da geração. Se houve alteração relevante, exige regenerar o PDF. Contratos aprovados não podem ser editados/regenerados; croquis associados a eles também ficam protegidos. A assinatura simulada é independente e não é pré-requisito presumido para a aprovação. O modelo definitivo e a metodologia oficial continuam sujeitos à validação.

Arquivos permitidos: PDF, PNG e JPEG; máximo de 10 MiB. Há validação de conteúdo por PDFBox/ImageIO, até 100 páginas para PDF e 25 milhões de pixels para imagens. PDF criptografado é rejeitado. Os downloads usam `Content-Disposition: attachment`, autorização e `Cache-Control: no-store`.

Antes do upload, o banco recebe uma entrada durável em `arquivo_pendente`, em transação independente. O cadastro de metadados ocorre na transação de negócio. Falha posterior deixa o objeto elegível para limpeza. A limpeza aguarda uma hora, verifica referências no banco e exclui apenas objetos sem referência. Arquivos substituídos entram no mesmo mecanismo. O processo exige uma única instância da API nesta implantação.

## Financeiro e concorrência

A ordem principal de bloqueio é projeto, contrato e compromisso. Na aprovação, também são protegidos os dados usados na verificação do documento. Operações financeiras usam isolamento `READ_COMMITTED` e bloqueio explícito de linhas, evitando que uma leitura anterior ao bloqueio use saldo desatualizado.

A aprovação reserva integralmente o valor anual inicial, desde que haja saldo. Contrato/exercício é único. O exercício inicial corresponde ao ano de início da vigência; não há rateio automático ou geração de exercícios posteriores sem uma regra definida pelo programa.

Pagamento exige compromisso de contrato aprovado e projeto correspondente. O valor é positivo e limitado ao saldo do compromisso. A chave de operação é globalmente única; o resumo criptográfico inclui autor e conteúdo normalizado. A verificação de reenvio antecede a verificação de quitação, permitindo repetir um pagamento que tenha quitado o compromisso. Auditoria e lançamento são confirmados juntos.

Relatórios totalizam todo o filtro, não apenas a página. Indicadores financeiros vêm do banco. Área protegida é a soma das áreas dos contratos vigentes; não representa união geoespacial de polígonos. A média é por registro de pagamento; quando não há pagamentos, retorna `null`.

A consulta de saldo atende à exceção E1 do UC08: confere aportes, vínculos, reservas, pagamentos e disponibilidade por projeto, retornando status de consolidação, aviso e identificadores dos registros envolvidos. Totais e conferência são lidos em uma transação `REPEATABLE_READ`, sem cache nem alterações nos dados. Havendo ocorrências, os totais são indicativos e a consolidação é marcada como incompleta. A verificação não altera os bloqueios e o isolamento das operações de aprovação e pagamento. O contrato HTTP e as regras verificadas estão em `docs/API.md`.

## Observabilidade

O Actuator disponibiliza métricas e detalhes de saúde somente para administradores autenticados. A liveness pública continua sem detalhes. Logs operacionais incluem o UUID da requisição, método, padrão de rota, status e duração, sem corpo, query string ou credenciais.

O estado do Redis é observado por operações de cache e sondagens administrativas limitadas. Falhas geram métricas e estado `DEGRADED`, sem retirar a API de prontidão; as consultas continuam pelo MariaDB. A recuperação também é sinalizada. O guia de endpoints, métricas, autenticação e logs está em [Observabilidade](OBSERVABILIDADE.md).

Validação em 17/10/2026: 54 testes aprovados, sem falhas ou erros; dois testes opcionais de serviços externos não executados. A falha do Redis foi simulada sem interromper o serviço local.

## Banco e execução local

V1 foi preservada. V2 acrescenta o fluxo de negócio, V3 o diário de arquivos e V4 o identificador de requisição da auditoria. Não edite migrações já aplicadas. O perfil `demonstracao` cria referências fictícias no MariaDB. O perfil `test` usa MariaDB isolado, preparado pelo script `scripts/Testar-MariaDB.ps1`. O banco MariaDB da VPS ainda não foi utilizado.

## Verificações e limites

Os testes automatizados exercitam HTTP, autenticação, transações, concorrência e os adaptadores. O servidor S3 do teste é local; Redis tem testes de gravação/TTL e indisponibilidade. A coleção Postman e `docs/API.md` apoiam a integração manual.

Ainda dependem de definição/homologação externa:

- Questionário, pesos e metodologia oficiais; texto contratual definitivo e regras de renovação.
- Migrações, collation, tipos e bloqueios na versão MariaDB da VPS. A validação local não substitui a homologação da infraestrutura da VPS.
- Fluxo integrado de arquivos pela VPS/Angular e Redis da infraestrutura. O adaptador S3 já foi validado no bucket R2 real em 16/10/2026, com envio, leitura, conferência de metadados e exclusão de objeto temporário; ver `docs/R2.md`.
- Fluxo real do Angular, proxy de mesma origem, HTTPS, backup/restauração e imagem Docker na VPS.

Esses limites não são ocultados pela metodologia demonstrativa. A implementação não realiza pagamentos reais, não gera assinatura digital e não faz integração bancária.

### Grupos do questionário

RF03/UC03: a configuração versionada contém grupos identificados por ID e nome, cada um com suas perguntas. A ordem dos arrays é preservada. A consulta `/api/v1/diagnosticos/{id}/questionario` fornece a configuração histórica fixada no diagnóstico. Pesos continuam por pergunta, sem multiplicador por grupo. Detalhes e exemplos em [API.md](API.md#grupos-de-indicadores-rf03--uc03).

A estrutura é persistida no JSON da entidade JPA `ParametroProjeto`; não exige uma nova tabela ou alteração do esquema. Versões antigas permanecem intactas e recebem uma representação de leitura explícita de perguntas históricas sem classificação. A implementação dos grupos está concluída; a definição do conteúdo e dos pesos oficiais permanece pendente.


## Alertas de vencimento — 18/10/2026

Configuração global persistida pela V7, padrão de 30 dias e manutenção exclusiva do administrador. Contratos aprovados recebem classificação dinâmica em America/Sao_Paulo nas consultas e no histórico do produtor, com filtro antes da paginação. Rascunhos não geram alertas. A API oferece as rotas necessárias à futura tela Angular. Não há alteração financeira nem renovação automática. Ver [guia de integração](ALERTAS_VENCIMENTO.md).
