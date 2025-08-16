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


    private String gerarCaminhoImagem(Long ean) {
        return ean + ".png";
    }
}