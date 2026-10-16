# 🌿 PSA Gestor — Pagamento por Serviços Ambientais

Projeto acadêmico desenvolvido na disciplina de **Projeto Integrador**, voltado à gestão de um programa de Pagamento por Serviços Ambientais (PSA).

O sistema reúne cadastro de produtores e propriedades, diagnóstico ambiental, cálculo de incentivos, contratação e acompanhamento financeiro simulado. A interface web será responsiva, acessível pelo navegador em computadores e celulares.

O objetivo é entregar um **protótipo funcional**, acompanhado de documentação de requisitos, arquitetura, diagramas UML e testes.

## 🎯 Escopo do projeto

As funcionalidades previstas incluem:

- Autenticação e controle de acesso por perfis.
- Cadastro de produtores, propriedades, fontes hídricas, sub-bacias e projetos.
- Registro explícito da ausência de fontes hídricas na propriedade.
- Diagnóstico ambiental com salvamento progressivo, retomada e correção controlada.
- Configuração de questionários, pesos e parâmetros de cálculo.
- Apuração da pontuação ambiental e do valor anual do PSA.
- Registro de croquis e visitas técnicas.
- Geração de contratos a partir do modelo definido para o programa.
- Registro de assinatura simulada.
- Gestão de saldo inicial, aportes, reservas e pagamentos totais ou parciais.
- Consulta de saldos, contratos, vigências e relatórios.
- Acesso restrito do produtor às próprias informações e repasses.

**Os pagamentos são simulados.** O sistema não realiza transferências bancárias.

Integração bancária, assinatura digital com validade jurídica, sistema completo de informações geográficas, integração com MUDA e sincronização offline não integram o escopo atual.

## 🏗️ Arquitetura

O backend adota uma arquitetura de **monólito modular**: uma aplicação Spring Boot organizada por módulos de negócio, com separação entre API, serviços, persistência e modelos.

O frontend Angular consumirá a API REST. A publicação será realizada em uma VPS, com serviços em contêineres Docker e acesso HTTPS pelo Nginx Proxy Manager.

### Organização prevista do repositório

```text
Sistema_PSA_Gestor/
├── README.md               # Visão geral do projeto
├── api/                    # Backend Java e Spring Boot
│   └── README.md           # Execução, endpoints, segurança e testes
├── front/                  # Frontend Angular
│   └── README.md           # Execução, telas, rotas e integração
└── vps/                    # Infraestrutura e publicação
    └── README.md           # Contêineres, rede, configurações e backups
```

### Módulos do backend

| Módulo | Responsabilidade |
| --- | --- |
| `acesso` | Contas, autenticação, sessões e autorização. |
| `cadastros` | Produtores, propriedades, sub-bacias e projetos. |
| `configuracao` | Parâmetros de cálculo e questionários versionados. |
| `diagnostico` | Avaliações ambientais e respostas. |
| `calculo` | Fórmula do PSA e memória de cálculo. |
| `contratos` | Contratos, exercícios anuais e documentos relacionados. |
| `financeiro` | Recursos, reservas e pagamentos simulados. |
| `consultas` | Consultas e relatórios conforme o perfil. |
| `arquivos` | Integração com o armazenamento privado de arquivos. |
| `cache` | Cache opcional de indicadores não financeiros. |
| `comum` | Tratamento de erros e recursos compartilhados. |

A existência de um módulo na estrutura não significa que todas as suas funcionalidades estejam implementadas.

## 🛠️ Tecnologias

| Camada | Tecnologia | Versão ou definição |
| --- | --- | --- |
| Backend | Java | 25; JDK utilizado no desenvolvimento: 25.0.4.1 |
| Framework | Spring Boot | 4.1.1 |
| API REST | Spring Web MVC | Versão gerenciada pelo Spring Boot |
| Segurança | Spring Security | Autenticação por sessão e proteção CSRF |
| Persistência | Spring Data JPA e Hibernate ORM | Versões gerenciadas pelo Spring Boot |
| Validação | Jakarta Validation e Hibernate Validator | Versões gerenciadas pelo Spring Boot |
| Banco de dados | MariaDB Server | 13.0.2 |
| Migrações | Flyway | Evolução versionada do esquema |
| Frontend | Angular | 21 |
| Build do backend | Maven Wrapper | 3.9.11 |
| Testes | JUnit Jupiter, Spring Test e MockMvc | Testes unitários e de integração |
| Banco de teste | H2 | Exclusivamente para testes automatizados locais |
| Cache | Redis | Integração prevista para indicadores não financeiros |
| Arquivos | Cloudflare R2 | Bucket privado, compatível com a API S3 |
| Contêineres | Docker | Execução dos serviços na VPS |
| Proxy reverso | Nginx Proxy Manager | Entrada HTTPS e encaminhamento das requisições |
| Arquivos estáticos | Nginx | Publicação do frontend |

