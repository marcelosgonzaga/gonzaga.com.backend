-- Tabela de relatório de uso
CREATE TABLE relatorio_uso (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               codigo_cliente VARCHAR(4) NOT NULL,
                               data_uso DATETIME NOT NULL,
                               projeto_id BIGINT,
                               tipo_geracao VARCHAR(20),
                               quantidade_produtos INT,
                               nome_tema VARCHAR(255),
                               periodo_validade VARCHAR(100),
                               FOREIGN KEY (projeto_id) REFERENCES projeto(id) ON DELETE SET NULL,
                               INDEX idx_codigo_cliente (codigo_cliente),
                               INDEX idx_data_uso (data_uso)
);

-- Tabela de clientes para relatórios
CREATE TABLE relatorio_cliente (
                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                   codigo_cliente VARCHAR(4) NOT NULL UNIQUE,
                                   nome_cliente VARCHAR(255),
                                   data_registro DATE,
                                   total_uso INT DEFAULT 0,
                                   ultimo_uso DATETIME,
                                   ativo BOOLEAN DEFAULT TRUE,
                                   INDEX idx_codigo_cliente (codigo_cliente),
                                   INDEX idx_ativo (ativo)
);

-- Inserir clientes base a partir dos rodapés existentes
INSERT INTO relatorio_cliente (codigo_cliente, nome_cliente, data_registro, ativo)
SELECT DISTINCT
    CAST(codigo_loja AS CHAR) as codigo_cliente,
    CONCAT('Cliente ', CAST(codigo_loja AS CHAR)) as nome_cliente,
    CURDATE() as data_registro,
    TRUE as ativo
FROM rodape;