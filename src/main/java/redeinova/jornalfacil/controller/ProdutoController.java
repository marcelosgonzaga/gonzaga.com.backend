package redeinova.jornalfacil.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import redeinova.jornalfacil.dto.NovoProdutoDTO;
import redeinova.jornalfacil.dto.ProdutoResponse;
import redeinova.jornalfacil.model.Produto;
import redeinova.jornalfacil.service.ProdutoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ProdutoController {
    private final ProdutoService produtoService;

    @GetMapping
    public ResponseEntity<List<ProdutoResponse>> listarTodos() {
        List<Produto> produtos = produtoService.listarTodos();
        return ResponseEntity.ok(toResponseList(produtos));
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
        return ResponseEntity.ok(toResponseList(produtos));
    }

    @GetMapping("/paginado")
    public ResponseEntity<Page<ProdutoResponse>> listarPaginado(Pageable pageable) {
        Page<Produto> produtos = produtoService.listarPaginado(pageable);
        return ResponseEntity.ok(produtos.map(this::toResponse));
    }

    @PutMapping("/{id}/precos")
    public ResponseEntity<ProdutoResponse> atualizarPrecos(@PathVariable Long id, @RequestBody Map<String, BigDecimal> precos) {
        try {
            Produto produto = produtoService.buscarPorId(id);

            if (precos.containsKey("precoDe")) {
                produto.setPrecoDe(precos.get("precoDe"));
            }

            if (precos.containsKey("precoPor")) {
                produto.setPrecoPor(precos.get("precoPor"));
            }

            // Salvar as alterações - você precisará adicionar um método no serviço para isso
            Produto produtoAtualizado = produtoService.atualizarProduto(produto);
            return ResponseEntity.ok(toResponse(produtoAtualizado));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ProdutoController.java - Adicionar este método
    @PutMapping("/precos-em-lote")
    public ResponseEntity<List<ProdutoResponse>> atualizarPrecosEmLote(@RequestBody List<Map<String, Object>> produtosComPrecos) {
        try {
            List<ProdutoResponse> produtosAtualizados = new ArrayList<>();

            for (Map<String, Object> produtoPreco : produtosComPrecos) {
                Long id = Long.valueOf(produtoPreco.get("id").toString());
                Produto produto = produtoService.buscarPorId(id);

                if (produtoPreco.containsKey("precoDe")) {
                    BigDecimal precoDe = new BigDecimal(produtoPreco.get("precoDe").toString());
                    produto.setPrecoDe(precoDe);
                }

                if (produtoPreco.containsKey("precoPor")) {
                    BigDecimal precoPor = new BigDecimal(produtoPreco.get("precoPor").toString());
                    produto.setPrecoPor(precoPor);
                }

                Produto produtoAtualizado = produtoService.atualizarProduto(produto);
                produtosAtualizados.add(toResponse(produtoAtualizado));
            }

            return ResponseEntity.ok(produtosAtualizados);
        } catch (Exception e) {
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

    private List<ProdutoResponse> toResponseList(List<Produto> produtos) {
        return produtos.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}