CREATE TABLE localidade (id BIGINT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(120) NOT NULL, municipio VARCHAR(120) NOT NULL, uf CHAR(2) NOT NULL, UNIQUE(nome,municipio,uf));
CREATE TABLE sub_bacia (id BIGINT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(150) NOT NULL);
CREATE TABLE projeto (id BIGINT AUTO_INCREMENT PRIMARY KEY, nome VARCHAR(150) NOT NULL, origem_recursos VARCHAR(255) NOT NULL, ativo BOOLEAN NOT NULL DEFAULT TRUE);
CREATE TABLE auditoria_evento (id BIGINT AUTO_INCREMENT PRIMARY KEY, usuario_id BIGINT NOT NULL REFERENCES usuario(id), operacao VARCHAR(60) NOT NULL, recurso VARCHAR(60) NOT NULL, recurso_id BIGINT NOT NULL, criado_em DATETIME(6) NOT NULL);
CREATE TABLE propriedade (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, produtor_id BIGINT NOT NULL REFERENCES produtor(id), localidade_id BIGINT NOT NULL REFERENCES localidade(id), sub_bacia_id BIGINT REFERENCES sub_bacia(id),
 nome VARCHAR(150) NOT NULL, area_total DECIMAL(12,4) NOT NULL CHECK(area_total>0), car_situacao VARCHAR(20) NOT NULL CHECK(car_situacao IN ('POSSUI','NAO_POSSUI','NAO_SE_APLICA')), car_numero VARCHAR(100),
 fontes_situacao VARCHAR(20) NOT NULL DEFAULT 'NAO_AVALIADO' CHECK(fontes_situacao IN ('NAO_AVALIADO','AUSENTES','PRESENTES')),
 criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), atualizado_em DATETIME(6), atualizado_por BIGINT REFERENCES usuario(id),
 CHECK(car_situacao <> 'POSSUI' OR LENGTH(TRIM(car_numero))>0)
);
CREATE INDEX ix_propriedade_produtor ON propriedade(produtor_id);
CREATE TABLE fonte_hidrica (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, propriedade_id BIGINT NOT NULL REFERENCES propriedade(id), tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('NASCENTE','RIO','SANGA','ACUDE')),
 protecao VARCHAR(20) NOT NULL CHECK(protecao IN ('PROTEGIDA','NAO_PROTEGIDA','NAO_AVALIADA')), ativo BOOLEAN NOT NULL, observacoes TEXT,
 criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), atualizado_em DATETIME(6), atualizado_por BIGINT REFERENCES usuario(id)
);
CREATE TABLE visita (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, propriedade_id BIGINT NOT NULL REFERENCES propriedade(id), tecnico_id BIGINT NOT NULL REFERENCES usuario(id), data_visita DATE NOT NULL, observacoes TEXT NOT NULL,
 criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), atualizado_em DATETIME(6), atualizado_por BIGINT REFERENCES usuario(id)
);
CREATE TABLE parametro_projeto (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projeto(id), versao INT NOT NULL, valor_hectare DECIMAL(15,2) NOT NULL CHECK(valor_hectare>0),
 percentual DECIMAL(9,6) NOT NULL CHECK(percentual>=0 AND percentual<=1), configuracao_json TEXT NOT NULL, criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), UNIQUE(projeto_id,versao)
);
CREATE TABLE diagnostico (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, propriedade_id BIGINT NOT NULL REFERENCES propriedade(id), projeto_id BIGINT NOT NULL REFERENCES projeto(id), parametro_id BIGINT NOT NULL REFERENCES parametro_projeto(id),
 tecnico_id BIGINT NOT NULL REFERENCES usuario(id), data_visita DATE NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('RASCUNHO','CONCLUIDO','EM_CORRECAO')), revisao INT NOT NULL,
 respostas_json TEXT NOT NULL, pontuacao DECIMAL(9,6), memoria_json TEXT, motivo_correcao TEXT, concluido_em DATETIME(6), criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE TABLE calculo_psa (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, diagnostico_id BIGINT NOT NULL REFERENCES diagnostico(id), parametro_id BIGINT NOT NULL REFERENCES parametro_projeto(id), revisao_diagnostico INT NOT NULL,
 memoria_json TEXT NOT NULL, area_contratada DECIMAL(12,4) NOT NULL CHECK(area_contratada>0), valor_hectare DECIMAL(15,2) NOT NULL, percentual DECIMAL(9,6) NOT NULL, pontuacao DECIMAL(9,6) NOT NULL,
 valor_anual DECIMAL(15,2) NOT NULL CHECK(valor_anual>=0), criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE TABLE croqui (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, propriedade_id BIGINT NOT NULL REFERENCES propriedade(id), nome_original VARCHAR(255) NOT NULL, chave VARCHAR(255) NOT NULL UNIQUE, tipo_mime VARCHAR(100) NOT NULL,
 tamanho BIGINT NOT NULL, area_psa DECIMAL(12,4) NOT NULL CHECK(area_psa>0), observacoes TEXT, criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE TABLE contrato (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, numero VARCHAR(30) NOT NULL UNIQUE, produtor_id BIGINT NOT NULL REFERENCES produtor(id), propriedade_id BIGINT NOT NULL REFERENCES propriedade(id), projeto_id BIGINT NOT NULL REFERENCES projeto(id),
 calculo_id BIGINT NOT NULL UNIQUE REFERENCES calculo_psa(id), croqui_id BIGINT REFERENCES croqui(id), inicio DATE NOT NULL, fim DATE NOT NULL, status VARCHAR(20) NOT NULL CHECK(status IN ('RASCUNHO','APROVADO')),
 documento_chave VARCHAR(255), documento_snapshot TEXT, modelo_versao VARCHAR(30), assinatura_em DATE, aprovado_em DATETIME(6), aprovado_por BIGINT REFERENCES usuario(id), criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), CHECK(fim>=inicio)
);
CREATE TABLE aporte (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, projeto_id BIGINT NOT NULL REFERENCES projeto(id), tipo VARCHAR(20) NOT NULL CHECK(tipo IN ('SALDO_INICIAL','APORTE')), data_aporte DATE NOT NULL,
 valor DECIMAL(15,2) NOT NULL CHECK(valor>0), descricao VARCHAR(255) NOT NULL, criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE TABLE compromisso_anual (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, contrato_id BIGINT NOT NULL REFERENCES contrato(id), exercicio INT NOT NULL, valor DECIMAL(15,2) NOT NULL CHECK(valor>0), criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id), UNIQUE(contrato_id,exercicio)
);
CREATE TABLE pagamento (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, compromisso_id BIGINT NOT NULL REFERENCES compromisso_anual(id), data_pagamento DATE NOT NULL, valor DECIMAL(15,2) NOT NULL CHECK(valor>0), referencia VARCHAR(100) NOT NULL,
 chave_operacao VARCHAR(64) NOT NULL UNIQUE, hash_requisicao CHAR(64) NOT NULL, observacao TEXT, criado_em DATETIME(6) NOT NULL, criado_por BIGINT NOT NULL REFERENCES usuario(id)
);
CREATE INDEX ix_aporte_projeto ON aporte(projeto_id);
CREATE INDEX ix_contrato_projeto ON contrato(projeto_id,status);
CREATE INDEX ix_pagamento_compromisso ON pagamento(compromisso_id);
