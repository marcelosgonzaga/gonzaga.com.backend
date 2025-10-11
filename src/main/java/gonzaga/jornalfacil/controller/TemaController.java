package gonzaga.jornalfacil.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import gonzaga.jornalfacil.model.Tema;
import gonzaga.jornalfacil.service.TemaService;

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

    @Value("${file.temas-dir}")
    private String temasDir;

    // NOVO: Endpoint para criar tema
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tema> criarTema(
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam("imagem") MultipartFile imagem) {
        try {
            Tema novoTema = temaService.criarTema(nome, descricao, imagem);
            return ResponseEntity.status(HttpStatus.CREATED).body(novoTema);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NOVO: Endpoint para atualizar tema
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Tema> atualizarTema(
            @PathVariable Long id,
            @RequestParam("nome") String nome,
            @RequestParam("descricao") String descricao,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            Tema temaAtualizado = temaService.atualizarTema(id, nome, descricao, imagem);
            return ResponseEntity.ok(temaAtualizado);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // NOVO: Endpoint para deletar tema
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarTema(@PathVariable Long id) {
        try {
            temaService.deletarTema(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Endpoint principal para servir imagens - CORRIGIDO
    @GetMapping("/imagens/temas/{nome:.+}")
    public ResponseEntity<byte[]> getImagem(@PathVariable String nome) {
        try {
            // Sanitize filename
            String safeFileName = nome.replace("..", "").replace("/", "").replace("\\", "");

            Path imagePath = Paths.get(temasDir + safeFileName);
            System.out.println("Tentando carregar imagem: " + imagePath.toString());

            if (!Files.exists(imagePath)) {
                System.out.println("Arquivo não encontrado: " + imagePath.toString());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String mimeType = Files.probeContentType(imagePath);

            // Determinar MIME type correto para WebP
            if (mimeType == null) {
                if (safeFileName.toLowerCase().endsWith(".webp")) {
                    mimeType = "image/webp";
                } else if (safeFileName.toLowerCase().endsWith(".png")) {
                    mimeType = "image/png";
                } else if (safeFileName.toLowerCase().endsWith(".jpg") || safeFileName.toLowerCase().endsWith(".jpeg")) {
                    mimeType = "image/jpeg";
                } else {
                    mimeType = "application/octet-stream";
                }
            }

            System.out.println("Imagem carregada com sucesso: " + safeFileName + ", tipo: " + mimeType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(imageBytes);
        } catch (IOException e) {
            System.out.println("Erro ao carregar imagem: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao carregar imagem: " + e.getMessage()).getBytes());
        }
    }

    // Endpoint para streaming de imagens (melhor para grandes arquivos)
    @GetMapping("/imagens/stream/{nome:.+}")
    public void streamImagem(@PathVariable String nome, HttpServletResponse response) throws IOException {
        // Sanitize filename
        String safeFileName = nome.replace("..", "").replace("/", "").replace("\\", "");

        Path imagePath = Paths.get(temasDir + safeFileName);

        if (!Files.exists(imagePath)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null) {
            // Determinar MIME type baseado na extensão do arquivo
            if (safeFileName.toLowerCase().endsWith(".webp")) {
                mimeType = "image/webp";
            } else if (safeFileName.toLowerCase().endsWith(".png")) {
                mimeType = "image/png";
            } else if (safeFileName.toLowerCase().endsWith(".jpg") || safeFileName.toLowerCase().endsWith(".jpeg")) {
                mimeType = "image/jpeg";
            } else {
                mimeType = "application/octet-stream";
            }
        }

        response.setContentType(mimeType);
        response.setHeader("Content-Disposition", "inline; filename=\"" + safeFileName + "\"");

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
        // Sanitize filename
        String safeFileName = nome.replace("..", "").replace("/", "").replace("\\", "");

        Path imagePath = Paths.get(temasDir + safeFileName);

        try {
            String info = String.format(
                    "Imagem: %s\nExiste: %s\nTamanho: %d bytes\nTipo MIME: %s\nLegível: %s",
                    safeFileName,
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

    // Adicione este método no TemaController para debug
    @GetMapping("/debug/imagens/{nome:.+}")
    public ResponseEntity<String> debugImagem(@PathVariable String nome) {
        try {
            String safeFileName = nome.replace("..", "").replace("/", "").replace("\\", "");
            Path imagePath = Paths.get(temasDir + safeFileName);

            String debugInfo = String.format(
                    "Arquivo: %s\n" +
                            "Caminho completo: %s\n" +
                            "Existe: %s\n" +
                            "É legível: %s\n" +
                            "Tamanho: %d bytes\n" +
                            "Tipo MIME: %s",
                    safeFileName,
                    imagePath.toString(),
                    Files.exists(imagePath),
                    Files.exists(imagePath) ? Files.isReadable(imagePath) : false,
                    Files.exists(imagePath) ? Files.size(imagePath) : 0,
                    Files.exists(imagePath) ? Files.probeContentType(imagePath) : "N/A"
            );

            return ResponseEntity.ok(debugInfo);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro no debug: " + e.getMessage());
        }
    }
}