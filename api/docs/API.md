# Contrato de integração — API implementada

Base: `/api/v1`. Autenticação por cookie de sessão e token CSRF. Datas: `YYYY-MM-DD`; instantes de saída: ISO 8601 UTC. Valores decimais de saída são strings. Coleções usam `{items, page, size, totalItems}`, página inicial 0 e tamanho entre 1 e 100.

A = administrador; G = gestor; T = técnico; P = produtor, sempre limitado aos próprios registros. Contas são criadas somente por A.

## Autenticação: leitura obrigatória antes de integrar o frontend

O passo a passo completo está em [Autenticação e integração com Angular](AUTENTICACAO_FRONTEND.md), incluindo criação do administrador, login, exemplos Angular, proxy local, Swagger e diagnóstico de erros.

**Login não cria uma conta.** O administrador inicial é criado ao iniciar o backend com as variáveis de provisionamento; depois ele cria as demais contas. Não há usuário/senha padrão, cadastro público, JWT ou autenticação Basic.

O frontend deve seguir esta sequência, preservando os cookies entre todas as chamadas:

1. `GET /api/v1/auth/csrf`: obter `{token, headerName}`.
2. `POST /api/v1/auth/login`: enviar `{email, senha}`, mais o cabeçalho cujo nome e valor vieram do passo 1.
3. Após receber 200, executar **outro** `GET /api/v1/auth/csrf`: o login invalida o token anterior e troca o identificador da sessão.
4. Enviar o CSRF atualizado nas operações de gravação, inclusive logout, alteração de senha e upload. O navegador envia o cookie `PSA_SESSION` automaticamente.
5. Ao recarregar o frontend, consultar `GET /api/v1/auth/me` para verificar a sessão. Não confiar apenas em dados salvos no navegador.

Use URLs relativas com proxy (`/api/v1/...`). Não tente ler `PSA_SESSION` pelo JavaScript: ele é `HttpOnly`. A proteção XSRF padrão do Angular, sozinha, não integra este backend, pois o token vem do endpoint JSON, e não de um cookie `XSRF-TOKEN`.

Exemplo de resposta de login ou `/auth/me`:

```json
{"id":1,"nome":"Técnico de demonstração","email":"tecnico@example.test","perfil":"TECNICO","ativo":true,"produtorId":null}
```

O perfil deve orientar as telas, mas a API continua sendo responsável por autorizar cada operação. O administrador não possui automaticamente as permissões de gestor ou técnico.

## Rotas

| Rota | Método | Perfil |
| --- | --- | --- |
| `/auth/csrf` | GET | Público |
| `/auth/login` | POST | Público, com CSRF |
| `/auth/me` | GET | Autenticado |
| `/auth/senha` | PUT | Autenticado |
| `/auth/logout` | POST | Autenticado |
| `/usuarios` | GET, POST | A |
| `/usuarios/{id}` | PUT | A |
| `/usuarios/{id}/senha` | PUT | A |
| `/produtores` | GET / POST | G,T / T |
| `/produtores/{id}` | GET / PUT | G,T,P / G,T |
| `/propriedades` | GET / POST | G,T / T |
| `/propriedades/{id}` | GET / PUT | G,T,P / G,T |
| `/localidades`, `/sub-bacias`, `/projetos` | GET | A,G,T |
| `/propriedades/{id}/fontes-hidricas` | GET / POST | G,T,P / T |
| `/fontes-hidricas/{id}` | PUT | T |
| `/propriedades/{id}/levantamento-hidrico` | PUT | T |
| `/propriedades/{id}/visitas` | GET / POST | G,T,P / T |
| `/visitas/{id}` | PUT | T |
| `/projetos/{id}/parametros` | GET / PUT | A,G,T / A |
| `/propriedades/{id}/diagnosticos` | GET / POST | G,T,P / T |
| `/diagnosticos/{id}` | GET | G,T,P |
| `/diagnosticos/{id}/respostas` | PUT | T |
| `/diagnosticos/{id}/concluir`, `/reapurar`, `/abrir-correcao` | POST | T |
| `/diagnosticos/{id}/questionario` | GET | G,T |
| `/calculos` | POST | G,T |
| `/calculos/{id}` | GET | G,T,P |
| `/propriedades/{id}/croquis` | GET / POST | G,T,P / T |
| `/croquis/{id}` | PUT | T |
| `/croquis/{id}/arquivo` | GET | G,T,P |
| `/contratos` | GET / POST | G,T / G |
| `/contratos/{id}` | GET / PUT | G,T,P / G |
| `/contratos/{id}/gerar-documento`, `/assinatura-simulada`, `/aprovar` | POST | G |
| `/contratos/{id}/documento` | GET | G,T,P |
| `/contratos/{id}/compromissos` | GET | G |
| `/projetos/{id}/aportes` | GET, POST | G |
| `/financeiro/saldo` | GET | G |
| `/usuarios/{id}/bloqueio-login` | GET, DELETE | A |
| `/pagamentos` | GET, POST | G |
| `/pagamentos/{id}` | GET | G,P |
| `/relatorios/aplicacao-recursos`, `/indicadores` | GET | G |
| `/meu-historico`, `/meu-historico/pagamentos` | GET | P |

