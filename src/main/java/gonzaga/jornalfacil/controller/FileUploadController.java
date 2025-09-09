package gonzaga.jornalfacil.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import gonzaga.jornalfacil.service.FileStorageService;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "http://localhost:3000")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file, "produto");
        return ResponseEntity.ok("Arquivo armazenado com sucesso: " + fileName);
    }

    @GetMapping("/listar")
    public ResponseEntity<List<String>> listarArquivos(@RequestParam String subdirectory) {
        List<String> arquivos = fileStorageService.listarArquivos(subdirectory);
        return ResponseEntity.ok(arquivos);
    }
}
