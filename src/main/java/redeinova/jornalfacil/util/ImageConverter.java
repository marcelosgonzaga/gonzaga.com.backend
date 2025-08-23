package redeinova.jornalfacil.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class ImageConverter {

    private static final Logger logger = LoggerFactory.getLogger(ImageConverter.class);

    /**
     * Converte imagem WebP para PNG em bytes com fallback robusto
     */
    public byte[] convertWebpToPng(String imagePath) throws IOException {
        logger.debug("Convertendo WebP para PNG: {}", imagePath);

        try {
            // Primeiro verifica se o arquivo existe
            File file = new File(imagePath);
            if (!file.exists() || !file.canRead()) {
                throw new IOException("Arquivo não encontrado ou sem permissão de leitura: " + imagePath);
            }

            // Tenta carregar como WebP
            BufferedImage image = ImageIO.read(file);

            if (image == null) {
                throw new IOException("Não foi possível carregar a imagem: " + imagePath);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean success = ImageIO.write(image, "PNG", baos);

            if (!success) {
                throw new IOException("Falha ao converter imagem para PNG");
            }

            logger.debug("Conversão WebP para PNG concluída com sucesso");
            return baos.toByteArray();

        } catch (Exception e) {
            logger.warn("Falha na conversão WebP, tentando fallback: {}", e.getMessage());

            // Fallback: tenta carregar como imagem regular
            try {
                return Files.readAllBytes(Paths.get(imagePath));
            } catch (Exception ex) {
                logger.error("Fallback também falhou: {}", ex.getMessage());
                throw new IOException("Erro ao processar imagem: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Verifica se o caminho da imagem é formato WebP
     */
    public boolean isWebpFormat(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return false;
        }

        String lowerPath = imagePath.toLowerCase();
        boolean isWebp = lowerPath.endsWith(".webp");
        logger.debug("Verificando formato WebP: {} -> {}", imagePath, isWebp);

        return isWebp;
    }

    /**
     * Método seguro para carregar qualquer formato de imagem
     */
    private BufferedImage loadImage(String imagePath) throws IOException {
        try {
            BufferedImage image;

            if (imagePath.startsWith("http") || imagePath.startsWith("file:")) {
                logger.debug("Carregando imagem de URL: {}", imagePath);
                image = ImageIO.read(new URL(imagePath));
            } else {
                logger.debug("Carregando imagem de arquivo local: {}", imagePath);
                File file = new File(imagePath);

                if (!file.exists()) {
                    throw new IOException("Arquivo não encontrado: " + imagePath);
                }

                if (!file.canRead()) {
                    throw new IOException("Sem permissão para ler arquivo: " + imagePath);
                }

                image = ImageIO.read(file);
            }

            return image;

        } catch (Exception e) {
            logger.error("Falha ao carregar imagem: {}", imagePath, e);
            throw new IOException("Falha ao carregar imagem: " + e.getMessage(), e);
        }
    }

    /**
     * Método alternativo para verificar se arquivo existe e é legível
     */
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