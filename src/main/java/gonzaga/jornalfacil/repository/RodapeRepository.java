package gonzaga.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import gonzaga.jornalfacil.model.Rodape;

import java.util.List;
import java.util.Optional;

public interface RodapeRepository extends JpaRepository<Rodape, Long> {

    // Método para buscar por código exato
    Optional<Rodape> findByCodigoLoja(Long codigoLoja);

    // Método para buscar rodapés com códigos que começam com determinado valor
    @Query("SELECT r FROM Rodape r WHERE CAST(r.codigoLoja AS string) LIKE CONCAT(:codigo, '%')")
    List<Rodape> findByCodigoLojaStartingWith(@Param("codigo") String codigo);

    // Método para buscar rodapés com códigos que terminam com determinado valor
    @Query("SELECT r FROM Rodape r WHERE CAST(r.codigoLoja AS string) LIKE CONCAT('%', :codigo)")
    List<Rodape> findByCodigoLojaEndingWith(@Param("codigo") String codigo);

    // Método para buscar rodapés com códigos que contêm determinado valor
    @Query("SELECT r FROM Rodape r WHERE CAST(r.codigoLoja AS string) LIKE CONCAT('%', :codigo, '%')")
    List<Rodape> findByCodigoLojaContaining(@Param("codigo") String codigo);

    // Método para buscar por intervalo de códigos
    List<Rodape> findByCodigoLojaBetween(Long start, Long end);

    // Método para buscar rodapés com códigos maiores que
    List<Rodape> findByCodigoLojaGreaterThan(Long codigo);

    // Método para buscar rodapés com códigos menores que
    List<Rodape> findByCodigoLojaLessThan(Long codigo);
}