ALTER TABLE auditoria_evento ADD COLUMN requisicao_id VARCHAR(64);
CREATE INDEX ix_auditoria_recurso ON auditoria_evento(recurso,recurso_id,criado_em);
