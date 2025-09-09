package redeinova.jornalfacil.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import redeinova.jornalfacil.model.Projeto;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProjetoRepository extends JpaRepository<Projeto, Long> {
    List<Projeto>findByDataInicioBetween(LocalDate inicio, LocalDate fim);
    List<Projeto>findByRodapeCodigoLoja(Long codigoLoja);
}