As versões das dependências gerenciadas são herdadas do Spring Boot. As definições efetivamente utilizadas devem ser consultadas no `pom.xml`, no Maven Wrapper e nos arquivos de dependências do frontend.

A documentação interativa com **OpenAPI/Swagger UI** está prevista para uma etapa posterior.

## 🔐 Autenticação e permissões

Não haverá cadastro público de usuários. O administrador inicial será provisionado previamente, e a criação das demais contas será realizada pelo administrador.

| Perfil | Responsabilidades previstas |
| --- | --- |
| Administrador | Gerenciar contas, perfis e parâmetros de cálculo por projeto. |
| Gestor | Preparar e aprovar contratos, calcular PSA, registrar recursos e pagamentos e acompanhar a gestão financeira. |
| Técnico | Cadastrar produtores e propriedades, realizar diagnósticos, calcular PSA e registrar croquis e visitas. |
| Produtor | Consultar exclusivamente suas informações, contratos e repasses. |

A matriz de permissões do projeto detalha as operações autorizadas e suas exceções. O administrador não herda automaticamente as atribuições dos demais perfis.

A autenticação utiliza:

- Sessão HTTP mantida no servidor.
- Cookie `HttpOnly`, `Secure` em produção e `SameSite=Lax`.
- Proteção CSRF nas operações que alteram dados.
- Rotação do identificador da sessão após o login.
- Senhas armazenadas com hash.
- Verificação da situação da conta, do perfil e da validade da sessão.
- Autorização por operação e por vínculo com os dados acessados.

O frontend utilizará caminhos relativos, como `/api/v1`, na mesma origem da aplicação web. Em desenvolvimento, essa comunicação será encaminhada pelo proxy do Angular.

## 🧮 Cálculo do PSA

A fórmula adotada é:

```text
Valor anual = (Valor da terra por hectare × Percentual) × Pontuação × Área contratada
```

Regras consideradas:

- A área contratada é expressa em hectares.
- O percentual é representado como fração no cálculo: `4%`, por exemplo, corresponde a `0.04`.
- O serviço inicial trabalha com pontuação normalizada entre `0` e `1`.
- Valores monetários e demais componentes da fórmula utilizam `BigDecimal`.
- A área contratada não pode ultrapassar a área total da propriedade.
- Questionários, pesos e parâmetros devem ser definidos e versionados.
- Cada memória de cálculo deverá preservar os valores e as versões utilizados.
- Alterações posteriores de parâmetros não devem modificar automaticamente contratos e cálculos históricos.

A implementação inicial arredonda o resultado final para duas casas decimais com `HALF_UP`. Essa decisão técnica deverá ser confirmada antes da conclusão do fluxo financeiro.

O questionário definitivo, seus pesos e os valores oficiais dos parâmetros não são presumidos pelo código.

## 💰 Gestão financeira simulada

O módulo financeiro deverá registrar os recursos disponíveis, o comprometimento decorrente dos contratos aprovados e os pagamentos simulados.

Uma obrigação anual poderá receber pagamento integral ou vários pagamentos parciais. Cada lançamento deverá permanecer vinculado ao contrato, ao produtor beneficiado e ao projeto de origem.

O acompanhamento financeiro considera:

```text
Saldo em caixa = recursos recebidos − pagamentos registrados

Comprometido pendente = valores reservados ainda não pagos

Saldo disponível = saldo em caixa − comprometido pendente
```

O processamento deverá garantir:

- Reserva do valor correspondente na aprovação do contrato.
- Validação do saldo e dos limites da obrigação.
- Atualização consistente de pagamentos e compromissos.
- Registro de auditoria.
- Proteção contra pagamentos duplicados por reenvio de requisições.
- Controle de concorrência nas operações financeiras.

O MariaDB será a fonte dos dados financeiros. Saldos, reservas e pagamentos não dependerão do Redis.

## 📁 Arquivos e documentos

Croquis e documentos contratuais serão armazenados em **bucket privado do Cloudflare R2**.

O MariaDB manterá os metadados, a chave do objeto e o vínculo com o registro correspondente. As credenciais do armazenamento ficarão exclusivamente no backend.

O fluxo previsto inclui:

- Upload e download controlados pela API.
- Validação de perfil e permissão sobre o documento.
- Validação de tamanho e tipo de arquivo.
- Preservação dos documentos de contratos aprovados.
- Tratamento de falhas entre o armazenamento no R2 e a persistência no banco.
- Identificação e compensação de objetos órfãos.

