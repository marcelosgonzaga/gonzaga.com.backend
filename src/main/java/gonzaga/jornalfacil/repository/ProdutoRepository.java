package gonzaga.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import gonzaga.jornalfacil.model.ClassificacaoProduto;
import gonzaga.jornalfacil.model.Produto;

import java.util.List;
import java.util.Optional;

public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    List<Produto> findByDescricaoContainingIgnoreCase(String descricao);
    Optional<Produto>  findByEan(Long ean);
    List<Produto> findByClassificacao(ClassificacaoProduto classificacao);
}
