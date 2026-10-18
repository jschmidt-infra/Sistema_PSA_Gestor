CREATE TABLE evento_acesso (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 evento VARCHAR(30) NOT NULL,
 motivo VARCHAR(60) NOT NULL,
 usuario_id BIGINT,
 requisicao_id CHAR(36),
 criado_em DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE INDEX ix_evento_acesso_data ON evento_acesso(criado_em);
CREATE INDEX ix_evento_acesso_usuario ON evento_acesso(usuario_id,criado_em);
