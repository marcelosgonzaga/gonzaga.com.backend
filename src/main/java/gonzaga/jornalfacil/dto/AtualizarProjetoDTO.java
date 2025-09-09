package gonzaga.jornalfacil.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Data
@RequiredArgsConstructor
public class AtualizarProjetoDTO {
    @NotNull
    private LocalDate dataInicio;

    @NotNull
    private LocalDate dataFim;

    private Long rodapeId;

}