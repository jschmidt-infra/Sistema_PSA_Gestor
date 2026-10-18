# Revisão das permissões das rotas

Base: Permissões dos perfis, revisão 0.4, alinhada à Especificação 2.1 e à Arquitetura 2.0; seção de rotas do contrato da API. Administrador não herda as permissões de gestor ou técnico.

Inventário: **61 combinações de método e rota de negócio**. A matriz de teste independente está em `src/test/resources/permissoes-rotas.json`.

| Método e rota | Administrador | Gestor | Técnico | Produtor |
|---|---|---|---|---|
| `GET /api/v1/configuracoes/vencimento-contratos` | Sim | Não | Não | Não |
| `PUT /api/v1/configuracoes/vencimento-contratos` | Sim | Não | Não | Não |
| `GET /api/v1/usuarios` | Sim | Não | Não | Não |
| `POST /api/v1/usuarios` | Sim | Não | Não | Não |
| `PUT /api/v1/usuarios/{id}` | Sim | Não | Não | Não |
| `PUT /api/v1/usuarios/{id}/senha` | Sim | Não | Não | Não |
| `GET /api/v1/usuarios/{id}/bloqueio-login` | Sim | Não | Não | Não |
| `DELETE /api/v1/usuarios/{id}/bloqueio-login` | Sim | Não | Não | Não |
| `GET /api/v1/produtores` | Não | Sim | Sim | Não |
| `POST /api/v1/produtores` | Não | Não | Sim | Não |
| `GET /api/v1/produtores/{id}` | Não | Sim | Sim | Sim |
| `PUT /api/v1/produtores/{id}` | Não | Sim | Sim | Não |
| `GET /api/v1/propriedades` | Não | Sim | Sim | Não |
| `POST /api/v1/propriedades` | Não | Não | Sim | Não |
| `GET /api/v1/propriedades/{id}` | Não | Sim | Sim | Sim |
| `PUT /api/v1/propriedades/{id}` | Não | Sim | Sim | Não |
| `GET /api/v1/propriedades/{id}/fontes-hidricas` | Não | Sim | Sim | Sim |
| `POST /api/v1/propriedades/{id}/fontes-hidricas` | Não | Não | Sim | Não |
| `PUT /api/v1/fontes-hidricas/{id}` | Não | Não | Sim | Não |
| `PUT /api/v1/propriedades/{id}/levantamento-hidrico` | Não | Não | Sim | Não |
| `GET /api/v1/propriedades/{id}/visitas` | Não | Sim | Sim | Sim |
| `POST /api/v1/propriedades/{id}/visitas` | Não | Não | Sim | Não |
| `PUT /api/v1/visitas/{id}` | Não | Não | Sim | Não |
| `GET /api/v1/localidades` | Sim | Sim | Sim | Não |
| `GET /api/v1/sub-bacias` | Sim | Sim | Sim | Não |
| `GET /api/v1/projetos` | Sim | Sim | Sim | Não |
| `GET /api/v1/projetos/{id}/parametros` | Sim | Sim | Sim | Não |
| `PUT /api/v1/projetos/{id}/parametros` | Sim | Não | Não | Não |
| `POST /api/v1/propriedades/{id}/diagnosticos` | Não | Não | Sim | Não |
| `GET /api/v1/propriedades/{id}/diagnosticos` | Não | Sim | Sim | Sim |
| `GET /api/v1/diagnosticos/{id}` | Não | Sim | Sim | Sim |
| `GET /api/v1/diagnosticos/{id}/questionario` | Não | Sim | Sim | Não |
| `PUT /api/v1/diagnosticos/{id}/respostas` | Não | Não | Sim | Não |
| `POST /api/v1/diagnosticos/{id}/concluir` | Não | Não | Sim | Não |
| `POST /api/v1/diagnosticos/{id}/reapurar` | Não | Não | Sim | Não |
| `POST /api/v1/diagnosticos/{id}/abrir-correcao` | Não | Não | Sim | Não |
| `POST /api/v1/calculos` | Não | Sim | Sim | Não |
| `GET /api/v1/calculos/{id}` | Não | Sim | Sim | Sim |
| `GET /api/v1/propriedades/{id}/croquis` | Não | Sim | Sim | Sim |
| `POST /api/v1/propriedades/{id}/croquis` | Não | Não | Sim | Não |
| `PUT /api/v1/croquis/{id}` | Não | Não | Sim | Não |
| `GET /api/v1/croquis/{id}/arquivo` | Não | Sim | Sim | Sim |
| `GET /api/v1/contratos` | Não | Sim | Sim | Não |
| `POST /api/v1/contratos` | Não | Sim | Não | Não |
| `GET /api/v1/contratos/{id}` | Não | Sim | Sim | Sim |
| `PUT /api/v1/contratos/{id}` | Não | Sim | Não | Não |
| `POST /api/v1/contratos/{id}/gerar-documento` | Não | Sim | Não | Não |
| `POST /api/v1/contratos/{id}/assinatura-simulada` | Não | Sim | Não | Não |
| `POST /api/v1/contratos/{id}/aprovar` | Não | Sim | Não | Não |
| `GET /api/v1/contratos/{id}/documento` | Não | Sim | Sim | Sim |
| `GET /api/v1/contratos/{id}/compromissos` | Não | Sim | Não | Não |
| `POST /api/v1/projetos/{id}/aportes` | Não | Sim | Não | Não |
| `GET /api/v1/projetos/{id}/aportes` | Não | Sim | Não | Não |
| `GET /api/v1/financeiro/saldo` | Não | Sim | Não | Não |
| `POST /api/v1/pagamentos` | Não | Sim | Não | Não |
| `GET /api/v1/pagamentos/{id}` | Não | Sim | Não | Sim |
| `GET /api/v1/pagamentos` | Não | Sim | Não | Não |
| `GET /api/v1/relatorios/aplicacao-recursos` | Não | Sim | Não | Não |
| `GET /api/v1/indicadores` | Não | Sim | Não | Não |
| `GET /api/v1/meu-historico` | Não | Não | Não | Sim |
| `GET /api/v1/meu-historico/pagamentos` | Não | Não | Não | Sim |

