package gonzaga.jornalfacil.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import gonzaga.jornalfacil.dto.NovoProdutoDTO;
import gonzaga.jornalfacil.exception.EntityAlreadyExistsException;
import gonzaga.jornalfacil.exception.InvalidProductDataException;
import gonzaga.jornalfacil.model.ClassificacaoProduto;
import gonzaga.jornalfacil.model.Produto;
import gonzaga.jornalfacil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ProdutoService {

    private static final Logger logger = LoggerFactory.getLogger(ProdutoService.class);
    private final ProdutoRepository produtoRepository;

    @Transactional
    public Produto criarProduto(NovoProdutoDTO dto) {
        if (produtoRepository.findByEan(dto.getEan()).isPresent()) {
            throw new EntityAlreadyExistsException("Já existe um produto com o EAN informado");
        }

        // Validação adicional para descrição
        if (dto.getDescricao() == null || dto.getDescricao().trim().isEmpty()) {
            throw new InvalidProductDataException("Descrição do produto é obrigatória");
        }

        if (dto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO && dto.getPrecoDe() == null) {
            throw new InvalidProductDataException("Preço de é obrigatório para medicamentos");
        }

        Produto produto = new Produto();
        produto.setEan(dto.getEan());
        produto.setDescricao(dto.getDescricao());
        produto.setPrecoDe(dto.getPrecoDe());
        produto.setPrecoPor(dto.getPrecoPor());
        produto.setClassificacao(dto.getClassificacao());
        produto.setCaminhoImagem(gerarCaminhoImagem(dto.getEan()));

        return produtoRepository.save(produto);
    }

    public List<Produto> buscarPorDescricao(String descricao) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new InvalidProductDataException("Parâmetro de busca não pode ser vazio");
        }
        return produtoRepository.findByDescricaoContainingIgnoreCase(descricao);
    }

    public List<Produto> listarTodos() {
        return produtoRepository.findAll();
    }

    public Produto buscarPorId(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Produto não encontrado com ID: " + id));
    }

    @Transactional
    public void excluirProduto(Long id) {
        if (!produtoRepository.existsById(id)) {
            throw new EntityNotFoundException("Produto não encontrado com ID: " + id);
        }
        produtoRepository.deleteById(id);
    }

    public Page<Produto> listarPaginado(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }

    @Transactional
    public Produto atualizarProduto(Produto produto) {
        if (!produtoRepository.existsById(produto.getId())) {
            throw new EntityNotFoundException("Produto não encontrado com ID: " + produto.getId());
        }

        // **CONVERSÃO DE CENTAVOS PARA REAIS - CORREÇÃO PRINCIPAL**
        if (produto.getPrecoPor() != null && produto.getPrecoPor().compareTo(new BigDecimal("100")) > 0) {
            BigDecimal precoPorCorrigido = produto.getPrecoPor().divide(new BigDecimal("100"));
            produto.setPrecoPor(precoPorCorrigido);
        }

        if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(new BigDecimal("100")) > 0) {
            BigDecimal precoDeCorrigido = produto.getPrecoDe().divide(new BigDecimal("100"));
            produto.setPrecoDe(precoDeCorrigido);
        }

        // Validações após a conversão
        if (produto.getPrecoPor() == null) {
            throw new InvalidProductDataException("Preço por é obrigatório");
        }

        if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Preço por não pode ser negativo");
        }

        // **VALIDAÇÃO CORRIGIDA PARA MEDICAMENTOS - CONSIDERAR ISENTOS**
        if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO) {
            // Para medicamentos isentos, precoDe pode ser null ou 0
            boolean isIsento = produto.getPrecoDe() == null ||
                    produto.getPrecoDe().compareTo(BigDecimal.ZERO) == 0;

            if (!isIsento) {
                // Apenas valida medicamentos NÃO isentos
                if (produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidProductDataException("Preço de não pode ser negativo para medicamentos");
                }

                // **CORREÇÃO: Permitir pequenas diferenças e ajustar automaticamente**
                BigDecimal diferenca = produto.getPrecoDe().subtract(produto.getPrecoPor());

                // Se precoDe for menor que precoPor, ajusta automaticamente
                if (diferenca.compareTo(BigDecimal.ZERO) < 0) {
                    logger.info("Ajustando automaticamente precoDe para produto ID {}: {} -> {}",
                            produto.getId(), produto.getPrecoDe(), produto.getPrecoPor());

                    // Define precoDe igual a precoPor
                    produto.setPrecoDe(produto.getPrecoPor());
                }
            }
            // Medicamentos isentos não precisam de validação adicional
        }

        return produtoRepository.save(produto);
    }

    @Transactional
    public List<Produto> atualizarProdutosEmLote(List<Produto> produtos) {
        List<Produto> produtosAtualizados = new ArrayList<>();

        for (Produto produto : produtos) {
            if (!produtoRepository.existsById(produto.getId())) {
                throw new EntityNotFoundException("Produto não encontrado com ID: " + produto.getId());
            }

            // **APLICAR MESMA CONVERSÃO DO MÉTODO INDIVIDUAL**
            if (produto.getPrecoPor() != null && produto.getPrecoPor().compareTo(new BigDecimal("100")) > 0) {
                BigDecimal precoPorCorrigido = produto.getPrecoPor().divide(new BigDecimal("100"));
                produto.setPrecoPor(precoPorCorrigido);
            }

            if (produto.getPrecoDe() != null && produto.getPrecoDe().compareTo(new BigDecimal("100")) > 0) {
                BigDecimal precoDeCorrigido = produto.getPrecoDe().divide(new BigDecimal("100"));
                produto.setPrecoDe(precoDeCorrigido);
            }

            // Validações após conversão
            if (produto.getPrecoPor() == null) {
                throw new InvalidProductDataException("Preço por é obrigatório para o produto ID: " + produto.getId());
            }

            if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidProductDataException("Preço por não pode ser negativo para o produto ID: " + produto.getId());
            }

            // **VALIDAÇÃO CORRIGIDA PARA LOTE - MESMA LÓGICA DO MÉTODO INDIVIDUAL**
            if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO) {
                if (produto.getPrecoDe() == null) {
                    throw new InvalidProductDataException("Preço de é obrigatório para medicamento ID: " + produto.getId());
                }

                if (produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidProductDataException("Preço de não pode ser negativo para medicamento ID: " + produto.getId());
                }

                // **APENAS VALIDAR SE precoDe > 0**
                if (produto.getPrecoDe().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal diferenca = produto.getPrecoDe().subtract(produto.getPrecoPor());

                    // Se precoDe for menor que precoPor, ajusta automaticamente
                    if (diferenca.compareTo(BigDecimal.ZERO) < 0) {
                        logger.info("Ajustando automaticamente precoDe para produto ID {} em lote: {} -> {}",
                                produto.getId(), produto.getPrecoDe(), produto.getPrecoPor());

                        // Define precoDe igual a precoPor
                        produto.setPrecoDe(produto.getPrecoPor());
                    }
                }
            }

            produtosAtualizados.add(produtoRepository.save(produto));
        }

        return produtosAtualizados;
    }

    private String gerarCaminhoImagem(Long ean) {
        return ean + ".png";
    }
}