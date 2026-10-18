CREATE TABLE tentativa_login (
 email_chave CHAR(64) PRIMARY KEY,
 falhas INT NOT NULL DEFAULT 0,
 bloqueado_ate DATETIME(6),
 CHECK(falhas BETWEEN 0 AND 5)
);
