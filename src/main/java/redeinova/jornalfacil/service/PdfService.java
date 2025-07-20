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

@Service
public class PdfService {

    public byte[] gerarEncarte(Projeto projeto) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30); // Margens de 30px
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Configuração de fontes
            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font fontCabecalho = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font fontProduto = FontFactory.getFont(FontFactory.HELVETICA, 10);

            // Cabeçalho
            Paragraph titulo = new Paragraph("Encarte Promocional", fontTitulo);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);

            // Período de validade
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            String periodo = "Válido de " + projeto.getDataInicio().format(formatter) +
                    " a " + projeto.getDataFim().format(formatter);
            Paragraph validade = new Paragraph(periodo, fontCabecalho);
            validade.setAlignment(Element.ALIGN_CENTER);
            document.add(validade);

            // Adicionar espaço
            document.add(new Paragraph(" "));

            // Lista de produtos (4 colunas)
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // Formatação de moeda
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

            for (Produto produto : projeto.getProdutos()) {
                PdfPCell cell = new PdfPCell();
                Paragraph p = new Paragraph();

                p.add(new Chunk(produto.getDescricao() + "\n", fontProduto));

                if (produto.getPrecoDe() != null) {
                    p.add(new Chunk("De: " + currencyFormat.format(produto.getPrecoDe()) + "   ", fontProduto));
                }

                p.add(new Chunk("Por: " + currencyFormat.format(produto.getPrecoPor()), fontProduto));
                cell.addElement(p);
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