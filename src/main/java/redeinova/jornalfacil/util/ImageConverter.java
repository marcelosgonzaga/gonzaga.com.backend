package redeinova.jornalfacil.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

@Component
public class ImageConverter {
    private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class);

    public byte[] convertToPng(String imagePath) throws IOException {
        logger.debug("Convertendo imagem para PNG: {}", imagePath);

        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                throw new IOException("Arquivo não encontrado: " + imagePath);
            }

            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new IOException("Não foi possível ler a imagem: " + imagePath);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(image, "PNG", baos);

            if (!success) {
                throw new IOException("Falha ao converter imagem para PNG");
            }

            return baos.toByteArray();
        } catch (Exception e) {
            logger.error("Falha na conversão: {}", e.getMessage());
            throw new IOException("Falha ao processar imagem: " + e.getMessage(), e);
        }
    }

    public boolean isSupportedFormat(String imagePath) {
        if (imagePath == null) return false;
        String lowerPath = imagePath.toLowerCase();
        return lowerPath.endsWith(".png") || lowerPath.endsWith(".jpg") ||
                lowerPath.endsWith(".jpeg");
    }

    public boolean isWebpFormat(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }
        String lowerPath = imagePath.toLowerCase();
        return lowerPath.endsWith(".webp");
    }

    public boolean isImageAccessible(String imagePath) {
        try {
            File file = new File(imagePath);
            return file.exists() && file.canRead() && file.length() > 0;
        } catch (Exception e) {
            logger.warn("Erro ao verificar acessibilidade da imagem: {}", imagePath, e);
            return false;
        }
    }
}