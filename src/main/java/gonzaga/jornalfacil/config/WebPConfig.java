package gonzaga.jornalfacil.config;

import org.springframework.context.annotation.Configuration;
import com.twelvemonkeys.imageio.plugins.webp.WebPImageReaderSpi;

import javax.imageio.spi.IIORegistry;
import jakarta.annotation.PostConstruct;

@Configuration
public class WebPConfig {

    @PostConstruct
    public void registerWebPPlugins() {
        try {
            IIORegistry registry = IIORegistry.getDefaultInstance();
            registry.registerServiceProvider(new WebPImageReaderSpi());
            System.out.println("WebP ImageReader registrado com sucesso");
        } catch (Exception e) {
            System.err.println("Falha ao registrar plugin WebP: " + e.getMessage());
            e.printStackTrace();
        }
    }
}