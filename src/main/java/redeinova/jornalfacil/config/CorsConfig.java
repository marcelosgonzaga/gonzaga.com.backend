package redeinova.jornalfacil.config;

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

    //@Override
    //public void addCorsMappings(CorsRegistry registry) {
        // Configuração CORS para permitir acesso do frontend
       /* registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Type", "Content-Length", "Content-Disposition")
                .allowCredentials(true)
                .maxAge(3600);
    }*/
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

        // Imagens de temas
        String temasDir = "file:" + uploadDir + "/imagens/temas/";

        registry.addResourceHandler("/imagens/temas/**")
                .addResourceLocations(temasDir)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        return resource.exists() && resource.isReadable() ? resource : null;
                    }
                });

        // Imagens de produtos
        String produtosDir = "file:" + uploadDir + "/imagens/produtos/";
        registry.addResourceHandler("/imagens/produtos/**")
                .addResourceLocations(produtosDir)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        return resource.exists() && resource.isReadable() ? resource : null;
                    }
                });

        // Imagens de rodapés
        String rodapesDir = "file:" + uploadDir + "/imagens/rodapes/";
        registry.addResourceHandler("/imagens/rodapes/**")
                .addResourceLocations(rodapesDir)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource resource = location.createRelative(resourcePath);
                        return resource.exists() && resource.isReadable() ? resource : null;
                    }
                });
    }
}







//package redeinova.jornalfacil.config;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.Resource;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//import org.springframework.web.servlet.resource.PathResourceResolver;
//
//import java.io.IOException;
//
//@Configuration
//public class CorsConfig implements WebMvcConfigurer {
//
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("http://localhost:5173", "http://localhost:3000")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
//                .allowedHeaders("*")
//                .exposedHeaders("Content-Type", "Content-Length", "Content-Disposition")
//                .allowCredentials(true)
//                .maxAge(3600);
//    }
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        // Configuração para imagens de temas
//        String temasDir = "file:///E:/itensEncarteFacil/imagens/temas/";
//
//        registry.addResourceHandler("/imagens/temas/**")
//                .addResourceLocations(temasDir)
//                .setCachePeriod(3600)
//                .resourceChain(true)
//                .addResolver(new PathResourceResolver() {
//                    @Override
//                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
//                        Resource resource = location.createRelative(resourcePath);
//                        if (resource.exists() && resource.isReadable()) {
//                            return resource;
//                        }
//                        throw new IOException("Arquivo não encontrado ou não pode ser lido: " + resourcePath);
//                    }
//                });
//
//        // Configuração para imagens de produtos
//        String produtosDir = "file:///E:/itensEncarteFacil/imagens/produtos/";
//
//        registry.addResourceHandler("/imagens/produtos/**")
//                .addResourceLocations(produtosDir)
//                .setCachePeriod(3600)
//                .resourceChain(true)
//                .addResolver(new PathResourceResolver() {
//                    @Override
//                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
//                        Resource resource = location.createRelative(resourcePath);
//                        if (resource.exists() && resource.isReadable()) {
//                            return resource;
//                        }
//                        return null; // Retorna null para tentar outros ResourceHandlers
//                    }
//                });
//
//        // Configuração para imagens de rodapés
//        String rodapesDir = "file:///E:/itensEncarteFacil/imagens/rodapes/";
//
//        registry.addResourceHandler("/imagens/rodapes/**")
//                .addResourceLocations(rodapesDir)
//                .setCachePeriod(3600)
//                .resourceChain(true)
//                .addResolver(new PathResourceResolver() {
//                    @Override
//                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
//                        Resource resource = location.createRelative(resourcePath);
//                        if (resource.exists() && resource.isReadable()) {
//                            return resource;
//                        }
//                        throw new IOException("Arquivo não encontrado ou não pode ser lido: " + resourcePath);
//                    }
//                });
//    }
//}