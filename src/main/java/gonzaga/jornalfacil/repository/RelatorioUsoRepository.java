package gonzaga.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import gonzaga.jornalfacil.model.RelatorioUso;

import java.time.LocalDateTime;
import java.util.List;

public interface RelatorioUsoRepository extends JpaRepository<RelatorioUso, Long> {

    List<RelatorioUso> findByCodigoCliente(String codigoCliente);

    List<RelatorioUso> findByCodigoClienteAndDataUsoBetween(
            String codigoCliente, LocalDateTime inicio, LocalDateTime fim);

    @Query("SELECT COUNT(r) FROM RelatorioUso r WHERE r.codigoCliente = :codigoCliente AND r.dataUso >= :dataInicio")
    Long countByCodigoClienteSince(@Param("codigoCliente") String codigoCliente,
                                   @Param("dataInicio") LocalDateTime dataInicio);

    @Query("SELECT r FROM RelatorioUso r WHERE r.dataUso BETWEEN :inicio AND :fim ORDER BY r.dataUso DESC")
    List<RelatorioUso> findByPeriodo(@Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim);

    @Query("SELECT r.codigoCliente, COUNT(r) as total FROM RelatorioUso r " +
            "WHERE r.dataUso BETWEEN :inicio AND :fim " +
            "GROUP BY r.codigoCliente ORDER BY total DESC")
    List<Object[]> findUsoPorClienteNoPeriodo(@Param("inicio") LocalDateTime inicio,
                                              @Param("fim") LocalDateTime fim);

    @Query("SELECT r.tipoGeracao, COUNT(r) FROM RelatorioUso r " +
            "WHERE r.dataUso BETWEEN :inicio AND :fim " +
            "GROUP BY r.tipoGeracao")
    List<Object[]> findUsoPorTipoGeracao(@Param("inicio") LocalDateTime inicio,
                                         @Param("fim") LocalDateTime fim);
}