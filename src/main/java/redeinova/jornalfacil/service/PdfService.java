package redeinova.jornalfacil.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redeinova.jornalfacil.model.Projeto;
import redeinova.jornalfacil.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redeinova.jornalfacil.util.ImageConverter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    private final ImageConverter imageConverter;

    public PdfService(ImageConverter imageConverter) {
        this.imageConverter = imageConverter;
    }

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.rodapes-dir}")
    private String rodapesDir;

    @Value("${file.placeholder-path:}")
    private String placeholderPath;

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        logger.info("Iniciando geração de encarte para o projeto ID: {}", projeto.getId());

        // Configuração do documento PDF
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            logger.debug("Configurando PDFWriter e abrindo documento");
            PdfWriter.getInstance(document, baos);
            document.open();

            // Adicionar tema como imagem de fundo
            logger.debug("Adicionando tema ao documento");
            adicionarTema(document, projeto);

            // Adicionar produtos em uma tabela 4x4
            logger.debug("Adicionando produtos ao documento");
            adicionarProdutos(document, projeto);

            // Adicionar rodapé
            logger.debug("Adicionando rodapé ao documento");
            adicionarRodape(document, projeto);

            logger.info("Encarte gerado com sucesso para o projeto ID: {}", projeto.getId());

            byte[] pdfBytes = baos.toByteArray();
            if (!isPdfValid(pdfBytes)) {
                throw new DocumentException("PDF gerado é inválido - falha na validação");
            }

            return pdfBytes;

        } catch (Exception e) {
            logger.error("Erro ao gerar encarte para o projeto ID: {}", projeto.getId(), e);
            throw e;
        } finally {
            if (document != null && document.isOpen()) {
                logger.debug("Fechando documento PDF");
                document.close();
            }
        }
    }

    private void adicionarTema(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getTema().getCaminhoImagem());
                String temaPath = temasDir + fileName;
                logger.debug("Carregando imagem do tema: {}", temaPath);

                // Verifica se o arquivo existe e é acessível primeiro
                if (!imageConverter.isImageAccessible(temaPath)) {
                    throw new IOException("Arquivo de tema não acessível: " + temaPath);
                }

                Image temaImage;

                // Verificar se é WebP e converter se necessário
                if (imageConverter.isWebpFormat(temaPath)) {
                    logger.debug("Convertendo imagem WebP do tema para PNG");
                    byte[] imageBytes = imageConverter.convertWebpToPng(temaPath);
                    temaImage = Image.getInstance(imageBytes);
                } else {
                    // Tenta carregar diretamente
                    temaImage = Image.getInstance(temaPath);
                }

                temaImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
                temaImage.setAbsolutePosition(0, 0);
                document.add(temaImage);

                logger.debug("Imagem do tema adicionada com sucesso");

            } catch (Exception e) {
                logger.warn("Falha ao carregar imagem do tema, usando fallback. Erro: {}", e.getMessage());

                // Fallback mais robusto - tenta carregar placeholder genérico
                try {
                    String placeholderPath = "classpath:/static/images/theme-placeholder.png";
                    Image placeholder = Image.getInstance(getClass().getResource(placeholderPath));
                    placeholder.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
                    placeholder.setAbsolutePosition(0, 0);
                    document.add(placeholder);
                    logger.debug("Placeholder de tema adicionado com sucesso");
                } catch (Exception ex) {
                    logger.warn("Falha ao carregar placeholder, usando texto alternativo");

                    // Fallback final para texto
                    Paragraph temaTexto = new Paragraph("Tema: " + projeto.getTema().getDescricao(),
                            FontFactory.getFont(FontFactory.HELVETICA, 24));
                    temaTexto.setAlignment(Element.ALIGN_CENTER);
                    document.add(temaTexto);
                }
            }
        } else {
            logger.warn("Projeto não possui tema definido ou caminho da imagem do tema é nulo");

            // Adicionar mensagem de fallback
            Paragraph fallback = new Paragraph("Tema não disponível",
                    FontFactory.getFont(FontFactory.HELVETICA, 18));
            fallback.setAlignment(Element.ALIGN_CENTER);
            document.add(fallback);
        }
    }

    private void adicionarProdutos(Document document, Projeto projeto) throws DocumentException {
        logger.debug("Preparando tabela de produtos (4 colunas)");
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20f);

        logger.info("Adicionando {} produtos ao encarte", projeto.getProdutos().size());

        if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
            logger.warn("Nenhum produto encontrado para o projeto ID: {}", projeto.getId());
            PdfPCell emptyCell = new PdfPCell(new Phrase("Nenhum produto selecionado",
                    FontFactory.getFont(FontFactory.HELVETICA, 14)));
            emptyCell.setColspan(4);
            emptyCell.setBorder(Rectangle.NO_BORDER);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
        } else {
            for (Produto produto : projeto.getProdutos()) {
                try {
                    PdfPCell cell = new PdfPCell();
                    cell.setBorder(Rectangle.NO_BORDER);
                    cell.setPadding(5);
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);

                    // Adicionar imagem do produto
                    adicionarImagemProduto(cell, produto);

                    // Adicionar preços
                    adicionarPrecosProduto(cell, produto);

                    // Adicionar descrição do produto (opcional)
                    adicionarDescricaoProduto(cell, produto);

                    table.addCell(cell);
                    logger.debug("Produto ID: {} adicionado à tabela", produto.getId());
                } catch (Exception e) {
                    logger.error("Erro ao adicionar produto ID: {} ao PDF. Erro: {}", produto.getId(), e.getMessage());
                    // Adiciona célula vazia para manter o layout
                    PdfPCell errorCell = new PdfPCell(new Phrase("Erro no produto",
                            FontFactory.getFont(FontFactory.HELVETICA, 10)));
                    errorCell.setBorder(Rectangle.NO_BORDER);
                    table.addCell(errorCell);
                }
            }
        }

        document.add(table);
        logger.debug("Tabela de produtos adicionada ao documento");
    }

    private void adicionarImagemProduto(PdfPCell cell, Produto produto) {
        if (produto.getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(produto.getCaminhoImagem());
                String imagePath = produtosDir + fileName;
                logger.debug("Carregando imagem do produto: {}", imagePath);

                Image productImage;

                // Verificar se é WebP e converter se necessário
                if (imageConverter.isWebpFormat(imagePath)) {
                    logger.debug("Convertendo imagem WebP do produto para PNG");
                    byte[] imageBytes = imageConverter.convertWebpToPng(imagePath);
                    productImage = Image.getInstance(imageBytes);
                } else {
                    // Verificar se o arquivo existe antes de tentar carregar
                    if (!new File(imagePath).exists()) {
                        throw new IOException("Arquivo não encontrado: " + imagePath);
                    }
                    productImage = Image.getInstance(imagePath);
                }

                productImage.scaleToFit(100, 100);
                productImage.setAlignment(Image.ALIGN_CENTER);

                // Criar parágrafo para centralizar a imagem
                Paragraph imageParagraph = new Paragraph();
                imageParagraph.setAlignment(Element.ALIGN_CENTER);
                imageParagraph.add(new Chunk(productImage, 0, 0));

                cell.addElement(imageParagraph);

                logger.debug("Imagem do produto adicionada com sucesso");
            } catch (Exception e) {
                logger.warn("Falha ao carregar imagem do produto ID: {}, tentando placeholder. Erro: {}",
                        produto.getId(), e.getMessage());
                logger.debug("Caminho tentado: {}", produtosDir + extractFileName(produto.getCaminhoImagem()));

                // Fallback para placeholder - verificar se o caminho está configurado
                try {
                    if (placeholderPath != null && !placeholderPath.trim().isEmpty() && new File(placeholderPath).exists()) {
                        logger.debug("Tentando carregar placeholder: {}", placeholderPath);
                        Image placeholder = Image.getInstance(placeholderPath);
                        placeholder.scaleToFit(100, 100);
                        placeholder.setAlignment(Image.ALIGN_CENTER);

                        Paragraph placeholderParagraph = new Paragraph();
                        placeholderParagraph.setAlignment(Element.ALIGN_CENTER);
                        placeholderParagraph.add(new Chunk(placeholder, 0, 0));

                        cell.addElement(placeholderParagraph);
                        logger.debug("Placeholder adicionado com sucesso para produto ID: {}", produto.getId());
                    } else {
                        throw new IOException("Placeholder não configurado ou não encontrado");
                    }
                } catch (Exception ex) {
                    logger.error("Falha ao carregar placeholder para produto ID: {}, usando texto alternativo. Erro: {}",
                            produto.getId(), ex.getMessage());

                    // Fallback para texto
                    Paragraph errorText = new Paragraph("[Imagem indisponível]",
                            FontFactory.getFont(FontFactory.HELVETICA, 10));
                    errorText.setAlignment(Element.ALIGN_CENTER);
                    cell.addElement(errorText);
                }
            }
        } else {
            logger.warn("Produto ID: {} não possui caminho de imagem definido", produto.getId());
            try {
                // Fallback para placeholder - verificar se o caminho está configurado
                if (placeholderPath != null && !placeholderPath.trim().isEmpty() && new File(placeholderPath).exists()) {
                    logger.debug("Usando placeholder para produto sem imagem definida");
                    Image placeholder = Image.getInstance(placeholderPath);
                    placeholder.scaleToFit(100, 100);
                    placeholder.setAlignment(Image.ALIGN_CENTER);

                    Paragraph placeholderParagraph = new Paragraph();
                    placeholderParagraph.setAlignment(Element.ALIGN_CENTER);
                    placeholderParagraph.add(new Chunk(placeholder, 0, 0));

                    cell.addElement(placeholderParagraph);
                } else {
                    throw new IOException("Placeholder não configurado ou não encontrado");
                }
            } catch (Exception ex) {
                logger.error("Falha ao carregar placeholder para produto ID: {}, usando texto alternativo. Erro: {}",
                        produto.getId(), ex.getMessage());

                Paragraph errorText = new Paragraph("[Sem imagem]",
                        FontFactory.getFont(FontFactory.HELVETICA, 10));
                errorText.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(errorText);
            }
        }
    }

    private void adicionarPrecosProduto(PdfPCell cell, Produto produto) {
        try {
            logger.debug("Formatando preços para o produto ID: {}", produto.getId());
            Paragraph precoParagraph = new Paragraph();
            precoParagraph.setAlignment(Element.ALIGN_CENTER);

            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
                Font deFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                deFont.setColor(100, 100, 100); // Cor cinza
                Chunk deChunk = new Chunk("De: " + formatCurrency(produto.getPrecoDe()) + "\n", deFont);
                precoParagraph.add(deChunk);
            }

            Font porFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            porFont.setColor(0, 0, 0); // Cor preta
            Chunk porChunk = new Chunk("Por: " + formatCurrency(produto.getPrecoPor()), porFont);
            precoParagraph.add(porChunk);

            cell.addElement(precoParagraph);
            logger.debug("Preços formatados e adicionados com sucesso para produto ID: {}", produto.getId());
        } catch (Exception e) {
            logger.error("Erro ao formatar preços para o produto ID: {}. Erro: {}", produto.getId(), e.getMessage());

            Paragraph errorParagraph = new Paragraph("[Preços indisponíveis]",
                    FontFactory.getFont(FontFactory.HELVETICA, 10));
            errorParagraph.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(errorParagraph);
        }
    }

    private void adicionarDescricaoProduto(PdfPCell cell, Produto produto) {
        try {
            // Adicionar descrição abreviada do produto
            if (produto.getDescricao() != null && !produto.getDescricao().isEmpty()) {
                String descricaoAbreviada = produto.getDescricao();
                if (descricaoAbreviada.length() > 30) {
                    descricaoAbreviada = descricaoAbreviada.substring(0, 27) + "...";
                }

                Paragraph descParagraph = new Paragraph(descricaoAbreviada,
                        FontFactory.getFont(FontFactory.HELVETICA, 8));
                descParagraph.setAlignment(Element.ALIGN_CENTER);
                cell.addElement(descParagraph);
            }
        } catch (Exception e) {
            logger.warn("Erro ao adicionar descrição do produto ID: {}", produto.getId(), e);
        }
    }

    private void adicionarRodape(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getRodape().getCaminhoImagem());
                String rodapePath = rodapesDir + fileName;
                logger.debug("Carregando imagem do rodapé: {}", rodapePath);

                Image rodapeImage;

                // Verificar se é WebP e converter se necessário
                if (imageConverter.isWebpFormat(rodapePath)) {
                    logger.debug("Convertendo imagem WebP do rodapé para PNG");
                    byte[] imageBytes = imageConverter.convertWebpToPng(rodapePath);
                    rodapeImage = Image.getInstance(imageBytes);
                } else {
                    rodapeImage = Image.getInstance(rodapePath);
                }

                rodapeImage.scaleToFit(document.getPageSize().getWidth(), 50);
                rodapeImage.setAbsolutePosition(0, 0);
                document.add(rodapeImage);

                logger.debug("Imagem do rodapé adicionada com sucesso");
            } catch (Exception e) {
                logger.warn("Falha ao carregar imagem do rodapé. Erro: {}", e.getMessage());
                // Não adicionamos fallback para rodapé para não poluir o layout
            }
        }
    }

    private String formatCurrency(BigDecimal value) {
        try {
            if (value == null) {
                return "R$ 0,00";
            }
            return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
        } catch (Exception e) {
            logger.error("Erro ao formatar valor monetário: {}. Retornando valor não formatado. Erro: {}",
                    value, e.getMessage());
            return value != null ? "R$ " + value.toString() : "R$ 0,00";
        }
    }

    private String extractFileName(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            logger.warn("Tentativa de extrair nome de arquivo de caminho nulo ou vazio");
            return "";
        }

        try {
            if (filePath.contains("/")) {
                return filePath.substring(filePath.lastIndexOf("/") + 1);
            }
            if (filePath.contains("\\")) {
                return filePath.substring(filePath.lastIndexOf("\\") + 1);
            }
            return filePath;
        } catch (Exception e) {
            logger.error("Erro ao extrair nome do arquivo do caminho: {}. Erro: {}", filePath, e.getMessage());
            return filePath; // Retorna o caminho original em caso de erro
        }
    }

    // Método para validar se o PDF foi gerado corretamente
    private boolean isPdfValid(byte[] pdfBytes) {
        try {
            if (pdfBytes == null || pdfBytes.length < 5) {
                logger.error("PDF inválido: bytes nulos ou muito curtos");
                return false;
            }

            // Verificar se começa com o header do PDF (%PDF-)
            String header = new String(pdfBytes, 0, 5);
            boolean isValid = "%PDF-".equals(header);

            if (!isValid) {
                logger.error("PDF inválido - header incorreto: {}", header);
            }

            return isValid;

        } catch (Exception e) {
            logger.error("Erro ao validar PDF", e);
            return false;
        }
    }

    // Método auxiliar para debug - verificar se arquivos existem
    private boolean arquivoExiste(String caminhoCompleto) {
        try {
            java.io.File file = new java.io.File(caminhoCompleto);
            boolean existe = file.exists() && file.isFile();
            if (!existe) {
                logger.warn("Arquivo não encontrado: {}", caminhoCompleto);
            }
            return existe;
        } catch (Exception e) {
            logger.error("Erro ao verificar existência do arquivo: {}. Erro: {}", caminhoCompleto, e.getMessage());
            return false;
        }
    }
}