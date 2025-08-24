package redeinova.jornalfacil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import redeinova.jornalfacil.model.Projeto;
import redeinova.jornalfacil.model.Produto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.rodapes-dir}")
    private String rodapesDir;

    public byte[] gerarEncarteImagem(Projeto projeto) throws IOException {
        BufferedImage bufferedImage = new BufferedImage(2480, 3508, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            desenharTemaFundo(g2d, bufferedImage, projeto);
            desenharProdutos(g2d, bufferedImage, projeto);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "jpg", baos);
            return baos.toByteArray();
        } finally {
            g2d.dispose();
        }
    }

    private void desenharTemaFundo(Graphics2D g2d, BufferedImage bufferedImage, Projeto projeto) throws IOException {
        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getTema().getCaminhoImagem());
                String temaPath = temasDir + fileName;

                // Se for WebP, tentar usar PNG equivalente
                if (fileName.toLowerCase().endsWith(".webp")) {
                    String pngPath = temaPath.replace(".webp", ".png");
                    if (new File(pngPath).exists()) {
                        temaPath = pngPath;
                    }
                }

                BufferedImage temaImage = ImageIO.read(new File(temaPath));
                if (temaImage != null) {
                    g2d.drawImage(temaImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
                    return;
                }
            } catch (IOException e) {
                logger.warn("Falha ao carregar tema: {}", e.getMessage());
            }
        }

        // Fallback
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
        g2d.setColor(Color.BLUE);
        g2d.setFont(new Font("Arial", Font.BOLD, 48));
        g2d.drawString("Tema: " + (projeto.getTema() != null ? projeto.getTema().getDescricao() : "Não disponível"), 100, 100);
    }

    private void desenharProdutos(Graphics2D g2d, BufferedImage bufferedImage, Projeto projeto) throws IOException {
        int cellWidth = bufferedImage.getWidth() / 4;
        int cellHeight = (int)(bufferedImage.getHeight() * 0.6) / 4;
        int startY = (int)(bufferedImage.getHeight() * 0.3);

        for (int i = 0; i < projeto.getProdutos().size() && i < 16; i++) {
            Produto produto = projeto.getProdutos().get(i);
            int x = (i % 4) * cellWidth;
            int y = startY + (i / 4) * cellHeight;

            desenharImagemProduto(g2d, produto, x, y, cellWidth, cellHeight);
            desenharPrecosProduto(g2d, produto, x, y, cellHeight);
        }
    }

    private void desenharImagemProduto(Graphics2D g2d, Produto produto, int x, int y, int cellWidth, int cellHeight) {
        if (produto.getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(produto.getCaminhoImagem());
                String imagePath = produtosDir + fileName;

                // Se for WebP, tentar usar PNG equivalente
                if (fileName.toLowerCase().endsWith(".webp")) {
                    String pngPath = imagePath.replace(".webp", ".png");
                    if (new File(pngPath).exists()) {
                        imagePath = pngPath;
                    }
                }

                BufferedImage productImage = ImageIO.read(new File(imagePath));
                g2d.drawImage(productImage, x + 10, y + 10, cellWidth - 20, cellHeight - 40, null);
            } catch (IOException e) {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.fillRect(x + 10, y + 10, cellWidth - 20, cellHeight - 40);
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.PLAIN, 24));
                g2d.drawString("Produto", x + 20, y + 30);
            }
        }
    }

    private void desenharPrecosProduto(Graphics2D g2d, Produto produto, int x, int y, int cellHeight) {
        g2d.setColor(Color.RED);
        g2d.setFont(new Font("Arial", Font.BOLD, 24));

        if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
            g2d.drawString("De: " + formatCurrency(produto.getPrecoDe()), x + 10, y + cellHeight - 30);
        }
        g2d.drawString("Por: " + formatCurrency(produto.getPrecoPor()), x + 10, y + cellHeight - 10);
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        if (filePath.contains("/")) return filePath.substring(filePath.lastIndexOf("/") + 1);
        if (filePath.contains("\\")) return filePath.substring(filePath.lastIndexOf("\\") + 1);
        return filePath;
    }
}