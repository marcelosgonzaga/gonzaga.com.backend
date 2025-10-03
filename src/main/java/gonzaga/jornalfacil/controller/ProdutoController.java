package gonzaga.jornalfacil.controller;

import gonzaga.jornalfacil.exception.InvalidProductDataException;
import gonzaga.jornalfacil.model.ClassificacaoProduto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import gonzaga.jornalfacil.dto.NovoProdutoDTO;
import gonzaga.jornalfacil.dto.ProdutoResponse;
import gonzaga.jornalfacil.model.Produto;
import gonzaga.jornalfacil.service.ProdutoService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RestController
@RequestMapping("/api/produtos")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class ProdutoController {
    private static final Logger logger = LoggerFactory.getLogger(ProdutoController.class);
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

    @PutMapping("/precos-em-lote")
    public ResponseEntity<List<ProdutoResponse>> atualizarPrecosEmLote(@RequestBody List<Map<String, Object>> produtosComPrecos) {
        try {
            logger.info("Recebendo atualização em lote para {} produtos", produtosComPrecos.size());

            List<ProdutoResponse> produtosAtualizados = new ArrayList<>();

            for (Map<String, Object> produtoPreco : produtosComPrecos) {
                Long id = Long.valueOf(produtoPreco.get("id").toString());
                Boolean isIsento = produtoPreco.containsKey("isento") ?
                        Boolean.valueOf(produtoPreco.get("isento").toString()) : false;

                logger.info("Processando produto ID: {}, precoDe: {}, precoPor: {}, isento: {}",
                        id, produtoPreco.get("precoDe"), produtoPreco.get("precoPor"), isIsento);

                Produto produto = produtoService.buscarPorId(id);

                // Processar precoDe
                if (produtoPreco.containsKey("precoDe")) {
                    Object precoDeObj = produtoPreco.get("precoDe");
                    if (precoDeObj != null && !precoDeObj.toString().isEmpty()) {
                        try {
                            // Converter para BigDecimal diretamente (já está em reais)
                            BigDecimal precoDe = new BigDecimal(precoDeObj.toString());
                            produto.setPrecoDe(precoDe);
                            logger.info("Produto {} - PrecoDe definido como: {}", id, precoDe);
                        } catch (NumberFormatException e) {
                            logger.warn("Formato inválido para precoDe do produto {}: {}", id, precoDeObj);
                            // Para medicamentos isentos, pode ser null
                            if (!isIsento || produto.getClassificacao() != ClassificacaoProduto.MEDICAMENTO) {
                                produto.setPrecoDe(null);
                            }
                        }
                    } else {
                        // Para medicamentos isentos, pode ser null
                        if (!isIsento || produto.getClassificacao() != ClassificacaoProduto.MEDICAMENTO) {
                            produto.setPrecoDe(null);
                        }
                    }
                }

                // Processar precoPor (OBRIGATÓRIO)
                if (produtoPreco.containsKey("precoPor")) {
                    Object precoPorObj = produtoPreco.get("precoPor");
                    if (precoPorObj != null && !precoPorObj.toString().isEmpty()) {
                        try {
                            // Converter para BigDecimal diretamente (já está em reais)
                            BigDecimal precoPor = new BigDecimal(precoPorObj.toString());
                            produto.setPrecoPor(precoPor);
                            logger.info("Produto {} - PrecoPor definido como: {}", id, precoPor);
                        } catch (NumberFormatException e) {
                            logger.error("Preço por inválido para o produto ID: {} - valor: {}", id, precoPorObj);
                            throw new InvalidProductDataException("Preço por inválido para o produto ID: " + id);
                        }
                    } else {
                        logger.error("Preço por é obrigatório para o produto ID: {}", id);
                        throw new InvalidProductDataException("Preço por é obrigatório para o produto ID: " + id);
                    }
                } else {
                    logger.error("Campo precoPor não encontrado para o produto ID: {}", id);
                    throw new InvalidProductDataException("Campo precoPor é obrigatório para o produto ID: " + id);
                }

                Produto produtoAtualizado = produtoService.atualizarProduto(produto);
                produtosAtualizados.add(toResponse(produtoAtualizado));
            }

            return ResponseEntity.ok(produtosAtualizados);
        } catch (Exception e) {
            logger.error("Erro ao atualizar preços em lote: {}", e.getMessage(), e);
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