Listagens aceitam `page` e `size`, exceto o agregado `/indicadores`. Filtros adicionais: propriedades por `produtorId`; contratos por `projetoId`, `status`, `vencimentoAte`; pagamentos por `projetoId`, `contratoId`, `dataInicio`, `dataFim`, `chaveOperacao`; saldo por `projetoId`; relatório por `projetoId`, `subBaciaId`, `dataInicio`, `dataFim`; indicadores pelos mesmos filtros e `dataReferencia`.

Os dados de referência são carregados por processo controlado; o MVP não expõe CRUD público para localidades, sub-bacias ou projetos. No `demonstracao`, as referências fictícias são criadas na primeira execução. O endpoint para criar exercícios adicionais não está liberado enquanto a regra de renovação estiver pendente.

## Sequência para demonstrar

1. Entrar como administrador e criar contas de gestor e técnico.
2. Consultar o projeto de demonstração e configurar os parâmetros abaixo.
3. Entrar como técnico e cadastrar produtor, propriedade e levantamento hídrico.
4. Criar diagnóstico, consultar seu questionário por grupos, salvar respostas e concluir.
5. Calcular o incentivo e enviar o croqui da mesma área.
6. Entrar como gestor, registrar saldo inicial, criar contrato e gerar o PDF.
7. Aprovar no ano inicial da vigência. Consultar o compromisso criado.
8. Registrar pagamentos parciais e consultar saldo e relatório.
9. Criar conta vinculada ao produtor e verificar o histórico restrito.

## Exemplos de entrada

Use IDs retornados pela API. Os números abaixo são ilustrativos, não identificadores garantidos.

### Criar usuário

```json
{"nome":"Técnico de demonstração","email":"tecnico@example.test","perfil":"TECNICO","ativo":true,"senha":"defina-uma-senha-privada"}
```

Para `PRODUTOR`, acrescente `produtorId`; para os demais perfis, omita o campo. PUT `/usuarios/{id}` recebe os mesmos campos, sem senha. A redefinição de senha recebe `{"novaSenha":"..."}`. Senha própria: consulte o schema de `/auth/senha` na coleção/OpenAPI.

### Produtor e propriedade

```json
{"nome":"Produtor de demonstração","documento":"12345678901","endereco":"Endereço de demonstração","telefone":"51999999999"}
```

```json
{"nome":"Propriedade de demonstração","produtorId":1,"localidadeId":1,"areaTotal":"10.0000","carSituacao":"NAO_POSSUI"}
```

O documento é validado quanto ao formato de 11/14 dígitos; o número fictício do exemplo não comprova CPF/CNPJ válido. Documento e endereço podem faltar no cadastro inicial, mas são obrigatórios para formalizar o contrato. Telefone ou e-mail é obrigatório. CAR admite `POSSUI`, `NAO_POSSUI` e `NAO_SE_APLICA`; quando `POSSUI`, informe `carNumero`.

### Parâmetros de demonstração

