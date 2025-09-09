package redeinova.jornalfacil.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TemaDTO {
    @NotBlank(message = "Nome é obrigatório")
    private String nome;

    @NotBlank(message = "Descrição é obrigatória")
    private String descricao;
}