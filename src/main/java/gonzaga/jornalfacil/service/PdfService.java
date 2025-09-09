package gonzaga.jornalfacil.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import gonzaga.jornalfacil.model.Projeto;
import gonzaga.jornalfacil.model.Produto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.rodapes-dir}")
    private String rodapesDir;

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        logger.info("Iniciando geração de encarte para o projeto ID: {}", projeto.getId());
        logger.debug("Dados do projeto: tema={}, produtos={}, datas={} a {}",
                projeto.getTema() != null ? projeto.getTema().getId() : "null",
                projeto.getProdutos() != null ? projeto.getProdutos().size() : 0,
                projeto.getDataInicio(),
                projeto.getDataFim());

        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            adicionarTema(document, projeto);
            adicionarProdutos(document, projeto);
            adicionarRodape(document, projeto);
            adicionarDatasValidade(document, projeto);

            logger.info("Encarte gerado com sucesso para o projeto ID: {}", projeto.getId());

            return baos.toByteArray();

        } catch (Exception e) {
            logger.error("Erro ao gerar encarte para o projeto ID: {}", projeto.getId(), e);
            throw e;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
    }

    private void adicionarTema(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getTema().getCaminhoImagem());
                String temaPath = temasDir + fileName;

                Image temaImage = Image.getInstance(temaPath);
                temaImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
                temaImage.setAbsolutePosition(0, 0);
                document.add(temaImage);
                logger.debug("Imagem do tema adicionada com sucesso");

            } catch (Exception e) {
                logger.warn("Falha ao carregar tema: {}", e.getMessage());
            }
        }
    }

    private void adicionarProdutos(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getProdutos() != null && !projeto.getProdutos().isEmpty()) {
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(75);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.setSpacingBefore(150f);

            float[] columnWidths = {1f, 1f, 1f, 1f};
            table.setWidths(columnWidths);

            for (int i = 0; i < 16; i++) {
                PdfPCell cell = new PdfPCell();
                cell.setBorder(PdfPCell.NO_BORDER);
                cell.setPadding(5);
                cell.setFixedHeight(150);

                if (i < projeto.getProdutos().size()) {
                    Produto produto = projeto.getProdutos().get(i);
                    adicionarProdutoNaCelula(cell, produto);
                }

                table.addCell(cell);
            }

            document.add(table);
        }
    }

    private void adicionarProdutoNaCelula(PdfPCell cell, Produto produto) {
        try {
            // Container principal
            PdfPTable container = new PdfPTable(1);
            container.setWidthPercentage(100);

            // Imagem do produto
            if (produto.getCaminhoImagem() != null) {
                try {
                    String fileName = extractFileName(produto.getCaminhoImagem());
                    String imagePath = produtosDir + fileName;

                    Image productImage = Image.getInstance(imagePath);
                    productImage.scaleToFit(70, 70);

                    PdfPCell imageCell = new PdfPCell(productImage);
                    imageCell.setBorder(PdfPCell.NO_BORDER);
                    imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    imageCell.setPaddingBottom(5);

                    container.addCell(imageCell);
                } catch (Exception e) {
                    logger.warn("Falha ao carregar imagem do produto: {}", e.getMessage());
                }
            }

            // Box de preços (replicando o estilo do frontend)
            PdfPTable priceTable = new PdfPTable(1);
            priceTable.setWidthPercentage(100);

            PdfPCell priceCell = new PdfPCell();
            priceCell.setBorder(PdfPCell.NO_BORDER);
            priceCell.setBackgroundColor(new Color(220, 38, 38)); // Vermelho similar ao frontend
            priceCell.setPadding(5);
            priceCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            // Adicionar preços formatados
            Paragraph priceParagraph = new Paragraph();
            priceParagraph.setAlignment(Element.ALIGN_CENTER);

            // Preço De (se existir e for maior que zero)
            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
                Font deFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
                deFont.setColor(Color.WHITE);
                Paragraph deParagraph = new Paragraph("De: " + formatCurrency(produto.getPrecoDe()), deFont);
                deParagraph.setAlignment(Element.ALIGN_LEFT);
                priceCell.addElement(deParagraph);
            }

            // Preço Por
            Font porFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            porFont.setColor(Color.WHITE);
            Paragraph porParagraph = new Paragraph("R$ " + formatCurrencySimple(produto.getPrecoPor()), porFont);
            porParagraph.setAlignment(Element.ALIGN_CENTER);
            priceCell.addElement(porParagraph);

            // Texto "a unid."
            Font unitFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            unitFont.setColor(Color.WHITE);
            Paragraph unitParagraph = new Paragraph("a unid.", unitFont);
            unitParagraph.setAlignment(Element.ALIGN_RIGHT);
            priceCell.addElement(unitParagraph);

            priceTable.addCell(priceCell);
            container.addCell(priceTable);

            cell.addElement(container);

        } catch (Exception e) {
            logger.error("Erro ao adicionar produto na célula: {}", e.getMessage());
        }
    }

    private void adicionarDatasValidade(Document document, Projeto projeto) {
        if (projeto.getDataInicio() != null && projeto.getDataFim() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                String textoDatas = String.format("Ofertas válidas de %s até %s ou enquanto durarem os estoques",
                        projeto.getDataInicio().format(formatter),
                        projeto.getDataFim().format(formatter));

                Paragraph datasParagraph = new Paragraph(textoDatas,
                        FontFactory.getFont(FontFactory.HELVETICA, 10));
                datasParagraph.setAlignment(Element.ALIGN_CENTER);
                datasParagraph.setSpacingBefore(20f);

                document.add(datasParagraph);

            } catch (Exception e) {
                logger.warn("Erro ao adicionar datas de validade: {}", e.getMessage());
            }
        }
    }

    private void adicionarRodape(Document document, Projeto projeto) {
        if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getRodape().getCaminhoImagem());
                String rodapePath = rodapesDir + fileName;

                Image rodapeImage = Image.getInstance(rodapePath);
                rodapeImage.scaleToFit(200, 50);
                rodapeImage.setAlignment(Image.ALIGN_CENTER);

                document.add(rodapeImage);

            } catch (Exception e) {
                logger.warn("Falha ao carregar rodapé: {}", e.getMessage());
            }
        }
    }

    private String formatCurrency(BigDecimal value) {
        try {
            if (value == null) return "0,00";
            return NumberFormat.getCurrencyInstance(new Locale("pt", "BR"))
                    .format(value)
                    .replace("R$", "")
                    .trim();
        } catch (Exception e) {
            return value != null ? value.toString() : "0,00";
        }
    }

    private String formatCurrencySimple(BigDecimal value) {
        try {
            if (value == null) return "0,00";
            String formatted = formatCurrency(value);
            // Formato simplificado: "19,90" em vez de "R$ 19,90"
            return formatted;
        } catch (Exception e) {
            return "0,00";
        }
    }

    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        if (filePath.contains("/")) return filePath.substring(filePath.lastIndexOf("/") + 1);
        if (filePath.contains("\\")) return filePath.substring(filePath.lastIndexOf("\\") + 1);
        return filePath;
    }
}




