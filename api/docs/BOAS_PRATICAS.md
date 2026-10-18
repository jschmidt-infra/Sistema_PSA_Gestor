# Convenções de desenvolvimento da API

## Organização e responsabilidades

Os pacotes são organizados por domínio. Controllers recebem entradas HTTP, aplicam a validação declarativa e encaminham operações aos serviços. Serviços coordenam autorização por titularidade, regras de negócio, transações e auditoria. Repositórios concentram a persistência. As entidades representam o estado persistido; alterações de esquema exigem migrações Flyway.

Preserve os contratos HTTP ao refatorar. Alterações em nomes de campos, códigos de erro, permissões ou regras de cálculo precisam ser tratadas como mudanças funcionais, com documentação e testes correspondentes.

## Legibilidade e comentários

- Use nomes que expressem a intenção e extraia validações ou transformações complexas para métodos pequenos.
- Evite duplicar regras compartilhadas, como os limites de senhas.
- Comente decisões, restrições e motivos que não são evidentes no código: ordem de bloqueios, versões históricas, limpeza compensatória e limites de memória.
- Não acrescente comentários que apenas repitam instruções, nem informações que fiquem desatualizadas facilmente.
- Use Javadoc quando o contrato de um componente exigir explicação, como quem fecha um fluxo de arquivo.
- Prefira imports explícitos no código de produção e remova imports não utilizados.

## Formatação automatizada

O Maven executa `spotless:check` na fase `verify`. O formato Java usa Google Java Format, estilo AOSP, com versões fixadas no `pom.xml`. A verificação não modifica os arquivos.

Para formatar após editar:

```powershell
./mvnw.cmd spotless:apply
```

Para conferir apenas a formatação:

```powershell
./mvnw.cmd spotless:check
```

Use o script de testes para a validação completa em bancos descartáveis:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Testar-MariaDB.ps1
```

## Cuidados de implementação

- Valores monetários usam `BigDecimal` e arredondamento explícito. Preserve os registros históricos e a memória dos cálculos.
- Mantenha os bloqueios e transações que evitam pagamentos concorrentes acima do saldo; não substitua garantias do banco por verificações apenas em memória.
- Feche recursos com `try-with-resources`. O adaptador R2 fecha seu cliente ao encerrar o contexto da aplicação.
- Valide o conteúdo de uploads; não confie apenas na extensão ou no MIME informado. Verifique dimensões antes de decodificar imagens.
- Nunca registre senhas, cookies, tokens ou credenciais de infraestrutura. Erros de serviços externos devem gerar respostas controladas.
- Redis é uma otimização opcional; indisponibilidade do cache não deve interromper operações independentes dele.
- Teste regras e limites observáveis, incluindo autorização entre produtores e entradas inválidas. Testes devem continuar úteis mesmo após reorganizações internas.

## Limites da revisão

Formatação e testes reduzem regressões, mas não comprovam ausência absoluta de defeitos. Esta revisão não define a metodologia oficial pendente do grupo e não substitui a homologação na VPS nem a integração com o frontend. As evidências da suíte ficam em `ACEITACAO_RESULTADOS.md`.
