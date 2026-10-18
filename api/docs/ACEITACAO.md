# Plano e rastreabilidade dos testes de aceitação

Situação: **EM ANDAMENTO**. Esta etapa prepara a aceitação da API; não declara todos os requisitos cumpridos nem autoriza a publicação.

## Base e limites

A matriz usa RF01–RF18, UC01–UC10, US01–US08 e RN01–RN12 da Especificação de Requisitos, em conjunto com o Documento de Visão. Os requisitos não funcionais da seção 6 não têm código numérico no documento: os nomes `RNF-*` da matriz são identificadores locais de rastreabilidade. `ARQ-OBSERVABILIDADE` também é um identificador local.

A matriz editável está em [aceitacao-matriz.json](aceitacao-matriz.json). Cada entrada informa requisito, origem, critério, métodos de teste e pendências. Os [resultados](ACEITACAO_RESULTADOS.md) são gerados a partir do Surefire, sem transformar testes ignorados ou ausentes em aprovações. A matriz seleciona evidências; não constitui uma declaração de cobertura integral de todas as exceções dos casos de uso.

Os testes HTTP de integração utilizam MockMvc, serviços reais, JPA e MariaDB descartável. O perfil é `test`, o armazenamento desses cenários é local, e os usuários e dados são fictícios. A autenticação real tem testes próprios; nos cenários de negócio, a sessão é inserida pelo suporte de testes do Spring Security. Isso não substitui o fluxo completo no navegador.

## Executar e registrar evidências

