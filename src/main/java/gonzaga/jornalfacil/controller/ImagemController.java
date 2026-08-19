package gonzaga.jornalfacil.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("/api/imagens")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ImagemController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    @GetMapping("/{tipo}/{nomeArquivo:.+}")
    public ResponseEntity<Resource> getImagem(
            @PathVariable String tipo,
            @PathVariable String nomeArquivo) {

        log.debug("📸 Requisição de imagem - Tipo: {}, Arquivo: {}", tipo, nomeArquivo);

        // Validar tipo permitido
        if (!tipo.matches("temas|produtos|rodapes|placeholders")) {
            log.warn("Tipo de imagem inválido: {}", tipo);
            return ResponseEntity.badRequest().build();
        }

        // Sanitizar nome do arquivo
        String nomeSeguro = nomeArquivo.replaceAll("[^a-zA-Z0-9._-]", "");

        // Construir caminho completo
        Path caminho = Paths.get(uploadDir, "imagens", tipo, nomeSeguro);
        File arquivo = caminho.toFile();

        log.info("🔍 Procurando imagem em: {}", caminho.toAbsolutePath());

        if (!arquivo.exists()) {
            log.error("❌ Imagem NÃO encontrada: {}", caminho.toAbsolutePath());

            // Listar arquivos no diretório para debug
            File dir = caminho.getParent().toFile();
            if (dir.exists() && dir.isDirectory()) {
                log.info("📂 Arquivos disponíveis em {}:", dir.getPath());
                for (File f : dir.listFiles()) {
                    log.info("   - {}", f.getName());
                }
            }

            return ResponseEntity.notFound().build();
        }

        try {
            Resource resource = new FileSystemResource(arquivo);
            String contentType = Files.probeContentType(caminho);

            if (contentType == null) {
                if (nomeSeguro.toLowerCase().endsWith(".png")) {
                    contentType = "image/png";
                } else if (nomeSeguro.toLowerCase().endsWith(".jpg") || nomeSeguro.toLowerCase().endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (nomeSeguro.toLowerCase().endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            log.info("✅ Imagem servida com sucesso: {} ({}) - Tamanho: {} bytes",
                    nomeSeguro, contentType, arquivo.length());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("❌ Erro ao servir imagem: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/debug/{tipo}/{nomeArquivo:.+}")
    public ResponseEntity<String> debugImagem(
            @PathVariable String tipo,
            @PathVariable String nomeArquivo) {

        Path caminho = Paths.get(uploadDir, "imagens", tipo, nomeArquivo);
        File arquivo = caminho.toFile();

        StringBuilder debug = new StringBuilder();
        debug.append("📁 DEBUG DE IMAGEM\n");
        debug.append("==================\n");
        debug.append("Tipo: ").append(tipo).append("\n");
        debug.append("Arquivo: ").append(nomeArquivo).append("\n");
        debug.append("Caminho completo: ").append(caminho.toAbsolutePath()).append("\n");
        debug.append("Existe: ").append(arquivo.exists()).append("\n");

        if (arquivo.exists()) {
            debug.append("É arquivo: ").append(arquivo.isFile()).append("\n");
            debug.append("É legível: ").append(arquivo.canRead()).append("\n");
            debug.append("Tamanho: ").append(arquivo.length()).append(" bytes\n");
            try {
                debug.append("Content-Type: ").append(Files.probeContentType(caminho)).append("\n");
            } catch (Exception e) {
                debug.append("Content-Type: erro ao detectar\n");
            }
        } else {
            // Listar arquivos no diretório
            File dir = caminho.getParent().toFile();
            if (dir.exists() && dir.isDirectory()) {
                debug.append("\n📂 Arquivos disponíveis em ").append(dir.getPath()).append(":\n");
                for (File f : dir.listFiles()) {
                    debug.append("  - ").append(f.getName());
                    if (f.getName().toLowerCase().equals(nomeArquivo.toLowerCase())) {
                        debug.append(" ⬅️ CASE INSENSITIVE MATCH");
                    }
                    debug.append("\n");
                }
            } else {
                debug.append("\n❌ Diretório não encontrado: ").append(dir.getPath()).append("\n");
            }
        }

        return ResponseEntity.ok(debug.toString().replace("\n", "<br>"));
    }
}