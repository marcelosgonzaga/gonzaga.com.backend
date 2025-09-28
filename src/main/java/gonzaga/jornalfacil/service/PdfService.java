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

            // Converter para double e formatar corretamente
            double valorDouble = value.doubleValue() / 100.0; // Assume que o valor está em centavos

            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);

            return formatter.format(valorDouble);
        } catch (Exception e) {
            logger.error("Erro ao formatar valor: {}", value, e);
            return "0,00";
        }
    }

    private String formatCurrencySimple(BigDecimal value) {
        try {
            if (value == null) return "0,00";
            return formatCurrency(value);
        } catch (Exception e) {
            logger.error("Erro ao formatar valor simples: {}", value, e);
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

