package redeinova.jornalfacil.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import redeinova.jornalfacil.model.Projeto;
import redeinova.jornalfacil.model.Produto;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.math.BigDecimal;

@Service
public class PdfService {

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4, 20, 20, 30, 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Configurações de fonte
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontPeriodo = FontFactory.getFont(FontFactory.HELVETICA, 14);
            Font fontProdutoNome = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font fontPreco = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Título
            Paragraph titulo = new Paragraph("Encarte Promocional", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(10f);
            document.add(titulo);

            // Período de validade
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String periodo = "Válido de " + projeto.getDataInicio().format(formatter) +
                    " a " + projeto.getDataFim().format(formatter);
            Paragraph validade = new Paragraph(periodo, fontPeriodo);
            validade.setAlignment(Element.ALIGN_CENTER);
            validade.setSpacingAfter(20f);
            document.add(validade);

            // Tabela de produtos (4 colunas)
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // Formatação de moeda
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            for (Produto produto : projeto.getProdutos()) {
                PdfPCell cell = new PdfPCell();
                cell.setPadding(5);
                cell.setBorder(Rectangle.NO_BORDER);

                // Nome do produto
                Paragraph pNome = new Paragraph(produto.getDescricao(), fontProdutoNome);
                pNome.setAlignment(Element.ALIGN_CENTER);

                // Preços
                Paragraph pPreco = new Paragraph();
                if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
                    pPreco.add(new Chunk("De: ", fontProdutoNome));
                    pPreco.add(new Chunk(currencyFormat.format(produto.getPrecoDe()), fontProdutoNome));
                    pPreco.add(new Chunk("\n", fontProdutoNome));
                }
                pPreco.add(new Chunk("Por: ", fontProdutoNome));
                pPreco.add(new Chunk(currencyFormat.format(produto.getPrecoPor()), fontPreco));

                cell.addElement(pNome);
                cell.addElement(pPreco);
                table.addCell(cell);
            }

            document.add(table);
            return baos.toByteArray();

        } finally {
            if (document != null && document.isOpen()) {
                document.close();
            }
        }
    }
}