-- Apenas a base de identidade. As demais tabelas recebem migrações incrementais.
CREATE TABLE produtor (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    documento VARCHAR(14),
    endereco VARCHAR(255),
    telefone VARCHAR(30),
    email VARCHAR(254),
    criado_em DATETIME(6) NOT NULL,
    criado_por BIGINT NOT NULL,
    atualizado_em DATETIME(6),
    atualizado_por BIGINT,
    CONSTRAINT ck_produtor_contato CHECK (
        (telefone IS NOT NULL AND LENGTH(TRIM(telefone)) > 0)
        OR (email IS NOT NULL AND LENGTH(TRIM(email)) > 0)
    )
);

CREATE TABLE usuario (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    produtor_id BIGINT,
    versao_credenciais BIGINT NOT NULL DEFAULT 0,
    criado_em DATETIME(6) NOT NULL,
    criado_por BIGINT,
    atualizado_em DATETIME(6),
    atualizado_por BIGINT,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT uk_usuario_produtor UNIQUE (produtor_id),
    CONSTRAINT fk_usuario_produtor FOREIGN KEY (produtor_id) REFERENCES produtor(id),
    CONSTRAINT fk_usuario_autor FOREIGN KEY (criado_por) REFERENCES usuario(id),
    CONSTRAINT fk_usuario_editor FOREIGN KEY (atualizado_por) REFERENCES usuario(id),
    CONSTRAINT ck_usuario_perfil CHECK (perfil IN ('ADMINISTRADOR','GESTOR','TECNICO','PRODUTOR')),
    CONSTRAINT ck_usuario_vinculo CHECK (
        (perfil = 'PRODUTOR' AND produtor_id IS NOT NULL)
        OR (perfil <> 'PRODUTOR' AND produtor_id IS NULL)
    )
);

ALTER TABLE produtor ADD CONSTRAINT fk_produtor_autor FOREIGN KEY (criado_por) REFERENCES usuario(id);
ALTER TABLE produtor ADD CONSTRAINT fk_produtor_editor FOREIGN KEY (atualizado_por) REFERENCES usuario(id);
CREATE INDEX ix_produtor_nome ON produtor(nome);
