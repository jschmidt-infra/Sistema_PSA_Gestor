# Alertas de vencimento de contratos

## Configuração administrativa

A antecedência é global para o sistema, persistida no MariaDB pela migração V7, com padrão de **30 dias**. Somente o administrador consulta e altera a configuração:

| Método | Rota | Resultado |
|---|---|---|
| GET | `/api/v1/configuracoes/vencimento-contratos` | Configuração atual, HTTP 200 |
| PUT | `/api/v1/configuracoes/vencimento-contratos` | Salva e retorna a configuração, HTTP 200 |

Corpo do PUT e resposta de ambas as rotas:

```json
{"antecedenciaDias":30}
```

O frontend deve oferecer um campo numérico inteiro e um botão para salvar. São aceitos valores de 0 a 3650 dias, limite técnico para evitar entradas excessivas. Campo ausente, nulo ou fora desses limites retorna 422. Zero desativa a faixa antecipada, mas mantém as sinalizações de vencido e de vencimento no dia. PUT exige sessão administrativa e CSRF, conforme `AUTENTICACAO_FRONTEND.md`. Outros perfis recebem 403 e anônimos 401.

A alteração é auditada e vale para consultas posteriores, inclusive de contratos existentes. Reiniciar a aplicação não restaura o padrão. Não há cache dessa configuração. A tela já aberta precisa consultar os dados novamente para refletir a mudança.

## Dados para as telas

As respostas de `GET /api/v1/contratos`, `GET /api/v1/contratos/{id}` e `GET /api/v1/meu-historico` incluem os seguintes campos em cada contrato:

```json
{
  "fim":"2026-11-17",
  "status":"APROVADO",
  "situacaoVencimento":"PROXIMO_DO_VENCIMENTO",
  "diasParaVencimento":30,
  "antecedenciaAvisoDias":30,
  "dataReferenciaVencimento":"2026-10-18"
}
```

| Classificação | Regra | Texto sugerido |
|---|---|---|
| VENCIDO | Fim anterior a hoje | Vencido há N dias |
| VENCE_HOJE | Fim igual a hoje | Vence hoje |
| PROXIMO_DO_VENCIMENTO | Fim entre amanhã e hoje + antecedência, inclusive | Vence em N dias |
| DENTRO_DO_PRAZO | Fim posterior à janela | Dentro do prazo |
| NAO_APLICAVEL | Contrato não aprovado | Não mostrar aviso |

`diasParaVencimento` é negativo para vencidos, zero para hoje e positivo para datas futuras. Para rascunhos, é nulo. A data final existente em `fim` é preservada. O campo `status` continua representando o estado contratual, sem ser substituído pelo aviso.

O cálculo usa a data de `America/Sao_Paulo`, sem depender do fuso do navegador. Cada resposta usa uma única data e antecedência. Não depende de tarefa agendada, e não envia e-mails. A interface deve atualizar as consultas ao reabrir a tela e quando precisar renovar os avisos.

## Filtros e permissões

Gestor e técnico podem usar:

```text
GET /api/v1/contratos?situacaoVencimento=PROXIMO_DO_VENCIMENTO&page=0&size=20
```

O produtor usa o próprio histórico:

```text
GET /api/v1/meu-historico?situacaoVencimento=VENCIDO
```

O filtro aceita as quatro classificações aplicáveis a contratos aprovados. Valor desconhecido, vazio ou `NAO_APLICAVEL` retorna 400, código `SITUACAO_VENCIMENTO_INVALIDA`. Sem filtro, rascunhos continuam aparecendo conforme as regras anteriores.

O filtro é aplicado no banco antes da contagem/paginação. Na listagem geral pode ser combinado com `projetoId`, `status` e `vencimentoAte`; os critérios são cumulativos. Combinar um aviso com `status=RASCUNHO` retorna lista vazia. `vencimentoAte` sozinho mantém o comportamento anterior e não equivale ao novo filtro.

O histórico sempre utiliza o produtor vinculado à sessão. Não aceita trocar de titular por parâmetro. O detalhe de contrato continua verificando titularidade. Administrador não passa a ter acesso aos contratos por poder configurar a antecedência.

## Implantação e limites

A migração V7 cria e inicializa `configuracao_vencimento`. O Flyway a aplica ao iniciar a versão atualizada da API; não altere migrações anteriores. A mudança foi preparada para o backend: o campo administrativo, etiquetas e filtros visuais ainda precisam ser implementados no Angular.

Avisos não renovam contratos, não geram exercícios, não alteram compromissos e não bloqueiam pagamentos. Renovação continua dependente de definição própria.
