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
                    int maxSize = 415;
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
        g2d.setStroke(new BasicStroke(4f)); // Borda mais espessa
        g2d.drawRoundRect(boxX + 4, boxY + 4, boxWidth - 8, boxHeight - 8, 18, 18);

        // Textos dos preços
        g2d.setColor(Color.WHITE);

        // VERIFICAR SE A FONTE ROBOTO CONDENSED ESTÁ DISPONÍVEL
        String[] fontesPreferidas = {"Impact", "Arial Black", "Segoe UI Black", "SansSerif"};
        Font fonteFallback = new Font("Segoe UI Black", Font.BOLD, 20);

        // Preço De (se existir)
        if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
            Font fontePrecoDe = encontrarFonteDisponivel(g2d, new String[]{"Trebuchet MS", "Arial", "SansSerif"}, Font.BOLD, 15);
            //Font fontePrecoDe = encontrarFonteDisponivel(g2d, fontesPreferidas, Font.BOLD, 13);
            g2d.setFont(fontePrecoDe);
            String precoDeText = "De R$ " + formatCurrency(produto.getPrecoDe()) + " por apenas: ";
            g2d.drawString(precoDeText, boxX + 10, boxY + 20);
        }

        // Preço Por (valor principal)
        String precoPorText = formatCurrency(produto.getPrecoPor());
        String valorFormatado = precoPorText.replace("R$", "").trim();
        String[] partes = valorFormatado.split(",");
        String parteInteira = partes[0];
        String parteDecimal = partes.length > 1 ? "," + partes[1] : ",00";

        // CONFIGURAÇÕES DE FONTE COM FALLBACK
        Font fonteR$ = encontrarFonteDisponivel(g2d, fontesPreferidas, Font.BOLD, 36);
        Font fontePrincipal = encontrarFonteDisponivel(g2d, fontesPreferidas, Font.BOLD, 72);
        Font fonteCentavos = encontrarFonteDisponivel(g2d, fontesPreferidas, Font.BOLD, 36);

        // CALCULAR LARGURAS TOTAIS PARA CENTRALIZAÇÃO
        FontMetrics fmR$ = g2d.getFontMetrics(fonteR$);
        FontMetrics fmPrincipal = g2d.getFontMetrics(fontePrincipal);
        FontMetrics fmCentavos = g2d.getFontMetrics(fonteCentavos);

        int larguraR$ = fmR$.stringWidth("R$");
        int larguraPrincipal = fmPrincipal.stringWidth(parteInteira);
        int larguraCentavos = fmCentavos.stringWidth(parteDecimal);
        int espacamento = 5;

        int larguraTotal = larguraR$ + espacamento + larguraPrincipal + espacamento + larguraCentavos;

        // CALCULAR POSIÇÃO X INICIAL PARA CENTRALIZAR TODO O PREÇO
        int startX = boxX + (boxWidth - larguraTotal) / 2;
        int baseY = boxY + 90;

        // DESENHAR R$
        g2d.setFont(fonteR$);
        int r$Y = baseY - (fontePrincipal.getSize() - fonteR$.getSize()) / 3;
        g2d.drawString("R$", startX, r$Y);

        // DESENHAR VALOR PRINCIPAL
        g2d.setFont(fontePrincipal);
        int principalX = startX + larguraR$ + espacamento;
        g2d.drawString(parteInteira, principalX, baseY);

        // DESENHAR CENTAVOS
        g2d.setFont(fonteCentavos);
        int centavosX = principalX + larguraPrincipal + espacamento;
        int centavosY = baseY - (fontePrincipal.getSize() - fonteCentavos.getSize()) / 2;
        g2d.drawString(parteDecimal, centavosX, centavosY);

        // Texto "a unid."
        Font fonteUnid = encontrarFonteDisponivel(g2d, new String[]{"Trebuchet MS", "Arial", "SansSerif"}, Font.PLAIN, 18);
        g2d.setFont(fonteUnid);
        String unidText = "a unid.";
        FontMetrics fmUnid = g2d.getFontMetrics();
        int larguraUnid = fmUnid.stringWidth(unidText);
        int unidX = boxX + 220;
        int unidY = boxY + boxHeight - 10;
        g2d.drawString(unidText, unidX, unidY);
    }

    // MÉTODO AUXILIAR PARA ENCONTRAR FONTE DISPONÍVEL
    private Font encontrarFonteDisponivel(Graphics2D g2d, String[] fontes, int estilo, int tamanho) {
        for (String nomeFonte : fontes) {
            Font fonte = new Font(nomeFonte, estilo, tamanho);
            if (fonte.getFamily().equalsIgnoreCase(nomeFonte)) {
                return fonte;
            }
        }
        // Fallback para fonte padrão do sistema
        return new Font("Arial", estilo, tamanho);
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

            // Converter para double e dividir por 100 (assume que o valor está em centavos)
            double valorDouble = value.doubleValue() / 100.0;

            NumberFormat formatter = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
            formatter.setMinimumFractionDigits(2);
            formatter.setMaximumFractionDigits(2);

            return formatter.format(valorDouble);
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