```json
{
  "valorHectare":"20000.00",
  "percentual":"0.04",
  "metodologia":"DEMONSTRACAO_SOMA_PONDERADA",
  "grupos":[{
    "id":"conservacao_solo","nome":"Conservação do solo",
    "perguntas":[{
    "id":"p1","texto":"Pergunta demonstrativa","obrigatoria":true,"peso":"1.000000",
    "alternativas":[{"id":"a1","texto":"Atende","valor":"1.000000"},{"id":"a2","texto":"Não atende","valor":"0.000000"}]
    }]
  }]
}
```

Apenas `demonstracao`/`test` aceitam essa metodologia. Para cadastrar somente valores financeiros enquanto se aguarda a metodologia: `{"valorHectare":"20000.00","percentual":"0.04","metodologia":"PENDENTE","grupos":[]}`. A apuração fica bloqueada. O GET retorna a versão e `configuracaoJson`, contendo a configuração serializada; respostas e memória do diagnóstico também são preservadas em campos JSON serializados.

### Grupos de indicadores (RF03 / UC03)

Novas versões usam `grupos`, com `id`, `nome` e `perguntas` em cada grupo. A ordem dos arrays define a apresentação dos grupos e das perguntas. Os grupos são configuráveis; o Documento de Visão cita conservação do solo, recursos hídricos, produção e gestão da propriedade. O exemplo acima é demonstrativo, não define o questionário oficial.

- Somente o administrador configura as versões em `PUT /api/v1/projetos/{id}/parametros`.
- IDs de grupos são únicos na versão; IDs de perguntas são únicos em todo o questionário, inclusive entre grupos. IDs de alternativas são únicos dentro de cada pergunta.
- Grupos devem ter nome e pelo menos uma pergunta. Limite de 100 perguntas no total e 20 alternativas por pergunta. Elementos nulos são rejeitados.
- Pesos continuam associados às perguntas; os grupos não introduzem outro peso nem alteram a fórmula do PSA. Na demonstração, a soma dos pesos de todas as perguntas deve ser 1.
- `PENDENTE` exige `grupos: []`; perguntas e pesos oficiais ainda precisam ser definidos.

Para montar o formulário de uma avaliação, o frontend deve consultar **`GET /api/v1/diagnosticos/{id}/questionario`**, com sessão de técnico ou gestor. Retorno: `{"parametroId":1,"configuracao":{"valorHectare":20000.00,"percentual":0.04,"metodologia":"DEMONSTRACAO_SOMA_PONDERADA","grupos":[...]}}`. `configuracao` é um objeto JSON, sem necessidade de desserializar uma string. Esse endpoint entrega a versão fixada ao criar o diagnóstico, mesmo se o administrador já cadastrou outra versão no projeto. Para a configuração atual do projeto, o GET de parâmetros continua retornando `configuracaoJson` serializado.

Versões históricas com lista plana são lidas em um grupo de compatibilidade `LEGADO_SEM_GRUPO`, chamado “Perguntas históricas sem classificação”. Isso não reclassifica indicadores nem modifica os registros antigos. Esse ID é reservado: ao criar uma nova versão, o administrador deve classificar as perguntas em grupos. Novas configurações sem `grupos` são rejeitadas. A memória de novas apurações também registra os grupos da versão utilizada.

### Diagnóstico e cálculo

Criar diagnóstico: `{"projetoId":1,"dataVisita":"2026-10-16"}`.

Salvar respostas: `{"respostas":[{"perguntaId":"p1","alternativaId":"a1"}]}`. A lista substitui atomicamente o conjunto salvo; envie também as respostas que deseja manter. Concluir e reapurar não exigem corpo. Abrir correção: `{"motivo":"Descrição da correção"}`.

Criar cálculo: `{"diagnosticoId":1,"areaContratada":"1.0000"}`. Com os parâmetros demonstrativos acima e pontuação 1, o resultado é R$ 800,00 anuais. A área contratada não pode exceder a área total.

### Arquivos, contrato e aprovação

