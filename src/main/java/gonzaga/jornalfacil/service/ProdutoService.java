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
        logger.info("Atualizando produto ID: {} - PrecoDe: {}, PrecoPor: {}, Classificacao: {}",
                produto.getId(), produto.getPrecoDe(), produto.getPrecoPor(), produto.getClassificacao());

        if (!produtoRepository.existsById(produto.getId())) {
            throw new EntityNotFoundException("Produto não encontrado com ID: " + produto.getId());
        }

        // Validações diretas
        if (produto.getPrecoPor() == null) {
            throw new InvalidProductDataException("Preço por é obrigatório");
        }

        if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Preço por não pode ser negativo");
        }

        // Validação para medicamentos
        if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO) {
            boolean isIsento = produto.getPrecoDe() == null ||
                    produto.getPrecoDe().compareTo(BigDecimal.ZERO) == 0;

            if (!isIsento) {
                if (produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
                    throw new InvalidProductDataException("Preço de não pode ser negativo para medicamentos");
                }

                // Ajustar automaticamente se precoDe < precoPor
                if (produto.getPrecoDe().compareTo(produto.getPrecoPor()) < 0) {
                    logger.info("Ajustando automaticamente precoDe para produto ID {}: {} -> {}",
                            produto.getId(), produto.getPrecoDe(), produto.getPrecoPor());
                    produto.setPrecoDe(produto.getPrecoPor());
                }
            }
        }

        Produto produtoSalvo = produtoRepository.save(produto);
        logger.info("Produto salvo - ID: {} - PrecoDe: {}, PrecoPor: {}",
                produtoSalvo.getId(), produtoSalvo.getPrecoDe(), produtoSalvo.getPrecoPor());

        return produtoSalvo;
    }



    private String gerarCaminhoImagem(Long ean) {
        return ean + ".png";
    }
}