package gonzaga.jornalfacil.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus; // IMPORT ADICIONADO AQUI
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import gonzaga.jornalfacil.model.Rodape;
import gonzaga.jornalfacil.service.RodapeService;
import gonzaga.jornalfacil.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/rodapes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class RodapeController {

    private static final String IMAGE_BASE_PATH = "E:/itensEncarteFacil/imagens/rodapes/";
    private final RodapeService rodapeService;
    private final FileStorageService fileStorageService;

    public RodapeController(RodapeService rodapeService, FileStorageService fileStorageService) {
        this.rodapeService = rodapeService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping("/imagens/rodapes/{nome:.+}")
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

    // NOVO: Endpoint para criar rodapé
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Rodape> criarRodape(
            @RequestParam("codigoLoja") Long codigoLoja,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            Rodape rodape = new Rodape();
            rodape.setCodigoLoja(codigoLoja);

            if (imagem != null && !imagem.isEmpty()) {
                String nomeArquivo = fileStorageService.storeFile(imagem, "rodape");
                rodape.setCaminhoImagem("/imagens/rodapes/" + nomeArquivo);
            } else {
                // Usar imagem padrão ou gerar uma
                rodape.setCaminhoImagem("/imagens/rodapes/rodape_padrao.png");
            }

            Rodape rodapeSalvo = rodapeService.salvar(rodape);
            return ResponseEntity.status(HttpStatus.CREATED).body(rodapeSalvo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // NOVO: Endpoint para atualizar rodapé
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Rodape> atualizarRodape(
            @PathVariable Long id,
            @RequestParam("codigoLoja") Long codigoLoja,
            @RequestParam(value = "imagem", required = false) MultipartFile imagem) {
        try {
            Rodape rodapeExistente = rodapeService.buscarPorId(id)
                    .orElseThrow(() -> new RuntimeException("Rodapé não encontrado"));

            rodapeExistente.setCodigoLoja(codigoLoja);

            if (imagem != null && !imagem.isEmpty()) {
                String nomeArquivo = fileStorageService.storeFile(imagem, "rodape");
                rodapeExistente.setCaminhoImagem("/imagens/rodapes/" + nomeArquivo);
            }

            Rodape rodapeAtualizado = rodapeService.salvar(rodapeExistente);
            return ResponseEntity.ok(rodapeAtualizado);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // NOVO: Endpoint para deletar rodapé
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarRodape(@PathVariable Long id) {
        try {
            rodapeService.deletarPorId(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // NOVO: Endpoint para buscar rodapé por ID
    @GetMapping("/{id}")
    public ResponseEntity<Rodape> buscarPorId(@PathVariable Long id) {
        return rodapeService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}

// RodapeController.java
//package gonzaga.jornalfacil.controller;
//
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import gonzaga.jornalfacil.model.Rodape;
//import gonzaga.jornalfacil.service.RodapeService;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/rodapes")
//public class RodapeController {
//
//    private static final String IMAGE_BASE_PATH = "E:/itensEncarteFacil/imagens/rodapes/";
//    private final RodapeService rodapeService;
//
//    public RodapeController(RodapeService rodapeService) {
//        this.rodapeService = rodapeService;
//    }
//
//    @GetMapping("/imagens/rodapes/{nome:.+}")
//    public ResponseEntity<byte[]> getImagem(@PathVariable String nome) {
//        try {
//            Path imagePath = Paths.get(IMAGE_BASE_PATH + nome);
//
//            if (!Files.exists(imagePath)) {
//                return ResponseEntity.notFound().build();
//            }
//
//            byte[] imageBytes = Files.readAllBytes(imagePath);
//            return ResponseEntity.ok()
//                    .contentType(MediaType.IMAGE_PNG)
//                    .body(imageBytes);
//        } catch (IOException e) {
//            return ResponseEntity.internalServerError().build();
//        }
//    }
//
//    @GetMapping
//    public ResponseEntity<List<Rodape>> listarTodos() {
//        List<Rodape> rodapes = rodapeService.listarTodos();
//        return ResponseEntity.ok(rodapes);
//    }
//
//    @GetMapping("/buscar")
//    public ResponseEntity<List<Rodape>> buscarPorCodigo(@RequestParam String codigo) {
//        List<Rodape> rodapes = rodapeService.buscarPorCodigo(codigo);
//        return ResponseEntity.ok(rodapes);
//    }
//}