Croqui: `multipart/form-data` com `arquivo` (PDF, PNG ou JPEG), `areaPsa` e, opcionalmente, `observacoes`. A atualização usa os mesmos campos. Não envie JSON/base64 nem URLs de arquivo.

Contrato: `{"projetoId":1,"calculoId":1,"croquiId":1,"inicio":"2026-10-16","fim":"2027-10-15"}`.

Gerar documento: POST sem corpo. Download: GET `/contratos/{id}/documento`. Assinatura simulada: `{"dataRegistro":"2026-10-16"}`. Aprovar: `{"exercicio":2026}`. A assinatura não é condição automática para aprovar.

### Recursos e pagamentos

Aporte: `{"tipo":"SALDO_INICIAL","dataAporte":"2026-10-16","valor":"1000.00","descricao":"Recursos simulados"}`. Após o primeiro saldo inicial, utilize `tipo: "APORTE"`.

### Conferência da consolidação financeira — UC08

`GET /api/v1/financeiro/saldo?projetoId=1` é exclusivo do gestor. O filtro é opcional: sem ele, a consulta é global. Um projeto inexistente retorna 404. Os cinco valores monetários existentes continuam sendo strings; a resposta acrescenta o resultado da conferência:

```json
{
  "recursos": "1000.00",
  "totalPago": "900.00",
  "saldoCaixa": "100.00",
  "saldoComprometido": "-100.00",
  "saldoDisponivel": "200.00",
  "consolidacaoCompleta": false,
  "statusConsolidacao": "INCOMPLETA",
  "mensagem": "Consolidação financeira incompleta. Os totais são indicativos; revise os registros identificados antes de utilizá-los.",
  "quantidadeInconsistencias": 1,
  "inconsistencias": [
    {
      "codigo": "PAGAMENTOS_EXCEDEM_COMPROMISSO",
      "mensagem": "A soma dos pagamentos excede o compromisso; revisar os pagamentos identificados.",
      "recurso": "pagamento",
      "registroId": 12,
      "projetoId": 1,
      "contratoId": 3,
      "compromissoId": 4
    }
  ]
}
```

O exemplo representa um compromisso de 800 e um pagamento inconsistente de 900. Uma consulta concluída retorna HTTP 200 mesmo quando identifica inconsistências. O frontend deve verificar `consolidacaoCompleta` e apresentar o aviso e os registros; não deve exibir esses totais como uma consolidação validada. Uma falha de acesso ao banco continua retornando 503, sem substituir resultados por zeros.

| Código | Conferência / registro indicado |
| --- | --- |
| `APORTE_INVALIDO` | Valor não positivo, descrição/origem ausente ou projeto inexistente; identifica o aporte. |
| `SALDO_INICIAL_DUPLICADO` | Mais de um saldo inicial no mesmo projeto; identifica cada lançamento. |
| `CONTRATO_VINCULO_INVALIDO` | Contrato aprovado com projeto, produtor, propriedade, diagnóstico ou cálculo ausente/incompatível; identifica o contrato. |
| `CONTRATO_SEM_COMPROMISSO` | Contrato aprovado sem reserva do exercício inicial; identifica o contrato. |
| `COMPROMISSO_INVALIDO` | Contrato ausente/não aprovado, valor inválido/divergente do cálculo ou exercício diferente do inicial; identifica o compromisso. |
| `PAGAMENTO_INVALIDO` | Valor não positivo, referência vazia ou vínculo ausente/não aprovado; identifica o pagamento. |
| `PAGAMENTOS_EXCEDEM_COMPROMISSO` | Soma dos pagamentos superior à reserva; identifica todos os pagamentos do compromisso para revisão, sem presumir qual está errado. |
| `RECURSOS_INSUFICIENTES` | Reservas ou pagamentos superiores aos aportes do projeto; identifica o projeto, cujos aportes, contratos e pagamentos podem ser consultados pelas rotas existentes. |

`quantidadeInconsistencias` conta ocorrências, não registros distintos: um registro pode violar mais de uma regra. Identificadores de vínculos indisponíveis retornam `null`. Registros órfãos sem projeto identificável aparecem somente na consulta global; não são atribuídos arbitrariamente a um projeto. No total geral, cada projeto é conferido individualmente, evitando compensar um déficit com a sobra de outro projeto.

