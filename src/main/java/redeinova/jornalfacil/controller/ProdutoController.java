package redeinova.jornalfacil.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redeinova.jornalfacil.dto.NovoProdutoDTO;
import redeinova.jornalfacil.dto.ProdutoResponse;
import redeinova.jornalfacil.model.Produto;
import redeinova.jornalfacil.service.ProdutoService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ProdutoController {
    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listarTodos() {
        List<Produto> produtos = produtoService.listarTodos();
        List<ProdutoResponse> response = produtos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        Produto produto = produtoService.buscarPorId(id);
        return ResponseEntity.ok(toResponse(produto));
    }

    @PostMapping
    public ResponseEntity<ProdutoResponse> criarProduto(@RequestBody @Valid NovoProdutoDTO dto) {
        Produto produto = produtoService.criarProduto(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(produto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluirProduto(@PathVariable Long id) {
        produtoService.excluirProduto(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<ProdutoResponse>> buscarPorDescricao(
            @RequestParam String descricao) {
        List<Produto> produtos = produtoService.buscarPorDescricao(descricao);
        List<ProdutoResponse> response = produtos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // Versão paginada alternativa (opcional)
    @GetMapping("/paginado")
    public ResponseEntity<Page<ProdutoResponse>> listarPaginado(Pageable pageable) {
        Page<Produto> produtos = produtoService.listarPaginado(pageable);
        Page<ProdutoResponse> response = produtos.map(this::toResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/imagens/{nome:.+}")
    public ResponseEntity<byte[]> getImagemProduto(@PathVariable String nome) {
        try {
            Path imagePath = Paths.get("E:/itensEncarteFacil/imagens/produtos/" + nome);

            if (!Files.exists(imagePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);
            String mimeType = Files.probeContentType(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType != null ? mimeType : "image/png"))
                    .body(imageBytes);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private ProdutoResponse toResponse(Produto produto) {
        return ProdutoResponse.builder()
                .id(produto.getId())
                .ean(produto.getEan())
                .descricao(produto.getDescricao())
                .precoDe(produto.getPrecoDe())
                .precoPor(produto.getPrecoPor())
                .classificacao(produto.getClassificacao())
                .caminhoImagem(produto.getCaminhoImagem())
                .build();
    }
}