//package gonzaga.jornalfacil.service;
//
//import com.lowagie.text.*;
//import com.lowagie.text.pdf.PdfPCell;
//import com.lowagie.text.pdf.PdfPTable;
//import com.lowagie.text.pdf.PdfWriter;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import gonzaga.jornalfacil.model.Projeto;
//import gonzaga.jornalfacil.model.Produto;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import gonzaga.jornalfacil.util.ImageConverter;
//
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.text.NumberFormat;
//import java.util.Locale;
//import java.math.BigDecimal;
//
//@Service
//public class PdfService {
//
//    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
//    private final ImageConverter imageConverter;
//
//    public PdfService(ImageConverter imageConverter) {
//        this.imageConverter = imageConverter;
//    }
//
//    @Value("${file.temas-dir}")
//    private String temasDir;
//
//    @Value("${file.produtos-dir}")
//    private String produtosDir;
//
//    @Value("${file.rodapes-dir}")
//    private String rodapesDir;
//
//    @Value("${file.placeholder-path:}")
//    private String placeholderPath;
//
//    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
//        logger.info("Iniciando geração de encarte para o projeto ID: {}", projeto.getId());
//
//        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//        try {
//            PdfWriter.getInstance(document, baos);
//            document.open();
//
//            adicionarTema(document, projeto);
//            adicionarProdutos(document, projeto);
//            adicionarRodape(document, projeto);
//
//            logger.info("Encarte gerado com sucesso para o projeto ID: {}", projeto.getId());
//
//            byte[] pdfBytes = baos.toByteArray();
//            if (!isPdfValid(pdfBytes)) {
//                throw new DocumentException("PDF gerado é inválido");
//            }
//
//            return pdfBytes;
//
//        } catch (Exception e) {
//            logger.error("Erro ao gerar encarte para o projeto ID: {}", projeto.getId(), e);
//            throw e;
//        } finally {
//            if (document != null && document.isOpen()) {
//                document.close();
//            }
//        }
//    }
//
//    private void adicionarTema(Document document, Projeto projeto) throws DocumentException {
//        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
//            try {
//                String fileName = extractFileName(projeto.getTema().getCaminhoImagem());
//                String temaPath = temasDir + fileName;
//
//                // Se for WebP, tentar usar PNG equivalente
//                if (fileName.toLowerCase().endsWith(".webp")) {
//                    String pngPath = temaPath.replace(".webp", ".png");
//                    if (new File(pngPath).exists()) {
//                        temaPath = pngPath;
//                        logger.debug("Usando PNG equivalente para tema WebP: {}", pngPath);
//                    }
//                }
//
//                Image temaImage = Image.getInstance(temaPath);
//                temaImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
//                temaImage.setAbsolutePosition(0, 0);
//                document.add(temaImage);
//                logger.debug("Imagem do tema adicionada com sucesso");
//
//            } catch (Exception e) {
//                logger.warn("Falha ao carregar tema, usando fallback: {}", e.getMessage());
//                // Fallback para texto
//                Paragraph fallback = new Paragraph("Tema: " + (projeto.getTema() != null ?
//                        projeto.getTema().getDescricao() : "Não disponível"),
//                        FontFactory.getFont(FontFactory.HELVETICA, 18));
//                fallback.setAlignment(Element.ALIGN_CENTER);
//                document.add(fallback);
//            }
//        }
//    }
//
//    private void adicionarProdutos(Document document, Projeto projeto) throws DocumentException {
//        PdfPTable table = new PdfPTable(4);
//        table.setWidthPercentage(100);
//        table.setSpacingBefore(20f);
//
//        if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
//            PdfPCell emptyCell = new PdfPCell(new Phrase("Nenhum produto selecionado",
//                    FontFactory.getFont(FontFactory.HELVETICA, 14)));
//            emptyCell.setColspan(4);
//            emptyCell.setBorder(Rectangle.NO_BORDER);
//            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
//            table.addCell(emptyCell);
//        } else {
//            for (Produto produto : projeto.getProdutos()) {
//                try {
//                    PdfPCell cell = new PdfPCell();
//                    cell.setBorder(Rectangle.NO_BORDER);
//                    cell.setPadding(5);
//                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
//                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
//
//                    adicionarImagemProduto(cell, produto);
//                    adicionarPrecosProduto(cell, produto);
//                    adicionarDescricaoProduto(cell, produto);
//
//                    table.addCell(cell);
//                } catch (Exception e) {
//                    PdfPCell errorCell = new PdfPCell(new Phrase("Erro no produto",
//                            FontFactory.getFont(FontFactory.HELVETICA, 10)));
//                    errorCell.setBorder(Rectangle.NO_BORDER);
//                    table.addCell(errorCell);
//                }
//            }
//        }
//
//        document.add(table);
//    }
//
//    private void adicionarImagemProduto(PdfPCell cell, Produto produto) {
//        if (produto.getCaminhoImagem() != null) {
//            try {
//                String fileName = extractFileName(produto.getCaminhoImagem());
//                String imagePath = produtosDir + fileName;
//
//                // Se for WebP, tentar usar PNG equivalente
//                if (fileName.toLowerCase().endsWith(".webp")) {
//                    String pngPath = imagePath.replace(".webp", ".png");
//                    if (new File(pngPath).exists()) {
//                        imagePath = pngPath;
//                        logger.debug("Usando PNG equivalente para produto WebP: {}", pngPath);
//                    }
//                }
//
//                Image productImage = Image.getInstance(imagePath);
//                productImage.scaleToFit(100, 100);
//                productImage.setAlignment(Image.ALIGN_CENTER);
//
//                Paragraph imageParagraph = new Paragraph();
//                imageParagraph.setAlignment(Element.ALIGN_CENTER);
//                imageParagraph.add(new Chunk(productImage, 0, 0));
//                cell.addElement(imageParagraph);
//
//            } catch (Exception e) {
//                logger.warn("Falha ao carregar imagem do produto, usando placeholder");
//                adicionarPlaceholder(cell);
//            }
//        } else {
//            adicionarPlaceholder(cell);
//        }
//    }
//
//    private void adicionarPlaceholder(PdfPCell cell) {
//        try {
//            if (placeholderPath != null && new File(placeholderPath).exists()) {
//                Image placeholder = Image.getInstance(placeholderPath);
//                placeholder.scaleToFit(100, 100);
//                placeholder.setAlignment(Image.ALIGN_CENTER);
//
//                Paragraph placeholderParagraph = new Paragraph();
//                placeholderParagraph.setAlignment(Element.ALIGN_CENTER);
//                placeholderParagraph.add(new Chunk(placeholder, 0, 0));
//                cell.addElement(placeholderParagraph);
//            } else {
//                throw new IOException("Placeholder não disponível");
//            }
//        } catch (Exception e) {
//            Paragraph errorText = new Paragraph("[Sem imagem]",
//                    FontFactory.getFont(FontFactory.HELVETICA, 10));
//            errorText.setAlignment(Element.ALIGN_CENTER);
//            cell.addElement(errorText);
//        }
//    }
//
//    private void adicionarPrecosProduto(PdfPCell cell, Produto produto) {
//        try {
//            Paragraph precoParagraph = new Paragraph();
//            precoParagraph.setAlignment(Element.ALIGN_CENTER);
//
//            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
//                Font deFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
//                deFont.setColor(100, 100, 100);
//                Chunk deChunk = new Chunk("De: " + formatCurrency(produto.getPrecoDe()) + "\n", deFont);
//                precoParagraph.add(deChunk);
//            }
//
//            Font porFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
//            porFont.setColor(0, 0, 0);
//            Chunk porChunk = new Chunk("Por: " + formatCurrency(produto.getPrecoPor()), porFont);
//            precoParagraph.add(porChunk);
//
//            cell.addElement(precoParagraph);
//        } catch (Exception e) {
//            Paragraph errorParagraph = new Paragraph("[Preços indisponíveis]",
//                    FontFactory.getFont(FontFactory.HELVETICA, 10));
//            errorParagraph.setAlignment(Element.ALIGN_CENTER);
//            cell.addElement(errorParagraph);
//        }
//    }
//
//    private void adicionarDescricaoProduto(PdfPCell cell, Produto produto) {
//        try {
//            if (produto.getDescricao() != null && !produto.getDescricao().isEmpty()) {
//                String descricaoAbreviada = produto.getDescricao();
//                if (descricaoAbreviada.length() > 30) {
//                    descricaoAbreviada = descricaoAbreviada.substring(0, 27) + "...";
//                }
//
//                Paragraph descParagraph = new Paragraph(descricaoAbreviada,
//                        FontFactory.getFont(FontFactory.HELVETICA, 8));
//                descParagraph.setAlignment(Element.ALIGN_CENTER);
//                cell.addElement(descParagraph);
//            }
//        } catch (Exception e) {
//            // Ignorar erro na descrição
//        }
//    }
//
//    private void adicionarRodape(Document document, Projeto projeto) throws DocumentException {
//        if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
//            try {
//                String fileName = extractFileName(projeto.getRodape().getCaminhoImagem());
//                String rodapePath = rodapesDir + fileName;
//
//                // Se for WebP, tentar usar PNG equivalente
//                if (fileName.toLowerCase().endsWith(".webp")) {
//                    String pngPath = rodapePath.replace(".webp", ".png");
//                    if (new File(pngPath).exists()) {
//                        rodapePath = pngPath;
//                    }
//                }
//
//                Image rodapeImage = Image.getInstance(rodapePath);
//                rodapeImage.scaleToFit(document.getPageSize().getWidth(), 50);
//                rodapeImage.setAbsolutePosition(0, 0);
//                document.add(rodapeImage);
//
//            } catch (Exception e) {
//                logger.warn("Falha ao carregar rodapé: {}", e.getMessage());
//            }
//        }
//    }
//
//    private String formatCurrency(BigDecimal value) {
//        try {
//            if (value == null) return "R$ 0,00";
//            return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
//        } catch (Exception e) {
//            return value != null ? "R$ " + value.toString() : "R$ 0,00";
//        }
//    }
//
//    private String extractFileName(String filePath) {
//        if (filePath == null) return "";
//        if (filePath.contains("/")) return filePath.substring(filePath.lastIndexOf("/") + 1);
//        if (filePath.contains("\\")) return filePath.substring(filePath.lastIndexOf("\\") + 1);
//        return filePath;
//    }
//
//    private boolean isPdfValid(byte[] pdfBytes) {
//        try {
//            if (pdfBytes == null || pdfBytes.length < 5) return false;
//            String header = new String(pdfBytes, 0, 5);
//            return "%PDF-".equals(header);
//        } catch (Exception e) {
//            return false;
//        }
//    }
//}