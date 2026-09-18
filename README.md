# 🌿 PSA Gestor (Pagamento por Serviços Ambientais)

Repositório oficial do **PSA Gestor**, um sistema integrado de gestão focado no cadastro, diagnóstico ambiental, contratação e cálculo financeiro de incentivos para conservação de ecossistemas e manejo sustentável.

## 🏗️ Arquitetura do Projeto

O sistema está estruturado em uma arquitetura modularizada para facilitar a manutenção e a escalabilidade entre o desenvolvimento de backend, frontend e infraestrutura:

```
psa-gestor/
├── README.md               # 🌐 Este documento (Visão global do projeto)
├── api/                    # 🔌 Backend da aplicação (Spring Boot & Java)
│   └── README.md           #    Documentação técnica da API, Endpoints e Swagger
├── front/                  # 💻 Aplicação Front-end (Interface do Usuário)
│   └── README.md           #    Guia de telas, rotas e consumo de serviços
└── vps/                    # 🚀 Infraestrutura e Deploy
    └── README.md           #    Instruções de hospedagem, MariaDB, Nginx e Cloudflare R2

```

## 🛠️ Tecnologias Principais

* **Backend:** Java 25, Spring Boot 3, Spring Data JPA, Hibernate, Maven, JUnit 5 & Mockito.

* **Banco de Dados:** MariaDB:11.4.

* **Armazenamento de Arquivos:** Cloudflare R2 (Object Storage compatível com S3).

* **Documentação de API:** Springdoc OpenAPI (Swagger UI).

## 📚 Documentação por Módulo

Para instruções detalhadas de como configurar, rodar e interagir com cada parte do sistema, acesse os guias específicos:

* [**🔌 Documentação da API (Backend)**](api/README.md)**:** Detalha todos os endpoints HTTP, regras de negócio do motor financeiro de PSA, payloads JSON de exemplo e acesso ao Swagger UI.

* [**💻 Documentação do Front-end**](front/README.md)**:** Orientações de configuração do ambiente de desenvolvimento de interface, gestão de rotas e integração com os serviços da API.

* [**🚀 Guia de Deploy e Infraestrutura (VPS)**](vps/README.md)**:** Instruções de configuração do servidor de produção, proxy reverso Nginx, variáveis de ambiente e banco de dados na nuvem.

## 🚦 Status do Projeto

* **Backend:** Funcionalmente completo, com rotas abertas para integração, motor de cálculo financeiro testado via unidades automatizadas e upload de arquivos integrado. (Em desenvolvimento).  (Em desenvolvimento).

* **Front-end:** Em fase de integração com os endpoints da API.

* **Infraestrutura:** Pronto para homologação em ambiente Linux/VPS.

*Desenvolvido para gestão eficiente e transparente de incentivos ambientais.*
