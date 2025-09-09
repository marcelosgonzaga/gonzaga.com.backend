package gonzaga.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import gonzaga.jornalfacil.model.Tema;

import java.util.List;
import java.util.Optional;

public interface TemaRepository extends JpaRepository<Tema, Long> {
    // Consulta JPQL personalizada para busca case-insensitive
    @Query("SELECT t FROM Tema t WHERE LOWER(t.descricao) LIKE LOWER(CONCAT('%', :descricao, '%'))")
    List<Tema> buscarPorDescricao(@Param("descricao") String descricao);

    // Consulta para busca exata (opcional)
    Optional<Tema> findByDescricao(String descricao);
}
