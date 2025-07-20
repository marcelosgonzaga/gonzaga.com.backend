package redeinova.jornalfacil.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class NovoProjetoDTO {
    @NotNull(message = "ID do tema é obrigatório")
    private Long temaId;

    @NotNull(message = "Codigo do rodapé é obrigatório")
    private Long codigoLoja;

    @NotNull(message = "Data início é obrigatoria ")
    private LocalDate dataInicio;

    @NotNull(message = "Data final é obrigatória")
    private LocalDate dataFim;

    @NotEmpty(message = "Lista de produtos não poser vazia")
    private List<Long>produtoIds;

}
