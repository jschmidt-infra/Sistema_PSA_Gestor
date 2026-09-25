# 🌿 PSA Gestor — Pagamento por Serviços Ambientais

Projeto acadêmico desenvolvido na disciplina de **Projeto Integrador** para apoiar o cadastro de produtores e propriedades, o diagnóstico ambiental, o cálculo de incentivos, a geração de contratos e o acompanhamento financeiro simulado de um programa de Pagamento por Serviços Ambientais (PSA).

O objetivo é entregar um **protótipo funcional**, acompanhado de requisitos, arquitetura, diagramas e testes. A equipe é composta por **cinco integrantes**, com entrega prevista para **11/12/2026**.

## 🎯 Escopo do protótipo

- Autenticação e controle de acesso por perfis: administrador, gestor, técnico e produtor.
- Cadastro de produtores, propriedades e fontes hídricas, incluindo o registro explícito da ausência de fontes.
- Diagnóstico ambiental com salvamento progressivo, retomada e correção controlada.
- Configuração dos parâmetros de cálculo por projeto.
- Apuração da pontuação e do valor anual do PSA.
- Registro de croquis, visitas técnicas e geração de contratos a partir de modelo preliminar.
- Registro de assinatura simulada.
- Registro manual de saldo inicial, aportes e pagamentos totais ou parciais.
- Consulta de saldo disponível, valores comprometidos, relatórios e vigências.
- Consulta restrita do produtor aos próprios dados, contratos e repasses.

O sistema não realiza transferências bancárias. Os recursos e pagamentos são simulados para demonstração acadêmica. Integração bancária, assinatura digital com validade jurídica, SIG completo, integração com MUDA e sincronização offline não integram o escopo atual.

## 🏗️ Arquitetura e organização

O backend será uma única aplicação Spring Boot, organizada por módulos de negócio. O frontend Angular consumirá a API REST e terá interface responsiva para navegadores de computadores e celulares.

Organização prevista do repositório:

```text
psa-gestor/
├── README.md               # Visão geral do projeto
├── api/                    # Backend Java e Spring Boot
│   └── README.md           # Execução, endpoints, segurança e testes
├── front/                  # Frontend Angular
│   └── README.md           # Execução, telas, rotas e integração
└── vps/                    # Configurações de infraestrutura do Mini PC
    └── README.md           # Docker, publicação, volumes e backup
```

O nome `vps/` é mantido como organização da infraestrutura; a hospedagem definida para o projeto é um Mini PC próprio.

## 🛠️ Tecnologias

| Camada | Tecnologia | Versão / definição |
|---|---|---|
| Backend | Java | 25; atualização e distribuição padronizadas pela equipe |
| Framework | Spring Boot | 4.1.1 |
| API REST | Spring Web MVC | Starter do Spring Boot 4.1.1 |
| Segurança | Spring Security | Gerenciada pelo Spring Boot |
| Persistência | Spring Data JPA e Hibernate ORM | Gerenciadas pelo Spring Boot |
| Validação | Jakarta Validation / Hibernate Validator | Gerenciada pelo Spring Boot |
| Banco de dados | MariaDB Server | 13.0.2, já instalado conforme informado pelo responsável |
| Driver JDBC | MariaDB Connector/J | Gerenciado pelo Spring Boot |
| Frontend | Angular | 21 |
| Build do backend | Maven Wrapper | Proposta: Maven 3.9.16 |
| Documentação da API | springdoc-openapi / Swagger UI | Proposta: 3.1.1 |
| Testes | JUnit Jupiter e Mockito | Gerenciados pelo Spring Boot; JUnit 6 na base 4.1.1 |
| Publicação | Docker, Portainer e Cloudflare Tunnel | Ambiente definido pela equipe |

As dependências gerenciadas devem herdar suas versões do Spring Boot, evitando sobrescritas individuais sem necessidade. As versões efetivamente adotadas ficam registradas no `pom.xml`, no Maven Wrapper e no arquivo de dependências do frontend.