Com Java e Docker disponíveis, na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Testar-MariaDB.ps1
python scripts/Relatorio-Aceitacao.py
```

Use o Python instalado no ambiente se o comando `python` não estiver disponível. O primeiro comando cria e remove um MariaDB isolado; nunca substitua suas URLs pelo banco da aplicação. Ele executa `clean verify`, apagando resultados anteriores de `target`. O segundo comando lê os XMLs do Surefire e atualiza `docs/ACEITACAO_RESULTADOS.md`. Rode-o somente depois que a suíte terminar. Falhas e referências ausentes causam saída diferente de zero. Testes opcionais Redis/R2 podem ser ignorados quando as integrações reais não estão habilitadas, e isso é informado no relatório.

Ao adicionar ou renomear testes, atualize a matriz. Para a entrega final, preserve uma cópia dos resultados com revisão do código, data, ambiente e configuração sem segredos. Registre também os cenários manuais com esperado, observado, evidência e responsável pela validação.

## Cenários adicionados nesta etapa

| Cenário | Dado / quando | Resultado esperado |
|---|---|---|
| Vigência inválida | Gestor cria ou edita contrato com fim anterior ao início | HTTP 422, sem persistir a mudança inválida |
| Vigência de um dia | Início e fim iguais | Cadastro aceito com as datas preservadas |
| Limite de vencimento | Três contratos terminam antes, no dia e depois do limite | Consulta inclui os dois primeiros; exclui o posterior; filtros de projeto e status aplicáveis |
| Acesso ao filtro | Produtor tenta listar contratos gerais | HTTP 403 |
| Assinatura sem documento | Gestor registra assinatura antes da geração | HTTP 409 |
| Assinatura simulada | Documento existente, gestor e data válida | Registro único auditável; duplicidade recusada; técnico não autorizado; data futura recusada |
| Desempenho inicial | Diagnóstico com quatro grupos e 100 perguntas, após dois aquecimentos | Dez amostras corretas; cada fluxo combinado de apuração, cálculo e consulta em até 5.000 ms no teste local |

## Alertas de vencimento (RF14)

A antecedência global padrão é de 30 dias, alterável pelo administrador e persistida no MariaDB. A API distingue vencido, vence hoje, próximo do vencimento e dentro do prazo para contratos aprovados; rascunhos ficam como não aplicáveis. As consultas usam America/Sao_Paulo e permitem filtrar antes da paginação.

Os testes verificam ontem, hoje, amanhã, último dia da janela e dia seguinte, configuração zero, validação administrativa e auditoria, filtros combinados, histórico restrito ao titular e mudança do dia no fuso adotado. O filtro legado `vencimentoAte` mantém seu comportamento inclusivo e pode ser combinado com a nova classificação.

Falta validar a apresentação e o formulário de configuração no Angular. Renovação e geração de exercícios permanecem dependentes de definição e não são efeitos dos avisos. Contrato HTTP em [Alertas de vencimento](ALERTAS_VENCIMENTO.md).

## Desempenho: preparação da aceitação publicada

A seção 6 exige que a pontuação e o valor devido sejam apresentados em até cinco segundos. Não define percentil, número de usuários simultâneos ou tamanho de base. Portanto, não se pode substituir o limite por uma média nem afirmar capacidade de concorrência não medida.

A medição local fica em `target/aceitacao/desempenho-local.json` e no relatório. Mede processamento de requisições MockMvc com banco real e dados fictícios, sem navegador/rede/proxy. Usa 100 perguntas, o limite implementado, com pesos demonstrativos. Serve como regressão inicial, não como homologação do tempo percebido pelo usuário.

Na VPS, após definir a metodologia e integrar o frontend:

1. Registrar revisão do código, imagens Docker, configuração de recursos, tamanho da base, navegador e condições de rede. Usar somente dados de homologação.
2. Autenticar como técnico pelo fluxo real, preparar diagnósticos válidos de tamanho habitual e máximo e manter os serviços da publicação ativos.
3. Medir desde a ação na interface que solicita apuração/cálculo até os resultados visíveis. Se forem ações distintas, medir e registrar cada uma, além do percurso combinado quando aplicável.
4. Executar ao menos dez repetições por cenário, registrando todas as medições e erros. Registrar primeira execução e execuções aquecidas separadamente; não excluir uma falha do resultado de aceitação.
5. Comparar cada observação com cinco segundos. Investigar qualquer ultrapassagem; guardar evidência de rede e tempos da interface sem cookies, senhas ou dados pessoais.
6. Repetir com Redis indisponível para verificar continuidade e medir o impacto. Registrar CPU/memória da VPS. A verificação não exige teste de carga massivo para este projeto.

Os números de repetições são uma proposta de procedimento de teste, não um requisito adicional atribuído ao professor. Até essa execução, o desempenho ponta a ponta permanece pendente.

## Critérios para fechar a aceitação

- Metodologia e pesos aprovados pelo grupo, implementados e testados em `prod`.
- Pendências funcionais da matriz resolvidas ou formalmente ajustadas no escopo; os filtros de período, projeto e sub-bacia já têm cobertura combinada na API.
- Vencimentos sinalizados conforme regra acordada e casos de fronteira aprovados.
- Fluxos completos dos quatro perfis validados na interface, em desktop e celular, incluindo mensagens de erro e isolamento entre produtores.
- PDF revisado visualmente e texto contratual validado; upload/download privado no R2 conferidos no ambiente publicado.
- HTTPS, sessões/CSRF, permissões, métricas internas, logs e indisponibilidade real do Redis verificados na VPS.
- Desempenho ponta a ponta medido e suíte final sem falhas. Testes ignorados relevantes precisam ser executados ou permanecer explicitamente pendentes.

Os testes de privacidade verificam comportamentos técnicos delimitados; não constituem certificação jurídica de conformidade.

## Complemento: relatórios e permissões

Os testes de relatórios usam 12 pagamentos distribuídos em dois projetos, duas sub-bacias e quatro datas. Exercitam 36 combinações de projeto/sub-bacia/período, dia único, IDs sem correspondência e período sem resultados; cada cenário é conferido em três páginas (inicial, seguinte e fora do intervalo), com valores decimais e total independente da paginação. Há também cenário de banco sem pagamentos e rejeição de datas inválidas/invertidas.

A [matriz de rotas](PERMISSOES_ROTAS.md) é executada com os quatro perfis e sem autenticação, com CSRF válido e corpos válidos para que a validação de entrada não mascare uma falha de autorização. Um teste compara a matriz com os handlers registrados e falha se uma rota de negócio for adicionada ou removida sem revisão. Rotas de autenticação e infraestrutura têm verificações separadas. Dois produtores com dados próprios testam leitura permitida, IDs trocados e tentativa de alterar o beneficiário nos filtros de histórico.
