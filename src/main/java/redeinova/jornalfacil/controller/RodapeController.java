// RodapeController.java
package redeinova.jornalfacil.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redeinova.jornalfacil.model.Rodape;
import redeinova.jornalfacil.service.RodapeService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/rodapes")
public class RodapeController {

    private static final String IMAGE_BASE_PATH = "E:/itensEncarteFacil/imagens/rodapes/";
    private final RodapeService rodapeService;

    public RodapeController(RodapeService rodapeService) {
        this.rodapeService = rodapeService;
    }

    @GetMapping("/imagens/{nome:.+}")
    public ResponseEntity<byte[]> getImagem(@PathVariable String nome) {
        try {
            Path imagePath = Paths.get(IMAGE_BASE_PATH + nome);

            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Rodape>> listarTodos() {
        List<Rodape> rodapes = rodapeService.listarTodos();
        return ResponseEntity.ok(rodapes);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Rodape>> buscarPorCodigo(@RequestParam String codigo) {
        List<Rodape> rodapes = rodapeService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(rodapes);
    }
}