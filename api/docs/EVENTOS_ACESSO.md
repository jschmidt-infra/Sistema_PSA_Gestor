# Eventos de acesso

A migração V6 cria `evento_acesso`, separada da auditoria de operações de negócio. Cada registro contém somente identificador do evento, tipo, motivo, identificador interno do usuário autenticado quando disponível, identificador da requisição gerado pelo servidor e data/hora. Não existe campo livre para corpo de requisição, credenciais ou cabeçalhos.

| Evento | Quando é registrado |
| --- | --- |
| `LOGIN_SUCESSO` | Após salvar o contexto autenticado na sessão. |
| `LOGIN_RECUSADO` | Resposta HTTP de erro no endpoint de login: credenciais recusadas, bloqueio temporário, CSRF/acesso negado, entrada inválida ou indisponibilidade. |
| `SESSAO_ENCERRADA` | Logout, alteração de senha, revogação detectada na próxima requisição, reautenticação ou destruição da sessão pelo container. |

Nas recusas, `usuario_id` permanece nulo: o sistema não confirma uma identidade a partir do e-mail informado. Nas sessões autenticadas, o identificador interno permite relacionar o evento à conta. Não se armazenam senha, hash de senha, e-mail, nome, documento, IP, token CSRF, cookie ou identificador HTTP da sessão nesses registros ou mensagens de auditoria.

O identificador de requisição é gerado no servidor; não é um token de autenticação. Eventos fora de uma requisição, como expiração automática, podem ter esse campo nulo. A tabela não usa chave estrangeira de usuário, para preservar o histórico mesmo se a conta for removida por manutenção controlada. Os dados continuam sendo de acesso restrito, não dados públicos ou anonimizados.

## Encerramento e duplicidade

O logout exige CSRF válido. Uma tentativa de logout recusada não gera encerramento. Sessões anônimas criadas para obter CSRF não geram evento de encerramento autenticado. O mesmo encerramento é registrado uma única vez, mesmo quando a invalidação explícita também aciona o listener do container.

O listener recebe a notificação de destruição, mas a API Servlet não informa se ela ocorreu por timeout ou encerramento do servidor; nesses casos o motivo é `EXPIRACAO_OU_ENCERRAMENTO_CONTAINER`. Fechar a aba não encerra imediatamente a sessão: sem logout, o evento depende da expiração no servidor. Interrupções abruptas do processo não garantem execução do listener.

## Persistência e falhas

Os eventos usam transação independente no MariaDB, para que uma recusa de autenticação não desfaça o registro. Há também uma mensagem operacional com os mesmos campos mínimos. Se o banco estiver indisponível, permanece um aviso sanitizado no log com `persistencia=indisponivel`, sem imprimir a exceção ou suas informações internas. Não há reenvio automático desses eventos para o banco.

Uma falha da auditoria não impede logout nem revogação da sessão. Portanto, em indisponibilidade do banco, consultar também os logs operacionais; a tabela pode estar incompleta para aquele período. O registro não altera o limite de cinco tentativas nem a duração de uma hora do bloqueio.

## Consulta técnica

A implementação não adiciona tela nem endpoint público de consulta. Para conferência técnica com acesso autorizado ao banco:

```sql
SELECT id, evento, motivo, usuario_id, requisicao_id, criado_em
FROM evento_acesso
ORDER BY id DESC
LIMIT 100;
```

Os testes cobrem sucesso, senha incorreta, bloqueio, CSRF, entrada inválida, logout, alteração de senha, revogação, listener de expiração, prevenção de duplicidade, ausência de segredos e falha de persistência. Os testes de expiração acionam o listener diretamente, sem aguardar os 30 minutos de inatividade.