Sem movimentação, a resposta contém os cinco totais zerados, `consolidacaoCompleta: true`, `statusConsolidacao: "COMPLETA"`, zero ocorrências e uma lista vazia. Um contrato em rascunho sem reserva não constitui inconsistência. Pagamentos parciais válidos também não.

A conferência e os totais usam a mesma transação de leitura com snapshot consistente. A consulta não corrige, exclui nem altera lançamentos e não utiliza Redis. Totais preservam o cálculo dos registros disponíveis; referências quebradas podem impedir incluir registros órfãos nos agregados, razão pela qual a resposta deve ser apresentada como incompleta. Não há certificação de fatos externos: a conferência identifica as divergências estruturais e financeiras descritas acima.

```json
{"compromissoId":1,"projetoId":1,"valor":"300.00","dataPagamento":"2026-10-16","referencia":"Repasse simulado","chaveOperacao":"operacao-demonstracao-0001","observacao":"Registro acadêmico"}
```

A chave tem 16 a 64 caracteres: letras ASCII, números, `_` ou `-`. Gere uma chave por operação lógica e conserve-a nos reenvios. O primeiro pagamento retorna 201; repetição idêntica pelo mesmo usuário retorna 200 e o mesmo ID; alteração do conteúdo/usuário retorna 409. Não troque a chave apenas porque houve timeout.

## Erros

Formato: `{"codigo":"...","mensagem":"...","campos":[],"requisicaoId":"..."}`. O identificador também consta no cabeçalho `X-Request-ID`.

- 400: JSON, parâmetros ou paginação inválidos.
- 401: sessão ausente/inválida.
- 403: perfil sem permissão ou CSRF inválido.
- 404: registro ausente ou pertencente a outro produtor.
- 409: duplicidade, estado incompatível, saldo insuficiente ou chave reutilizada com outros dados.
- 429: cinco tentativas consecutivas de login recusadas para o e-mail; bloqueio de uma hora, código `LOGIN_TEMPORARIAMENTE_BLOQUEADO` e cabeçalho `Retry-After`. Tentativas durante o bloqueio não prorrogam o prazo. Um administrador pode consultar/remover o bloqueio pela rota `/usuarios/{id}/bloqueio-login`; DELETE exige CSRF e registra auditoria. Ver `docs/AUTENTICACAO_FRONTEND.md`.
- 413/415: arquivo grande/vazio ou conteúdo não aceito.
- 422: campos ou regra de negócio inválidos/incompletos.
- 503: banco ou armazenamento indisponível; preserve a chave do pagamento.

A ausência de um endpoint ou regra definida não autoriza o frontend a alterar diretamente o banco. A API é o único ponto de gravação do sistema.

### Consultas do produtor e revisão de permissões

O produtor pode consultar a lista de diagnósticos de sua propriedade, seu diagnóstico e seu cálculo pelas rotas de leitura acima. A API verifica o vínculo até a propriedade e o produtor antes de devolver os dados; IDs de outros produtores retornam 404. Isso não concede permissão para responder, concluir, reapurar ou calcular. O questionário de configuração continua restrito a gestor/técnico. Os históricos usam a identidade da sessão, nunca um `produtorId` enviado como filtro. Veja a [matriz revisada de rotas](PERMISSOES_ROTAS.md).

Nos relatórios, `dataInicio` e `dataFim` são inclusivos e filtram a data de pagamento; podem ser usados separadamente. `projetoId` e `subBaciaId` combinam-se com o período por interseção. `totalItems` e `totalPago` consideram todo o conjunto filtrado, não apenas a página. Página fora do intervalo retorna `items: []`, preservando os totais; conjunto sem correspondências retorna lista vazia, quantidade zero e valor zero. Período invertido ou data inválida retorna 400.


## Alertas de vencimento

A configuração administrativa, os campos retornados e os filtros estão em [Alertas de vencimento](ALERTAS_VENCIMENTO.md). O prazo padrão é 30 dias e pode ser alterado pelo administrador via API.
