package gonzaga.jornalfacil.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProdutoImagemController {

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @GetMapping("/imagens/{nomeArquivo:.+}")
    public ResponseEntity<byte[]> getImagemProduto(@PathVariable String nomeArquivo) {
        try {
            // Sanitize filename
            String safeFileName = nomeArquivo.replace("..", "").replace("/", "");

            Path imagePath = Paths.get(produtosDir + safeFileName);

            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String mimeType = Files.probeContentType(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "image/png"))
                    .body(imageBytes);
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping("/{id}/upload-imagem")
    public ResponseEntity<String> uploadImagemProduto(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("Arquivo vazio");
            }

            // Garante que o diretório existe
            Files.createDirectories(Paths.get(produtosDir));

            // Gera um nome de arquivo seguro
            String originalFilename = file.getOriginalFilename();
            String fileExtension = originalFilename != null ?
                    originalFilename.substring(originalFilename.lastIndexOf(".")) : ".png";
            String novoNomeArquivo = "produto_" + id + fileExtension;

            // Salva o arquivo
            Path destino = Paths.get(produtosDir + novoNomeArquivo);
            file.transferTo(destino.toFile());

            return ResponseEntity.ok(novoNomeArquivo);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Erro ao salvar imagem");
        }
    }
}