package redeinova.jornalfacil.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redeinova.jornalfacil.dto.NovoProdutoDTO;
import redeinova.jornalfacil.exception.EntityAlreadyExistsException;
import redeinova.jornalfacil.exception.InvalidProductDataException;
import redeinova.jornalfacil.model.ClassificacaoProduto;
import redeinova.jornalfacil.model.Produto;
import redeinova.jornalfacil.repository.ProdutoRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProdutoService {

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

        // Validações mais flexíveis
        if (produto.getPrecoPor() == null) {
            throw new InvalidProductDataException("Preço por é obrigatório");
        }

        if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Preço por não pode ser negativo");
        }

        // Para medicamentos, verificar apenas se precoDe não é negativo (pode ser zero para isentos)
        if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO &&
                produto.getPrecoDe() != null &&
                produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidProductDataException("Preço de não pode ser negativo para medicamentos");
        }

        return produtoRepository.save(produto);
    }

    // ProdutoService.java - Adicionar este método
    @Transactional
    public List<Produto> atualizarProdutosEmLote(List<Produto> produtos) {
        List<Produto> produtosAtualizados = new ArrayList<>();

        for (Produto produto : produtos) {
            if (!produtoRepository.existsById(produto.getId())) {
                throw new EntityNotFoundException("Produto não encontrado com ID: " + produto.getId());
            }

            // Validações
            if (produto.getPrecoPor() == null) {
                throw new InvalidProductDataException("Preço por é obrigatório para o produto ID: " + produto.getId());
            }

            if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidProductDataException("Preço por não pode ser negativo para o produto ID: " + produto.getId());
            }

            if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO &&
                    produto.getPrecoDe() != null &&
                    produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidProductDataException("Preço de não pode ser negativo para medicamento ID: " + produto.getId());
            }

            produtosAtualizados.add(produtoRepository.save(produto));
        }

        return produtosAtualizados;
    }

    private String gerarCaminhoImagem(Long ean) {
        return ean + ".png";
    }
}