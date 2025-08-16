package redeinova.jornalfacil.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redeinova.jornalfacil.model.Tema;
import redeinova.jornalfacil.service.TemaService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/temas")
@RequiredArgsConstructor
public class TemaController {

    private final TemaService temaService;
    private static final String IMAGE_BASE_PATH = "E:/itensEncarteFacil/imagens/temas/";


    // Endpoint principal para servir imagens
    @GetMapping("/imagens/temas/{nome:.+}")
    public ResponseEntity<byte[]> getImagem(@PathVariable String nome) {
        try {
            Path imagePath = Paths.get(IMAGE_BASE_PATH + nome);

            if (!Files.exists(imagePath)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String mimeType = Files.probeContentType(imagePath);

            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
            if (mimeType == null) {
                if (nome.toLowerCase().endsWith(".webp")) {
                    mimeType = "image/webp";
                } else {
                    mimeType = "application/octet-stream";
                }
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao carregar imagem: " + e.getMessage()).getBytes());
        }
    }

    // Endpoint para streaming de imagens (melhor para grandes arquivos)
    @GetMapping("/imagens/stream/{nome:.+}")
    public void streamImagem(@PathVariable String nome, HttpServletResponse response) throws IOException {
        Path imagePath = Paths.get(IMAGE_BASE_PATH + nome);

        if (!Files.exists(imagePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null) {
            mimeType = "image/jpeg"; // Default para JPG se não puder detectar
        }

        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + nome + "\"");

        try (InputStream is = Files.newInputStream(imagePath);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096]; // Buffer maior para melhor performance
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        }
    }

    // Endpoint para verificação de imagens (debug)
    @GetMapping("/imagens/verificar/{nome:.+}")
    public ResponseEntity<String> verificarImagem(@PathVariable String nome) {
        Path imagePath = Paths.get(IMAGE_BASE_PATH + nome);

        try {
            String info = String.format(
                    "Imagem: %s\nExiste: %s\nTamanho: %d bytes\nTipo MIME: %s\nLegível: %s",
                    nome,
                    Files.exists(imagePath),
                    Files.exists(imagePath) ? Files.size(imagePath) : 0,
                    Files.exists(imagePath) ? Files.probeContentType(imagePath) : "N/A",
                    Files.exists(imagePath) ? Files.isReadable(imagePath) : false
            );
            return ResponseEntity.ok(info);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao verificar imagem: " + e.getMessage());
        }
    }

    // Endpoints para gerenciamento de temas
    @GetMapping("/buscar")
    public ResponseEntity<List<Tema>> buscarPorDescricao(@RequestParam String termo) {
        List<Tema> temas = temaService.buscarTemasPorDescricao(termo);
        return ResponseEntity.ok(temas);
    }

    @GetMapping
    public ResponseEntity<List<Tema>> listarTodos() {
        List<Tema> temas = temaService.listarTodos();
        return ResponseEntity.ok(temas);
    }
}