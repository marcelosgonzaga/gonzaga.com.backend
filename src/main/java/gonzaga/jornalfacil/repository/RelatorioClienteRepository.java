package gonzaga.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import gonzaga.jornalfacil.model.RelatorioCliente;

import java.util.List;
import java.util.Optional;

public interface RelatorioClienteRepository extends JpaRepository<RelatorioCliente, Long> {

    Optional<RelatorioCliente> findByCodigoCliente(String codigoCliente);

    List<RelatorioCliente> findByAtivoTrue();

    @Query("SELECT c FROM RelatorioCliente c WHERE c.codigoCliente LIKE %:codigo%")
    List<RelatorioCliente> findByCodigoClienteContaining(String codigo);
}