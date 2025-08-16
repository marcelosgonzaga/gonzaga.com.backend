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

@Service
public class ImageService {

    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.upload-dir}/imagens/rodapes/")
    private String rodapesDir;

    public byte[] gerarEncarteImagem(Projeto projeto) throws IOException {
        // Configuração da imagem JPG
        BufferedImage bufferedImage = new BufferedImage(2480, 3508, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        try {
            // Fundo branco
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            // Carregar e desenhar tema de fundo
            desenharTemaFundo(g2d, bufferedImage, projeto);

            // Desenhar produtos em grid 4x4
            desenharProdutos(g2d, bufferedImage, projeto);

            // Converter para byte array
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
                String temaPath = temasDir + projeto.getTema().getCaminhoImagem();
                BufferedImage temaImage = ImageIO.read(new File(temaPath));
                g2d.drawImage(temaImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
            } catch (IOException e) {
                // Fallback para fundo padrão
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
                g2d.setColor(Color.BLUE);
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                g2d.drawString("Tema não encontrado", 100, 100);
            }
        }
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
                String imagePath = produtosDir + produto.getCaminhoImagem();
                BufferedImage productImage = ImageIO.read(new File(imagePath));
                g2d.drawImage(productImage, x + 10, y + 10, cellWidth - 20, cellHeight - 40, null);
            } catch (IOException e) {
                // Fallback para placeholder
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
}





//package redeinova.jornalfacil.service;
//
//import java.awt.Font;
//import org.springframework.beans.factory.annotation.Value;
//import redeinova.jornalfacil.model.Projeto;
//import redeinova.jornalfacil.model.Produto;
//import org.springframework.stereotype.Service;
//
//import javax.imageio.ImageIO;
//import java.awt.*;
//import java.awt.image.BufferedImage;
//import java.io.ByteArrayOutputStream;
//import java.io.File;
//import java.io.IOException;
//import java.text.NumberFormat;
//import java.util.Locale;
//import java.math.BigDecimal;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//@Service
//public class ImageService {
//    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
//
//    @Value("${file.upload-dir}")
//    private String uploadDir;
//
//    public byte[] gerarEncarteImagem(Projeto projeto) throws IOException {
//        logger.debug("Iniciando geração de imagem para projeto ID: {}", projeto.getId());
//
//        BufferedImage bufferedImage = new BufferedImage(2480, 3508, BufferedImage.TYPE_INT_RGB);
//        Graphics2D g2d = bufferedImage.createGraphics();
//
//        // Fundo branco
//        g2d.setColor(Color.WHITE);
//        g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
//
//        // Carregar e desenhar tema de fundo
//        if (projeto.getTema() != null && projeto.getTema().getCaminhoImagem() != null) {
//            String temaPath = uploadDir + "/imagens/temas/" + projeto.getTema().getCaminhoImagem();
//            logger.debug("Tentando carregar imagem do tema: {}", temaPath);
//            try {
//                BufferedImage temaImage = ImageIO.read(new File(temaPath));
//                g2d.drawImage(temaImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
//            } catch (IOException e) {
//                logger.warn("Falha ao carregar imagem do tema, usando placeholder");
//                // Desenhar um fundo padrão
//                g2d.setColor(Color.WHITE);
//                g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());
//                g2d.setColor(Color.BLUE);
//                g2d.setFont(new Font("Arial", Font.BOLD, 48));
//                g2d.drawString("Tema não encontrado", 100, 100);
//            }
//        }
//
//        // Desenhar produtos (4x4 grid)
//        int cellWidth = bufferedImage.getWidth() / 4;
//        int cellHeight = (int)(bufferedImage.getHeight() * 0.6) / 4;
//        int startY = (int)(bufferedImage.getHeight() * 0.3);
//
//        for (int i = 0; i < projeto.getProdutos().size() && i < 16; i++) {
//            Produto produto = projeto.getProdutos().get(i);
//            int x = (i % 4) * cellWidth;
//            int y = startY + (i / 4) * cellHeight;
//
//            // Desenhar imagem do produto
//            if (produto.getCaminhoImagem() != null) {
//                String imagePath = uploadDir + "/imagens/produtos/" + produto.getCaminhoImagem();
//                logger.debug("Tentando carregar imagem do produto: {}", imagePath);
//                try {
//                    BufferedImage productImage = ImageIO.read(new File(imagePath));
//                    g2d.drawImage(productImage, x + 10, y + 10, cellWidth - 20, cellHeight - 40, null);
//                } catch (IOException e) {
//                    logger.warn("Falha ao carregar imagem do produto, usando placeholder");
//                    // Desenhar um retângulo com texto
//                    g2d.setColor(Color.LIGHT_GRAY);
//                    g2d.fillRect(x + 10, y + 10, cellWidth - 20, cellHeight - 40);
//                    g2d.setColor(Color.BLACK);
//                    g2d.setFont(new Font("Arial", Font.PLAIN, 24));
//                    g2d.drawString("Produto", x + 20, y + 30);
//                }
//            }
//
//            // Desenhar preços
//            g2d.setColor(Color.RED);
//            g2d.setFont(new Font("Arial", Font.BOLD, 24));
//
//            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
//                g2d.drawString("De: " + formatCurrency(produto.getPrecoDe()), x + 10, y + cellHeight - 30);
//            }
//            g2d.drawString("Por: " + formatCurrency(produto.getPrecoPor()), x + 10, y + cellHeight - 10);
//        }
//
//        g2d.dispose();
//
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(bufferedImage, "jpg", baos);
//        return baos.toByteArray();
//    }
//
//    private String formatCurrency(BigDecimal value) {
//        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
//    }
//}