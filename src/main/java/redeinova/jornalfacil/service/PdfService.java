package redeinova.jornalfacil.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redeinova.jornalfacil.model.Projeto;
import redeinova.jornalfacil.model.Produto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;

@Service
public class PdfService {

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.upload-dir}/imagens/rodapes/")
    private String rodapesDir;

    @Value("${file.upload-dir}/placeholders/product-placeholder.jpg")
    private String placeholderPath;

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        // Configuração do documento PDF
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Adicionar tema como imagem de fundo
            adicionarTema(document, projeto);

            // Adicionar produtos em uma tabela 4x4
            adicionarProdutos(document, projeto);

            return baos.toByteArray();
        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }

    private void adicionarTema(Document document, Projeto projeto) throws DocumentException {
        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
            try {
                String temaPath = temasDir + projeto.getTema().getCaminhoImagem();
                Image temaImage = Image.getInstance(temaPath);
                temaImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
                temaImage.setAbsolutePosition(0, 0);
                document.add(temaImage);
            } catch (Exception e) {
                // Fallback para texto se a imagem não puder ser carregada
                Paragraph temaTexto = new Paragraph("Tema: " + projeto.getTema().getDescricao(),
                        FontFactory.getFont(FontFactory.HELVETICA, 24));
                temaTexto.setAlignment(Element.ALIGN_CENTER);
                document.add(temaTexto);
            }
        }
    }

    private void adicionarProdutos(Document document, Projeto projeto) throws DocumentException {
        PdfPTable table = new PdfPTable(4); // 4 colunas
        table.setWidthPercentage(100);
        table.setSpacingBefore(20f);

        for (Produto produto : projeto.getProdutos()) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(5);

            // Adicionar imagem do produto
            adicionarImagemProduto(cell, produto);

            // Adicionar preços
            adicionarPrecosProduto(cell, produto);

            table.addCell(cell);
        }

        document.add(table);
    }

    private void adicionarImagemProduto(PdfPCell cell, Produto produto) {
        if (produto.getCaminhoImagem() != null) {
            try {
                String imagePath = produtosDir + produto.getCaminhoImagem();
                Image productImage = Image.getInstance(imagePath);
                productImage.scaleToFit(100, 100);
                cell.addElement(new Chunk(productImage, 0, 0));
            } catch (Exception e) {
                // Fallback para placeholder
                try {
                    Image placeholder = Image.getInstance(placeholderPath);
                    placeholder.scaleToFit(100, 100);
                    cell.addElement(new Chunk(placeholder, 0, 0));
                } catch (Exception ex) {
                    cell.addElement(new Chunk("[Imagem indisponível]",
                            FontFactory.getFont(FontFactory.HELVETICA, 10)));
                }
            }
        }
    }

    private void adicionarPrecosProduto(PdfPCell cell, Produto produto) {
        Paragraph precoParagraph = new Paragraph();
        if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
            precoParagraph.add(new Chunk("De: " + formatCurrency(produto.getPrecoDe()) + "\n",
                    FontFactory.getFont(FontFactory.HELVETICA, 10)));
        }
        precoParagraph.add(new Chunk("Por: " + formatCurrency(produto.getPrecoPor()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        cell.addElement(precoParagraph);
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }
}



//package redeinova.jornalfacil.service;
//
//import com.lowagie.text.*;
//import com.lowagie.text.pdf.PdfPCell;
//import com.lowagie.text.pdf.PdfPTable;
//import com.lowagie.text.pdf.PdfWriter;
//import org.springframework.beans.factory.annotation.Value;
//import redeinova.jornalfacil.model.Projeto;
//import redeinova.jornalfacil.model.Produto;
//import org.springframework.stereotype.Service;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.text.NumberFormat;
//import java.util.Locale;
//import java.math.BigDecimal;
//
//@Service
//public class PdfService {
//    @Value("${file.upload-dir}")
//    private String uploadDir;
//
//    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
//        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//
//        try {
//            PdfWriter.getInstance(document, baos);
//            document.open();
//
//            // Adicionar tema como imagem de fundo
//            if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
//                try {
//                    String temaPath = uploadDir + "/imagens/temas/" + projeto.getTema().getCaminhoImagem();
//                    Image temaImage = Image.getInstance(temaPath);
//                    temaImage.scaleToFit(document.getPageSize().getWidth(), document.getPageSize().getHeight());
//                    temaImage.setAbsolutePosition(0, 0);
//                    document.add(temaImage);
//                } catch (Exception e) {
//                    // Adicionar texto alternativo se o tema não puder ser carregado
//                    Paragraph temaTexto = new Paragraph("Tema: " + projeto.getTema().getDescricao(),
//                            FontFactory.getFont(FontFactory.HELVETICA, 24));
//                    temaTexto.setAlignment(Element.ALIGN_CENTER);
//                    document.add(temaTexto);
//                }
//            }
//
//            // Adicionar produtos
//            float margin = 20;
//            float width = document.getPageSize().getWidth() - 2 * margin;
//            float height = document.getPageSize().getHeight() - 2 * margin;
//
//            PdfPTable table = new PdfPTable(4);
//            table.setWidthPercentage(100);
//            table.setSpacingBefore(20f);
//
//            for (Produto produto : projeto.getProdutos()) {
//                PdfPCell cell = new PdfPCell();
//                cell.setBorder(Rectangle.NO_BORDER);
//                cell.setPadding(5);
//
//                // Adicionar imagem do produto
//                if (produto.getCaminhoImagem() != null) {
//                    String imagePath = uploadDir + "/imagens/produtos/" + produto.getCaminhoImagem();
//                    try {
//                        Image productImage = Image.getInstance(imagePath);
//                        productImage.scaleToFit(100, 100);
//                        cell.addElement(new Chunk(productImage, 0, 0));
//                    } catch (IOException e) {
//                        // Usar placeholder se a imagem não existir
//                        try {
//                            String placeholderPath = uploadDir + "/placeholders/product-placeholder.jpg";
//                            Image placeholder = Image.getInstance(placeholderPath);
//                            placeholder.scaleToFit(100, 100);
//                            cell.addElement(new Chunk(placeholder, 0, 0));
//                        } catch (Exception ex) {
//                            // Se o placeholder também falhar, adicionar texto alternativo
//                            cell.addElement(new Chunk("[Imagem não disponível]",
//                                    FontFactory.getFont(FontFactory.HELVETICA, 10)));
//                        }
//                    }
//                }
//
//                // Adicionar preços
//                Paragraph precoParagraph = new Paragraph();
//                if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
//                    precoParagraph.add(new Chunk("De: " + formatCurrency(produto.getPrecoDe()) + "\n",
//                            FontFactory.getFont(FontFactory.HELVETICA, 10)));
//                }
//                precoParagraph.add(new Chunk("Por: " + formatCurrency(produto.getPrecoPor()),
//                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
//
//                cell.addElement(precoParagraph);
//                table.addCell(cell);
//            }
//
//            document.add(table);
//            return baos.toByteArray();
//        } finally {
//            if (document != null && document.isOpen()) {
//                document.close();
//            }
//        }
//    }
//
//    private String formatCurrency(BigDecimal value) {
//        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
//    }
//}