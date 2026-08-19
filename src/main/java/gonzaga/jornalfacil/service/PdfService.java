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

    // CONSTANTES COM AS MESMAS DIMENSÕES DO IMAGESERVICE
    private static final float PDF_WIDTH = 2480f;
    private static final float PDF_HEIGHT = 3508f;

    // CRIAR PAGESIZE CUSTOMIZADO
    //private static final PageSize PAGE_SIZE_A4_HIGH_RES = new PageSize(PDF_WIDTH, PDF_HEIGHT);

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.rodapes-dir}")
    private String rodapesDir;
    private PdfWriter writer;

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        logger.info("Iniciando geração de encarte para o projeto ID: {}", projeto.getId());

        // USAR RECTANGLE DIRETAMENTE (abordagem alternativa)
        Document document = new Document(new Rectangle(PDF_WIDTH, PDF_HEIGHT), 40, 40, 60, 60);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            document.open();

            this.writer = writer;

            adicionarTema(document, projeto);
            adicionarDatasValidade(document, projeto);
            adicionarProdutos(document, projeto);
            adicionarRodape(document, projeto);

            logger.info("Encarte gerado com sucesso para o projeto ID: {}", projeto.getId());

        } catch (Exception e) {
            logger.error("Erro ao gerar encarte para o projeto ID: {}", projeto.getId(), e);
            throw e;
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }

        return baos.toByteArray();
    }

    private void adicionarTema(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getTema().getCaminhoImagem());
                String temaPath = temasDir + fileName;

                File file = new File(temaPath);
                if (!file.exists()) {
                    logger.warn("Arquivo de tema não encontrado: {}", temaPath);
                    return;
                }

                Image temaImage = Image.getInstance(temaPath);
                // AJUSTE: Escalar para as dimensões de alta resolução
                temaImage.scaleToFit(PDF_WIDTH - 80, PDF_HEIGHT - 80);
                temaImage.setAbsolutePosition(
                        (PDF_WIDTH - temaImage.getScaledWidth()) / 2,
                        (PDF_HEIGHT - temaImage.getScaledHeight()) / 2
                );
                document.add(temaImage);
                logger.debug("Imagem do tema adicionada com sucesso");

            } catch (Exception e) {
                logger.warn("Falha ao carregar tema: {}", e.getMessage());
            }
        }
    }

    private void adicionarProdutos(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getProdutos() != null && !projeto.getProdutos().isEmpty()) {
            // Criar a tabela de produtos
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setHorizontalAlignment(Element.ALIGN_CENTER);

            // DEFINIR LARGURA ABSOLUTA PARA ALTA RESOLUÇÃO
            table.setTotalWidth(PDF_WIDTH - 160f); // Margens proporcionais
            table.setLockedWidth(true);

            float[] columnWidths = {1f, 1f, 1f, 1f};
            table.setWidths(columnWidths);

            // ALTURA DAS CÉLULAS PROPORCIONAL À ALTA RESOLUÇÃO
            float cellHeight = 480f; // Aproximadamente 1/10 da altura total 480f

            for (int i = 0; i < 16; i++) {
                PdfPCell cell = new PdfPCell();
                cell.setBorder(PdfPCell.NO_BORDER);
                cell.setPadding(20); // espaço entre os box 20
                cell.setFixedHeight(cellHeight);

                if (i < projeto.getProdutos().size()) {
                    Produto produto = projeto.getProdutos().get(i);
                    adicionarProdutoNaCelula(cell, produto);
                } else {
                    cell.addElement(new Paragraph(" "));
                }
                table.addCell(cell);
            }

            // POSICIONAMENTO MANUAL ABSOLUTO - AJUSTADO PARA ALTA RESOLUÇÃO
            // Posicionar a ~35% do topo (mesmo posicionamento do ImageService)
            float yPosition = PDF_HEIGHT * 0.65f; // 3508 * 0.65 = ~2280px do TOPO

            // Calcular posição X para centralizar
            float xPosition = (PDF_WIDTH - table.getTotalWidth()) / 2;

            // POSICIONAMENTO ABSOLUTO
            PdfContentByte canvas = writer.getDirectContent();
            canvas.saveState();

            table.writeSelectedRows(
                    0,
                    -1,
                    xPosition,
                    yPosition, // TOPO da tabela
                    canvas
            );

            canvas.restoreState();

            logger.debug("Tabela de produtos posicionada em X: {}, Y: {}", xPosition, yPosition);
        }
    }

    private void adicionarProdutoNaCelula(PdfPCell cell, Produto produto) {
        try {
            // Container principal
            PdfPTable container = new PdfPTable(1);
            container.setWidthPercentage(100);
            container.setHorizontalAlignment(Element.ALIGN_CENTER);
            container.setSpacingBefore(0f);
            container.setSpacingAfter(0f);

            // Imagem do produto
            if (produto.getCaminhoImagem() != null) {
                try {
                    String fileName = extractFileName(produto.getCaminhoImagem());
                    String imagePath = produtosDir + fileName;

                    File file = new File(imagePath);
                    if (file.exists()) {
                        Image productImage = Image.getInstance(imagePath);
                        productImage.scaleToFit(350, 350);

                        PdfPCell imageCell = new PdfPCell(productImage);
                        imageCell.setBorder(PdfPCell.NO_BORDER);
                        imageCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        imageCell.setPaddingBottom(10);
                        imageCell.setFixedHeight(310f);

                        container.addCell(imageCell);
                    } else {
                        logger.warn("Imagem do produto não encontrada: {}", imagePath);
                        PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
                        emptyCell.setBorder(PdfPCell.NO_BORDER);
                        emptyCell.setFixedHeight(310f);
                        container.addCell(emptyCell);
                    }
                } catch (Exception e) {
                    logger.warn("Falha ao carregar imagem do produto: {}", e.getMessage());
                    PdfPCell emptyCell = new PdfPCell(new Phrase(" "));
                    emptyCell.setBorder(PdfPCell.NO_BORDER);
                    emptyCell.setFixedHeight(310f);
                    container.addCell(emptyCell);
                }
            }

            // Box de preços - ESTRUTURA SIMPLIFICADA
            PdfPTable priceTable = new PdfPTable(1);
            priceTable.setWidthPercentage(100);
            priceTable.setSpacingBefore(0f);
            priceTable.setSpacingAfter(0f);

            PdfPCell priceCell = new PdfPCell();
            priceCell.setBorder(PdfPCell.NO_BORDER);
            priceCell.setBackgroundColor(new Color(220, 38, 38));
            priceCell.setPadding(8);
            priceCell.setFixedHeight(100f); // Altura reduzida para melhor ajuste
            priceCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            priceCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

            // Container principal dentro da célula de preço
            PdfPTable innerTable = new PdfPTable(1);
            innerTable.setWidthPercentage(100);
            innerTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            // LINHA 1: Preço De (se existir)
            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
                Font deFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE);
                Paragraph deParagraph = new Paragraph("De R$ " + formatCurrency(produto.getPrecoDe()) + " por:", deFont);
                deParagraph.setAlignment(Element.ALIGN_LEFT);
                deParagraph.setSpacingAfter(5f);
                innerTable.addCell(createCell(deParagraph, PdfPCell.NO_BORDER, null, 0));
            }

            // LINHA 2: Preço principal - USANDO Phrase DIRETO
            String precoPorText = formatCurrency(produto.getPrecoPor());
            String valorFormatado = precoPorText.replace("R$", "").trim();
            String[] partes = valorFormatado.split(",");
            String parteInteira = partes[0];
            String parteDecimal = partes.length > 1 ? "," + partes[1] : ",00";

            // Criar o preço principal como uma única string formatada
            Font rsFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);
            Font mainFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 32, Color.WHITE);
            Font centsFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, Color.WHITE);

            // Combinar tudo em um único Paragraph
            Paragraph priceParagraph = new Paragraph();
            priceParagraph.setAlignment(Element.ALIGN_CENTER);

            // Adicionar R$
            Chunk rsChunk = new Chunk("R$ ", rsFont);
            priceParagraph.add(rsChunk);

            // Adicionar valor principal
            Chunk mainChunk = new Chunk(parteInteira, mainFont);
            priceParagraph.add(mainChunk);

            // Adicionar centavos
            Chunk centsChunk = new Chunk(parteDecimal, centsFont);
            priceParagraph.add(centsChunk);

            priceParagraph.setSpacingAfter(5f);
            innerTable.addCell(createCell(priceParagraph, PdfPCell.NO_BORDER, null, 0));

            // LINHA 3: "a unid."
            Font unitFont = FontFactory.getFont(FontFactory.HELVETICA, 14, Font.NORMAL, Color.WHITE);
            Paragraph unitParagraph = new Paragraph("a unid.", unitFont);
            unitParagraph.setAlignment(Element.ALIGN_RIGHT);
            innerTable.addCell(createCell(unitParagraph, PdfPCell.NO_BORDER, null, 0));

            priceCell.addElement(innerTable);
            priceTable.addCell(priceCell);
            container.addCell(priceTable);

            cell.addElement(container);

        } catch (Exception e) {
            logger.error("Erro ao adicionar produto na célula: {}", e.getMessage());
            // Fallback simples
            Font fallbackFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.BLACK);
            Paragraph fallback = new Paragraph("R$ " + formatCurrency(produto.getPrecoPor()), fallbackFont);
            fallback.setAlignment(Element.ALIGN_CENTER);
            cell.addElement(fallback);
        }
    }

    // Método auxiliar para criar células consistentes
    private PdfPCell createCell(Element element, int border, Color backgroundColor, float padding) {
        PdfPCell cell = new PdfPCell();
        cell.addElement(element);
        cell.setBorder(border);
        if (backgroundColor != null) {
            cell.setBackgroundColor(backgroundColor);
        }
        cell.setPadding(padding);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        return cell;
    }
    private void adicionarDatasValidade(Document document, Projeto projeto) {
        if (projeto.getDataInicio() != null && projeto.getDataFim() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                String textoDatas = String.format("Ofertas válidas de %s até %s ou enquanto durarem os estoques",
                        projeto.getDataInicio().format(formatter),
                        projeto.getDataFim().format(formatter));

                // FONTE AUMENTADA PARA ALTA RESOLUÇÃO (equivalente ao ImageService)
                Font datasFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 35, Color.BLACK); // Aumentado de 10 para 35

                Paragraph datasParagraph = new Paragraph(textoDatas, datasFont);
                datasParagraph.setAlignment(Element.ALIGN_CENTER);

                // POSICIONAMENTO EQUIVALENTE AO IMAGESERVICE (~33% do topo)
                float yPosition = PDF_HEIGHT * 0.67f; // 3508 * 0.67 = ~2350px

                ColumnText.showTextAligned(
                        writer.getDirectContent(),
                        Element.ALIGN_CENTER,
                        datasParagraph,
                        PDF_WIDTH / 2,
                        yPosition,
                        0
                );

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

                File file = new File(rodapePath);
                if (file.exists()) {
                    Image rodapeImage = Image.getInstance(rodapePath);
                    // AJUSTE: Tamanho aumentado para alta resolução
                    rodapeImage.scaleToFit(900, 300); // Aumentado de 300,100 para 900,300

                    float x = (PDF_WIDTH - rodapeImage.getScaledWidth()) / 2;
                    float y = 100f; // Posição do rodapé ajustada

                    rodapeImage.setAbsolutePosition(x, y);
                    document.add(rodapeImage);
                } else {
                    logger.warn("Arquivo de rodapé não encontrado: {}", rodapePath);
                }

            } catch (Exception e) {
                logger.warn("Falha ao carregar rodapé: {}", e.getMessage());
            }
        }
    }

    private String formatCurrency(BigDecimal value) {
        try {
            if (value == null) return "0,00";

            double valorDouble = value.doubleValue() / 100.0;

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