A integração utiliza a interface compatível com S3 do Cloudflare R2. URLs temporárias não serão utilizadas como referências permanentes no banco.

## ⚡ Cache

O Redis está previsto como recurso opcional para indicadores não financeiros, como contagens e áreas agregadas.

A estratégia definida contempla:

- Tempo de expiração de 60 segundos.
- Timeout de 200 milissegundos.
- Consulta ao banco quando o cache estiver indisponível.

Permissões, sessões, documentos, saldos e pagamentos não serão armazenados nesse cache.

Na estrutura inicial, o cache permanece desativado; a integração efetiva com Redis ainda será implementada.

## 🚀 Infraestrutura

A hospedagem prevista é uma **VPS da Loophost, em Taquara/RS**, com:

- **2 vCPU**
- **2 GB de RAM**
- **70 GB de armazenamento**
- **IP público fixo**

A VPS executará backend, frontend, MariaDB, Redis e Nginx Proxy Manager em contêineres Docker. O armazenamento dos arquivos permanecerá no Cloudflare R2.

| Finalidade | Endereço previsto |
| --- | --- |
| Aplicação web | `https://psagestor.duckdns.org` |
| Acesso técnico à API | `https://api-psagestor.duckdns.org` |

O navegador acessará a API por `/api/v1` no domínio do frontend, com encaminhamento pelo Nginx Proxy Manager.

Esses endereços representam o planejamento de publicação; não indicam que o sistema já esteja disponível.

MariaDB e Redis deverão permanecer em rede privada. O planejamento de operação inclui persistência dos dados, configurações externas ao código, backups e testes de restauração.

## 🧪 Desenvolvimento e testes

O ambiente de desenvolvimento do backend utiliza:

- JDK 25.
- Visual Studio Code.
- Git.
- DBeaver para administração e consulta do banco.
- Postman para testes das requisições.
- Maven Wrapper incluído no projeto.

A API pode ser executada localmente no Windows, conectando-se a um banco MariaDB de desenvolvimento por acesso privado, como túnel SSH.

Os testes automatizados locais utilizam H2 em memória e não dependem da VPS. A validação em H2 não substitui os testes das migrações, dos tipos e da concorrência no MariaDB.

As instruções de configuração, execução e testes devem ser mantidas no README do backend.

## 📚 Documentação

O desenvolvimento é orientado pelos seguintes artefatos:

- Documento de Visão.
- Especificação de Requisitos, casos de uso e user stories.
- Documento de Arquitetura e Diagramas UML.
- Modelo do Banco de Dados.
- Contrato da API.
- Fluxos e Regras Principais.
- Guia de Execução.
- Matriz de Permissões dos Perfis.
- Modelo Preliminar de Contrato.
- Plano e casos de teste, a serem atualizados conforme a implementação.

Os guias de cada módulo complementam esses documentos:

| Guia | Conteúdo |
| --- | --- |
| `api/README.md` | Configuração local, autenticação, endpoints e testes. |
| `front/README.md` | Execução, telas, rotas e integração com a API. |
| `vps/README.md` | Contêineres, rede, publicação, persistência e recuperação de backups. |

## 🚦 Estado do desenvolvimento

A estrutura inicial do backend foi criada e validada com compilação, geração do pacote executável e **12 testes automatizados aprovados** na validação inicial.

### Implementado na base inicial

- Organização dos módulos de negócio.
- Login e logout por sessão.
- Consulta da própria conta.
- Alteração da própria senha.
- Proteção CSRF e invalidação de sessões.
- Provisionamento opcional do administrador inicial.
- Migração inicial das tabelas de usuário e produtor.
- Serviço de cálculo do valor anual do PSA.
- Tratamento padronizado de erros.
- Coleção inicial do Postman.
- Maven Wrapper, configuração do VS Code e Dockerfile da API.

### Próximas etapas

1. Implementar a administração de usuários e os cadastros.
2. Desenvolver parâmetros, questionários e diagnósticos.
3. Persistir a memória de cálculo e integrar o fluxo contratual.
4. Implementar arquivos no R2 e geração de documentos.
5. Desenvolver reservas, aportes e pagamentos simulados.
6. Integrar o frontend e as consultas por perfil.
7. Implementar o cache dos indicadores permitidos.
8. Validar a aplicação no MariaDB e no ambiente Docker.
9. Concluir os testes, a documentação e a publicação do protótipo.

A estrutura inicial ainda não representa o MVP completo. As integrações com R2 e Redis, os fluxos financeiros, a geração de documentos e a publicação na VPS permanecem em desenvolvimento.

---

*Projeto Integrador — tecnologia aplicada à gestão de incentivos à conservação ambiental.*
