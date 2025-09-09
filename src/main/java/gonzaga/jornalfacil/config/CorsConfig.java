package gonzaga.jornalfacil.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Content-Length", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configuração para servir arquivos estáticos (imagens)

        // Imagens de temas -
        String temasPath = "file:" + uploadDir + "/imagens/temas/";
        System.out.println("Configurando temasPath: " + temasPath);

        registry.addResourceHandler("/imagens/temas/**")
                .addResourceLocations(temasPath)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            System.out.println("Arquivo de tema encontrado: " + resourcePath);
                            return resource;
                        }
                        System.out.println("Arquivo de tema NÃO encontrado: " + resourcePath);
                        return null;
                    }
                });

        // Imagens de produtos -
        String produtosPath = "file:" + uploadDir + "/imagens/produtos/";
        System.out.println("Configurando produtosPath: " + produtosPath);

        registry.addResourceHandler("/imagens/produtos/**")
                .addResourceLocations(produtosPath)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            System.out.println("Arquivo de produto encontrado: " + resourcePath);
                            return resource;
                        }
                        System.out.println("Arquivo de produto NÃO encontrado: " + resourcePath);
                        return null;
                    }
                });

        // Imagens de rodapés -
        String rodapesPath = "file:" + uploadDir + "/imagens/rodapes/";
        System.out.println("Configurando rodapesPath: " + rodapesPath);

        registry.addResourceHandler("/imagens/rodapes/**")
                .addResourceLocations(rodapesPath)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        if (resource.exists() && resource.isReadable()) {
                            System.out.println("Arquivo de rodapé encontrado: " + resourcePath);
                            return resource;
                        }
                        System.out.println("Arquivo de rodapé NÃO encontrado: " + resourcePath);
                        return null;
                    }
                });

        // Placeholders -
        String placeholdersPath = "file:" + uploadDir + "/placeholders/";
        System.out.println("Configurando placeholdersPath: " + placeholdersPath);

        registry.addResourceHandler("/placeholders/**")
                .addResourceLocations(placeholdersPath)
                .setCachePeriod(3600);
    }
}