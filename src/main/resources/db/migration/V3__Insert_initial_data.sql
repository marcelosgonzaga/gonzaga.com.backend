-- Inserir Temas
INSERT INTO tema (descricao, caminho_imagem) VALUES
                                                 ('Tema 01 - Padrão', '/imagens/temas/JORNAL01A.jpg'),
                                                 ('Tema 02 - Esportivo', '/imagens/temas/JORNAL02A.jpg'),
                                                 ('Tema 03 - Elegante', '/imagens/temas/JORNAL03A.jpg'),
                                                 ('Tema 04 - Padrão', '/imagens/temas/JORNAL04A.jpg'),
                                                 ('Tema 05 - Esportivo', '/imagens/temas/JORNAL05A.jpg'),
                                                 ('Tema 06 - Elegante', '/imagens/temas/JORNAL06A.jpg'),
                                                 ('Tema 07 - Padrão', '/imagens/temas/JORNAL07A.jpg'),
                                                 ('Tema 08 - Esportivo', '/imagens/temas/JORNAL08A.jpg'),
                                                 ('Tema 09 - Elegante', '/imagens/temas/JORNAL09A.jpg'),
                                                 ('Tema 10 - Padrão', '/imagens/temas/JORNAL10A.jpg'),
                                                 ('Tema 11 - Esportivo', '/imagens/temas/JORNAL11A.jpg'),
                                                 ('Tema 12 - Elegante', '/imagens/temas/JORNAL12A.jpg'),
                                                 ('Tema 13 - Padrão', '/imagens/temas/JORNAL13A.jpg'),
                                                 ('Tema 14 - Esportivo', '/imagens/temas/JORNAL14A.jpg'),
                                                 ('Tema 15 - Festivo', '/imagens/temas/JORNAL15A.jpg');

-- Inserir Rodapés
INSERT INTO rodape (codigo_loja, caminho_imagem) VALUES
                                                     (1, '/imagens/rodapes/1.png'),
                                                     (2, '/imagens/rodapes/2.png'),
                                                     (3, '/imagens/rodapes/3.png'),
                                                     (4, '/imagens/rodapes/4.png'),
                                                     (5, '/imagens/rodapes/5.png'),
                                                     (6, '/imagens/rodapes/6.png'),
                                                     (7, '/imagens/rodapes/7.png'),
                                                     (8, '/imagens/rodapes/8.png'),
                                                     (9, '/imagens/rodapes/9.png'),
                                                     (10, '/imagens/rodapes/10.png'),
                                                     (50, '/imagens/rodapes/50.png'),
                                                     (51, '/imagens/rodapes/51.png'),
                                                     (52, '/imagens/rodapes/52.png'),
                                                     (53, '/imagens/rodapes/53.png'),
                                                     (54, '/imagens/rodapes/54.png'),
                                                     (55, '/imagens/rodapes/55.png'),
                                                     (56, '/imagens/rodapes/56.png'),
                                                     (57, '/imagens/rodapes/57.png'),
                                                     (58, '/imagens/rodapes/58.png'),
                                                     (59, '/imagens/rodapes/59.png'),
                                                     (60, '/imagens/rodapes/60.png'),
                                                     (500, '/imagens/rodapes/500.png'),
                                                     (501, '/imagens/rodapes/501.png'),
                                                     (502, '/imagens/rodapes/502.png'),
                                                     (503, '/imagens/rodapes/503.png'),
                                                     (504, '/imagens/rodapes/504.png'),
                                                     (505, '/imagens/rodapes/505.png'),
                                                     (506, '/imagens/rodapes/506.png'),
                                                     (507, '/imagens/rodapes/507.png'),
                                                     (508, '/imagens/rodapes/508.png'),
                                                     (509, '/imagens/rodapes/509.png'),
                                                     (510, '/imagens/rodapes/510.png'),
                                                     (1000, '/imagens/rodapes/1000.png'),
                                                     (1001, '/imagens/rodapes/1001.png'),
                                                     (1002, '/imagens/rodapes/1002.png'),
                                                     (1003, '/imagens/rodapes/1003.png'),
                                                     (1004, '/imagens/rodapes/1004.png'),
                                                     (1005, '/imagens/rodapes/1005.png'),
                                                     (1006, '/imagens/rodapes/1006.png'),
                                                     (1007, '/imagens/rodapes/1007.png'),
                                                     (1008, '/imagens/rodapes/1008.png'),
                                                     (1009, '/imagens/rodapes/1009.png'),
                                                     (1010, '/imagens/rodapes/1010.png');

-- Inserir Produtos (16 produtos)
INSERT INTO produto (ean, descricao, preco_de, preco_por, classificacao, caminho_imagem) VALUES
-- Medicamentos
(7896016805566, 'Dipirona 500mg 10 comprimidos', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/7896016805566.png'),
(7896472901345, 'Ibuprofeno 400mg 10 comprimidos', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/7896472901345.png'),
(7898109241123, 'Omeprazol 20mg 14 cápsulas', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/7898109241123.png'),
(7896658004123, 'Losartana 50mg 30 comprimidos', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/7896658004123.png'),
(7897086200111, 'Sinvastatina 20mg 30 comprimidos', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/7897086200111.png'),
(7896006200145, 'Shampoo Anticaspa 200ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7896006200145.png'),
(7896035790023, 'Condicionador Hidratante 200ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7896035790023.png'),
(7896035790024, 'Maleato de Enalapril', 00.00, 00.00, 'MEDICAMENTO', '/imagens/produtos/07896181901068.png'),

-- Perfumaria
(7891234567890, 'Desodorante Aerosol 150ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7891234567890.png'),
(7890987654321, 'Sabonete Líquido 200ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7890987654321.png'),
(7891122334455, 'Creme Dental 90g', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7891122334455.png'),
(7895544332211, 'Protetor Solar FPS 30 120ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7895544332211.png'),
(7896677889900, 'Hidratante Corporal 200ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7896677889900.png'),
(7890011223344, 'Perfume 50ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7890011223344.png'),
(7899988776655, 'Água Micelar 200ml', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7899988776655.png'),
(7894433221100, 'Lenços Umedecidos 30 unidades', 00.00, 00.00, 'PERFUMARIA', '/imagens/produtos/7894433221100.png');

-- Inserir Projeto de exemplo
INSERT INTO projeto (data_inicio, data_fim, tema_id, rodape_id) VALUES
    ('2023-11-01', '2023-11-30', 1, 1);

-- Relacionar produtos ao projeto
# INSERT INTO projeto_produto (projeto_id, produto_id) VALUES
#                                                          (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7), (1, 8),
#                                                          (1, 9), (1, 10), (1, 11), (1, 12), (1, 13), (1, 14), (1, 15), (1, 16);

INSERT INTO projeto_produto (projeto_id, produto_id)
SELECT 1, p.id
FROM produto p
WHERE p.ean IN (
                7896016805566, 7896472901345, 7898109241123, 7896658004123,
                7897086200111, 7896006200145, 7896035790023, 7891234567890,
                7890987654321, 7891122334455, 7895544332211, 7896677889900,
                7890011223344, 7899988776655, 7894433221100, 7896035790024
    );