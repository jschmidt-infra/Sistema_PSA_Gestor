CREATE TABLE arquivo_pendente (
 chave VARCHAR(255) PRIMARY KEY,
 criado_em DATETIME(6) NOT NULL
);
CREATE INDEX ix_arquivo_pendente_data ON arquivo_pendente(criado_em);
