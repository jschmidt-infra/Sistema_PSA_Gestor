CREATE TABLE configuracao_vencimento (
    id BIGINT NOT NULL PRIMARY KEY,
    antecedencia_dias INT NOT NULL DEFAULT 30,
    CONSTRAINT ck_configuracao_vencimento_id CHECK (id = 1),
    CONSTRAINT ck_configuracao_vencimento_dias CHECK (antecedencia_dias BETWEEN 0 AND 3650)
);
INSERT INTO configuracao_vencimento(id, antecedencia_dias) VALUES (1, 30);
