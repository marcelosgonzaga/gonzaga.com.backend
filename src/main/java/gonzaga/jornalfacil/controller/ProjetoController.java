package gonzaga.jornalfacil.controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import gonzaga.jornalfacil.dto.AtualizarProjetoDTO;
import gonzaga.jornalfacil.dto.NovoProjetoDTO;
import gonzaga.jornalfacil.model.Projeto;
import gonzaga.jornalfacil.service.ProjetoService;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/projetos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ProjetoController {

    private final ProjetoService projetoService;

    @GetMapping
    public ResponseEntity<Page<Projeto>> listarTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Projeto> projetos = projetoService.listarTodos(pageable);
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<Projeto>> buscarPorPeriodo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        List<Projeto> projetos = projetoService.buscarPorPeriodo(inicio, fim);
        return ResponseEntity.ok(projetos);
    }

    @PostMapping
    public ResponseEntity<Projeto> criarProjeto(@RequestBody @Valid NovoProjetoDTO dto) {
        Projeto projeto = projetoService.criarProjeto(dto);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Location", "/api/projetos/" + projeto.getId())
                .body(projeto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Projeto> buscarPorId(@PathVariable Long id) {
        Projeto projeto = projetoService.buscarPorId(id);
        return ResponseEntity.ok(projeto);
    }

    @GetMapping("/{id}/pdf")
    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
    public ResponseEntity<byte[]> gerarPdf(
            @PathVariable Long id,
            @RequestParam(defaultValue = "SISTEMA") String codigoCliente) { // ✅ ADICIONADO parâmetro codigoCliente
        try {
            byte[] pdf = projetoService.gerarPdfProjeto(id, codigoCliente); // ✅ AGORA COM 2 PARÂMETROS
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=encarte.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao gerar PDF: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/{id}/jpg")
    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
    public ResponseEntity<byte[]> gerarJpg(@PathVariable Long id) {
        try {
            byte[] jpg = projetoService.gerarImagemProjeto(id);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=encarte.jpg")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(jpg);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao gerar JPG: " + e.getMessage()).getBytes());
        }
    }

    // ✅ NOVO ENDPOINT: Gerar PDF via SSR
    @GetMapping("/{id}/pdf-ssr")
    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
    public ResponseEntity<byte[]> gerarPdfSSR(
            @PathVariable Long id,
            @RequestParam(defaultValue = "SISTEMA") String codigoCliente) {
        try {
            byte[] pdf = projetoService.gerarPdfProjetoSSR(id, codigoCliente);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=encarte_ssr.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao gerar PDF via SSR: " + e.getMessage()).getBytes());
        }
    }

    // ✅ NOVO ENDPOINT: Gerar JPG via SSR
    @GetMapping("/{id}/jpg-ssr")
    @CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
    public ResponseEntity<byte[]> gerarJpgSSR(
            @PathVariable Long id,
            @RequestParam(defaultValue = "SISTEMA") String codigoCliente) {
        try {
            byte[] jpg = projetoService.gerarJpgProjetoSSR(id, codigoCliente);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=encarte_ssr.jpg")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(jpg);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro ao gerar JPG via SSR: " + e.getMessage()).getBytes());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Projeto> atualizarProjeto(
            @PathVariable Long id,
            @RequestBody @Valid AtualizarProjetoDTO dto) {
        Projeto projetoAtualizado = projetoService.atualizarProjeto(id, dto);
        return ResponseEntity.ok(projetoAtualizado);
    }
}