Referências: [Spring Boot — compatibilidade](https://docs.spring.io/spring-boot/system-requirements.html), [dependências gerenciadas](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html), [Maven](https://maven.apache.org/download.cgi) e [springdoc](https://springdoc.org/).

## 🔐 Acesso e perfis

Não haverá cadastro público de usuários. A conta inicial de administrador será criada previamente, e o administrador será responsável pela criação das demais contas.

| Perfil | Responsabilidade principal |
|---|---|
| Administrador | Gerenciar contas, perfis e parâmetros de cálculo por projeto. |
| Gestor | Calcular PSA, preparar e aprovar contratos, registrar aportes e pagamentos e consultar a gestão financeira. |
| Técnico | Cadastrar produtores e propriedades, realizar diagnósticos, calcular PSA e registrar croquis e visitas. |
| Produtor | Consultar exclusivamente as próprias informações e repasses. |

A matriz de permissões detalha as operações e exceções, incluindo correções cadastrais pelo gestor durante a preparação do contrato. O administrador não herda automaticamente as permissões dos demais perfis.

A proposta de autenticação utiliza sessão com cookie `HttpOnly` e `Secure`, proteção CSRF e CORS restrito ao frontend autorizado. A API deve validar o perfil e o acesso aos registros em cada operação.

## 🧮 Cálculo e regras financeiras

```text
Valor anual = (Valor da terra por hectare × Percentual) × Pontuação × Área contratada
```

- A área contratada é expressa em hectares, e o percentual deve ser convertido para fração no cálculo.
- O administrador configura os parâmetros por projeto.
- Questionário, pesos, escala da pontuação e percentual numérico permanecem sujeitos à definição da equipe e do responsável pelo programa.
- Valores monetários e demais componentes do cálculo devem utilizar `BigDecimal`, com regras explícitas de precisão e arredondamento.
- Cada cálculo preserva os parâmetros utilizados. Alterações posteriores não reescrevem automaticamente cálculos e contratos anteriores.
- A formalização e aprovação do contrato geram o comprometimento do valor anual correspondente.
- Uma obrigação anual pode receber vários pagamentos parciais. Cada registro permanece vinculado ao contrato, ao produtor e ao projeto.
- O reenvio da mesma operação não deve duplicar o pagamento.

Modelo financeiro proposto nos documentos de desenvolvimento:

```text
Saldo em caixa = recursos recebidos − pagamentos registrados
Comprometido pendente = compromissos anuais − pagamentos vinculados
Saldo disponível = saldo em caixa − comprometido pendente
```

## 🚀 Hospedagem e arquivos

Ambiente definido: **Mini PC Beelink N95, com 8 GB de RAM**, utilizando Docker, Portainer e Cloudflare Tunnel.

| Serviço | Endereço previsto |
|---|---|
| Frontend | https://psagestor.josueschmidt.com.br |
| Backend | https://psagestor-api.josueschmidt.com.br |

Esses endereços identificam o destino da publicação e não constituem confirmação de disponibilidade do sistema.

O frontend será servido como arquivos estáticos pelo Nginx. O backend acessará o MariaDB pela rede privada do ambiente Docker.

Para contratos e croquis, a proposta atual utiliza **volume persistente do Docker**, com referências aos arquivos no banco e download autorizado pela API. Cloudflare R2 não faz parte dessa proposta de armazenamento. O Cloudflare Tunnel é utilizado para disponibilizar o acesso web.

Credenciais e tokens devem ficar fora do repositório. O planejamento de backup contempla o banco e os arquivos persistidos.

## 📚 Documentação por módulo

Os guias abaixo devem acompanhar a implementação de cada módulo:

- **API — `api/README.md`:** configuração local, autenticação, endpoints, exemplos, Swagger UI e execução dos testes.
- **Frontend — `front/README.md`:** configuração local, telas, rotas, integração e build.
- **Infraestrutura — `vps/README.md`:** containers, rede, variáveis de ambiente, volumes, publicação e recuperação de backup.

A referência funcional é a **Especificação de Requisitos PSA Gestor, versão 2.1, de 25/09/2026**, complementada pelos documentos revisados de banco de dados, contrato da API, fluxos e regras, execução, permissões e modelo preliminar de contrato.

As propostas técnicas e decisões pendentes estão identificadas nesses documentos e devem ser validadas antes da implementação dos fluxos correspondentes.

## 🚦 Status e próximos passos

O projeto está em desenvolvimento acadêmico. Este README descreve o escopo e as decisões de arquitetura; a conclusão de funcionalidades deve ser acompanhada pelas tarefas, testes e entregas do repositório.

Sequência de implementação recomendada:

1. Preparar o projeto, o banco de desenvolvimento e a execução local da equipe.
2. Entregar login por perfil, cadastro de produtor e propriedade e consultas integradas.
3. Implementar diagnóstico, parâmetros por projeto e cálculo.
4. Implementar contratos, aportes, pagamentos parciais e consultas financeiras.
5. Concluir relatórios, testes, documentação e publicação do protótipo.

Permanecem pendentes as definições do questionário e dos pesos, as condições finais de vigência e renovação, as regras de correção após aprovação e a eventual exigência de vistoria para liberação de pagamentos. Dados fictícios de teste devem ser identificados como tal.

---

*Projeto Integrador — tecnologia aplicada à gestão de incentivos à conservação ambiental.*
