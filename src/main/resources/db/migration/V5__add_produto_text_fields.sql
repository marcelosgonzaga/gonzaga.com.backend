-- Adicionar campos de texto ao produto
ALTER TABLE produto
    ADD COLUMN informacoes_obrigatorias VARCHAR(500) DEFAULT NULL,
    ADD COLUMN texto_legal VARCHAR(500) DEFAULT NULL;

-- Comentários para documentacao
COMMENT ON COLUMN produto.informacoes_obrigatorias IS 'Principio Ativo, Registro MS, Fabricante - ate 500 caracteres';
COMMENT ON COLUMN produto.texto_legal IS 'Texto legal do produto - ate 500 caracteres';