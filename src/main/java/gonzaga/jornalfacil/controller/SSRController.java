package gonzaga.jornalfacil.controller;

import gonzaga.jornalfacil.service.ProjetoService;
import gonzaga.jornalfacil.service.SSRService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ssr")
@RequiredArgsConstructor
@Slf4j
public class SSRController {

    private final ProjetoService projetoService;
    private final SSRService ssrService;

    // CORREÇÃO: Remover "/api" duplicado da URL
    @GetMapping("/projetos/{id}/pdf")
    public ResponseEntity<byte[]> gerarPdfSSR(@PathVariable Long id) {
        try {
            log.info("📥 Recebida requisição para gerar PDF SSR do projeto ID: {}", id);

            var projeto = projetoService.buscarPorId(id);
            log.debug("Projeto encontrado: ID {}, Tema: {}, Produtos: {}",
                    projeto.getId(),
                    projeto.getTema() != null ? projeto.getTema().getId() : "null",
                    projeto.getProdutos() != null ? projeto.getProdutos().size() : 0);

            byte[] pdfBytes = ssrService.gerarPdfProjeto(projeto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename",
                    String.format("encarte_%d_ssr.pdf", projeto.getId()));
            headers.setContentLength(pdfBytes.length);

            log.info("✅ PDF SSR gerado com sucesso - Tamanho: {} bytes", pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ Erro na geração SSR do PDF para projeto {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro na geração SSR do PDF: " + e.getMessage()).getBytes());
        }
    }

    // NOVO ENDPOINT: Gerar JPG via SSR
    @GetMapping("/projetos/{id}/jpg")
    public ResponseEntity<byte[]> gerarJpgSSR(@PathVariable Long id) {
        try {
            log.info("📥 Recebida requisição para gerar JPG SSR do projeto ID: {}", id);

            var projeto = projetoService.buscarPorId(id);
            log.debug("Projeto encontrado: ID {}, Tema: {}, Produtos: {}",
                    projeto.getId(),
                    projeto.getTema() != null ? projeto.getTema().getId() : "null",
                    projeto.getProdutos() != null ? projeto.getProdutos().size() : 0);

            byte[] jpgBytes = ssrService.gerarJpgProjeto(projeto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_JPEG);
            headers.setContentDispositionFormData("filename",
                    String.format("encarte_%d_ssr.jpg", projeto.getId()));
            headers.setContentLength(jpgBytes.length);

            log.info("✅ JPG SSR gerado com sucesso - Tamanho: {} bytes", jpgBytes.length);
            return new ResponseEntity<>(jpgBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("❌ Erro na geração SSR do JPG para projeto {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erro na geração SSR do JPG: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("SSR Controller is healthy");
    }
}