“Sim” para produtor exige titularidade. Rotas `/meu-historico` usam exclusivamente o vínculo da sessão. O questionário administrativo não é disponibilizado ao produtor.

## Correção aplicada

A leitura de diagnóstico, lista de diagnósticos da propriedade e cálculo estava restrita a gestor/técnico, embora a matriz funcional previsse consulta do próprio resultado pelo produtor. Foi incluída a leitura pelo titular, com verificação do encadeamento cálculo → diagnóstico → propriedade → produtor. Não foram concedidas operações de escrita ou consulta de outro beneficiário. IDs alheios retornam 404.

## Autenticação, documentação e operação

| Rota | Regra |
|---|---|
| `GET /api/v1/auth/csrf` | Pública |
| `POST /api/v1/auth/login` | Pública com CSRF e credenciais válidas; limitação de tentativas |
| `GET /api/v1/auth/me` | Todos os perfis autenticados |
| `PUT /api/v1/auth/senha` | Todos os perfis, somente própria senha com confirmação da atual e CSRF |
| `POST /api/v1/auth/logout` | Encerra a sessão corrente; CSRF obrigatório; chamada sem sessão é idempotente |
| Swagger e `/v3/api-docs/**` | Administrador |
| `GET /actuator/health/liveness` | Público, sem detalhes privados |
| Demais consultas de health e metrics expostas | Administrador |
| Endpoints operacionais não liberados, como `/actuator/env` | Negados |

## Evidências e alcance

- `matrizDeTodasAsRotasValidaPerfisERecusaAnonimos`: 305 verificações HTTP, cobrindo cada rota de negócio com quatro perfis e anônimo. CSRF e entradas válidas permitem chegar à autorização. Perfis sem acesso recebem 403; anônimos recebem 401. Para perfis autorizados, IDs inexistentes podem retornar 404, sem confundir existência com permissão. Sucesso funcional e regras de negócio são verificados pelos demais cenários.
- `produtorSoConsultaSeusRecursosMesmoAlterandoIdsEFiltros`: dois produtores, 12 caminhos de consulta por titular, leitura própria 200 e troca de titular 404, nos dois sentidos, incluindo PDFs e croquis. Histórico e extrato ignoram a tentativa de selecionar outro produtor.
- `rotasDeAutenticacaoDocumentacaoEMetricasRespeitamPerfis`: login real, consulta da sessão, logout e troca da própria senha para os quatro perfis; Swagger, OpenAPI e métricas reservados ao administrador.
- Os testes anteriores cobrem conta desativada, perfil/senha alterados, CSRF e bloqueio temporário de login.

Resultados em [ACEITACAO_RESULTADOS.md](ACEITACAO_RESULTADOS.md). A execução usa MariaDB descartável e armazenamento local para os arquivos de teste; não certifica a configuração de rede, R2 ou proxy da VPS. O limite de campos que o gestor pode corrigir nos cadastros continua conforme a implementação atual; a extensão exata permanece uma definição funcional do grupo, conforme a matriz de perfis.
