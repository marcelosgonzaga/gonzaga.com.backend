package gonzaga.jornalfacil.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import gonzaga.jornalfacil.model.Projeto;
import gonzaga.jornalfacil.model.Produto;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

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
        logger.info("Iniciando geração de imagem JPG para projeto ID: {}", projeto.getId());

        // Criar imagem com alta resolução (ideal para impressão)
        BufferedImage bufferedImage = new BufferedImage(2480, 3508, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = bufferedImage.createGraphics();

        try {
            // Configurar máxima qualidade de renderização
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

            // Fundo branco
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight());

            desenharTemaFundo(g2d, bufferedImage, projeto);
            desenharProdutos(g2d, bufferedImage, projeto);
            desenharDatasValidade(g2d, bufferedImage, projeto);
            desenharRodape(g2d, bufferedImage, projeto);

            // Salvar com alta qualidade
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            // Configurar parâmetros de qualidade para JPEG
            javax.imageio.ImageWriteParam writeParam = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next().getDefaultWriteParam();
            writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
            writeParam.setCompressionQuality(0.95f); // 95% de qualidade

            javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
            writer.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
            writer.write(null, new javax.imageio.IIOImage(bufferedImage, null, null), writeParam);
            writer.dispose();

            logger.info("Imagem JPG gerada com sucesso para projeto ID: {}", projeto.getId());
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

                BufferedImage temaImage = ImageIO.read(new File(temaPath));
                if (temaImage != null) {
                    // Manter qualidade na renderização do tema
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

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
    }

    private void desenharProdutos(Graphics2D g2d, BufferedImage bufferedImage, Projeto projeto) throws IOException {
        // Configurar qualidade geral
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Layout de 4x4 produtos
        int cellWidth = bufferedImage.getWidth() / 4;
        int cellHeight = bufferedImage.getHeight() / 4;
        int startY = (int)(bufferedImage.getHeight() * 0.35);

        // VARIÁVEL PARA CONTROLAR O ESPAÇAMENTO VERTICAL ENTRE LINHAS
        int espacamentoVertical = -400; // Ajuste este valor conforme necessário

        for (int i = 0; i < projeto.getProdutos().size() && i < 16; i++) {
            Produto produto = projeto.getProdutos().get(i);
            int x = (i % 4) * cellWidth;
            int y = startY + (i / 4) * (cellHeight + espacamentoVertical);

            // Imagem do produto (maior e com melhor qualidade)
            desenharImagemProduto(g2d, produto, x, y, cellWidth, cellHeight);

            // Box de preços (posicionada abaixo da imagem)
            desenharBoxPrecos(g2d, produto, x, y, cellWidth, cellHeight);
        }
    }

    private void desenharImagemProduto(Graphics2D g2d, Produto produto, int x, int y, int cellWidth, int cellHeight) {
        if (produto.getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(produto.getCaminhoImagem());
                String imagePath = produtosDir + fileName;

                // Carregar imagem com alta qualidade
                BufferedImage originalImage = ImageIO.read(new File(imagePath));
                if (originalImage != null) {
                    // Manter proporções originais
                    int originalWidth = originalImage.getWidth();
                    int originalHeight = originalImage.getHeight();

                    // Definir tamanho máximo desejado mantendo proporção
                    int maxSize = 400;
                    int newWidth, newHeight;

                    if (originalWidth > originalHeight) {
                        newWidth = maxSize;
                        newHeight = (int) ((double) originalHeight / originalWidth * maxSize);
                    } else {
                        newHeight = maxSize;
                        newWidth = (int) ((double) originalWidth / originalHeight * maxSize);
                    }

                    // Criar imagem redimensionada com alta qualidade
                    BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2dResized = resizedImage.createGraphics();

                    // Configurar qualidade de renderização
                    g2dResized.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2dResized.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);
                    g2dResized.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);

                    g2dResized.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
                    g2dResized.dispose();

                    // Centralizar a imagem
                    int imageX = x + (cellWidth - newWidth) / 2;
                    int imageY = y + 20; // Posição vertical

                    // Desenhar a imagem com alta qualidade
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                            RenderingHints.VALUE_RENDER_QUALITY);

                    g2d.drawImage(resizedImage, imageX, imageY, null);
                    return;
                }
            } catch (IOException e) {
                logger.warn("Falha ao carregar imagem do produto {}: {}", produto.getId(), e.getMessage());
            }
        }

        // Fallback - caixa cinza
        g2d.setColor(Color.LIGHT_GRAY);
        int imageSize = 200;
        int imageX = x + (cellWidth - imageSize) / 2;
        int imageY = y + 20;
        g2d.fillRect(imageX, imageY, imageSize, imageSize);
    }

    private void desenharBoxPrecos(Graphics2D g2d, Produto produto, int x, int y, int cellWidth, int cellHeight) {
        // CONTROLE INDEPENDENTE DE LARGURA E ALTURA
        int boxWidth = 300;  // Largura fixa em pixels
        int boxHeight = 110;  // Altura fixa em pixels

        // Centralizar horizontalmente
        int boxX = x + (cellWidth - boxWidth) / 2;

        // VARIÁVEL PARA CONTROLAR A DISTÂNCIA ENTRE A IMAGEM E A BOX DE PREÇO
        int distanciaImagemBox = 30; // Ajuste este valor conforme necessário

        // Posicionar abaixo da imagem do produto
        int boxY = y + 350 + distanciaImagemBox;  // Ajuste vertical

        // Gradiente vermelho
        GradientPaint gradient = new GradientPaint(
                boxX, boxY, new Color(220, 38, 38),
                boxX, boxY + boxHeight, new Color(185, 28, 28)
        );

        g2d.setPaint(gradient);
        g2d.fillRoundRect(boxX, boxY, boxWidth, boxHeight, 15, 15); // Bordas mais arredondadas

        // Borda interna branca sutil
        g2d.setColor(new Color(255, 255, 255, 50));
        g2d.setStroke(new BasicStroke(2f)); // Borda mais espessa
        g2d.drawRoundRect(boxX + 2, boxY + 2, boxWidth - 4, boxHeight - 4, 12, 12);

        // Textos dos preços
        g2d.setColor(Color.WHITE);

        // Preço De (se existir)
        if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
            g2d.setFont(new Font("Arial", Font.BOLD, 16));
            String precoDeText = "De: " + formatCurrency(produto.getPrecoDe());
            g2d.drawString(precoDeText, boxX + 10, boxY + 20);
        }

        // Preço Por (valor principal)
        g2d.setFont(new Font("Arial Rounded MT Bold", Font.BOLD, 32));
        String precoPorText = "R$ " + formatCurrencySimple(produto.getPrecoPor());
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(precoPorText);
        g2d.drawString(precoPorText, boxX + (boxWidth - textWidth) / 2, boxY + 55);

        // Texto "a unid."
        g2d.setFont(new Font("Trebuchet MS", Font.PLAIN, 14));
        g2d.drawString("a unid.", boxX + boxWidth - 45, boxY + boxHeight - 10);
    }

    private void desenharDatasValidade(Graphics2D g2d, BufferedImage bufferedImage, Projeto projeto) {
        if (projeto.getDataInicio() != null && projeto.getDataFim() != null) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                String textoDatas = String.format("Ofertas válidas de %s até %s ou enquanto durarem os estoques",
                        projeto.getDataInicio().format(formatter),
                        projeto.getDataFim().format(formatter));

                // AUMENTAR TAMANHO DA FONTE
                g2d.setFont(new Font("Arial", Font.BOLD, 43)); // Aumentado de 14 para 18

                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(textoDatas);

                // AJUSTAR POSIÇÃO VERTICAL (Y) - mover mais para baixo
                int x = (bufferedImage.getWidth() - textWidth) / 2;
                int y = (int)(bufferedImage.getHeight() * 0.33); // Aumentado de 0.28 para 0.32

                // Sombra do texto para melhor legibilidade
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.drawString(textoDatas, x + 1, y + 1);

                g2d.setColor(Color.WHITE);
                g2d.drawString(textoDatas, x, y);

            } catch (Exception e) {
                logger.warn("Erro ao desenhar datas de validade: {}", e.getMessage());
            }
        }
    }

    private void desenharRodape(Graphics2D g2d, BufferedImage bufferedImage, Projeto projeto) {
        if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
            try {
                String fileName = extractFileName(projeto.getRodape().getCaminhoImagem());
                String rodapePath = rodapesDir + fileName;

                BufferedImage rodapeImage = ImageIO.read(new File(rodapePath));
                if (rodapeImage != null) {
                    int rodapeWidth = bufferedImage.getWidth() / 3;
                    int rodapeHeight = (int)(rodapeImage.getHeight() * ((double)rodapeWidth / rodapeImage.getWidth()));

                    // AJUSTAR POSIÇÃO HORIZONTAL (X) E VERTICAL (Y)
                    int x = (bufferedImage.getWidth() - rodapeWidth) / 2;
                    int y = bufferedImage.getHeight() - rodapeHeight - 55; // Aumentado de 20 para 50

                    g2d.drawImage(rodapeImage, x, y, rodapeWidth, rodapeHeight, null);
                }
            } catch (IOException e) {
                logger.warn("Falha ao carregar rodapé: {}", e.getMessage());
            }
        }
    }

    private String formatCurrency(BigDecimal value) {
        try {
            if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
                return "0,00";
            }

            // Formatar corretamente para o padrão brasileiro
            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);

            return formatter.format(value);
        } catch (Exception e) {
            System.out.println("Erro ao formatar valor: " + value + " - " + e.getMessage());
            return "0,00";
        }
    }

    private String formatCurrencySimple(BigDecimal value) {
        try {
            if (value == null || value.compareTo(BigDecimal.ZERO) == 0) {
                return "0,00";
            }

            return formatCurrency(value);
        } catch (Exception e) {
            System.out.println("Erro ao formatar valor simples: " + value + " - " + e.getMessage());
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


