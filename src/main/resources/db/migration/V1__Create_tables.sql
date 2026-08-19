-- Tabela Tema
CREATE TABLE IF NOT EXISTS tema (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    nome VARCHAR(255) NOT NULL,
                                    descricao VARCHAR(255),
                                    caminho_imagem VARCHAR(255) NOT NULL
);

-- Tabela Rodape
CREATE TABLE IF NOT EXISTS rodape (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                      codigo_loja BIGINT NOT NULL UNIQUE,
                                      caminho_imagem VARCHAR(255) NOT NULL
);

-- Tabela Produto
CREATE TABLE IF NOT EXISTS produto (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       ean BIGINT NOT NULL UNIQUE,
                                       descricao VARCHAR(255) NOT NULL,
                                        preco_de DECIMAL(10,2),
                                        preco_por DECIMAL(10,2) NOT NULL,
                                        classificacao ENUM('MEDICAMENTO','PERFUMARIA'),
                                        caminho_imagem VARCHAR(255),
                                        isento BOOLEAN DEFAULT FALSE NOT NULL
 );

-- Tabela Projeto (corrigida)
CREATE TABLE IF NOT EXISTS projeto (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       data_inicio DATE NOT NULL,
                                       data_fim DATE NOT NULL,
                                       tema_id BIGINT NOT NULL,
                                       rodape_id BIGINT NOT NULL,
                                       FOREIGN KEY (tema_id) REFERENCES tema(id),
                                       FOREIGN KEY (rodape_id) REFERENCES rodape(id)
);

-- Tabela de relacionamento Projeto_Produto
CREATE TABLE IF NOT EXISTS projeto_produto (
                                               projeto_id BIGINT,
                                               produto_id BIGINT,
                                               PRIMARY KEY (projeto_id, produto_id),
                                               FOREIGN KEY (projeto_id) REFERENCES projeto(id),
                                               FOREIGN KEY (produto_id) REFERENCES produto(id)
);