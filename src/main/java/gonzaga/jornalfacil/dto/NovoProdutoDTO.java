package gonzaga.jornalfacil.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import gonzaga.jornalfacil.model.ClassificacaoProduto;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NovoProdutoDTO {

    @NotNull(message = "EAN é obrigatório")
    @Positive(message = "EAN deve ser um número positivo")
    private Long ean;

    @NotNull
    @Size(max = 255, message = "Descrição deve ter no maximo 255 caracteres")
    private String descricao;

    @DecimalMin(value = "0.0", inclusive = false, message = "Preço De: deve ser maior que zero")
    private BigDecimal precoDe;

    @NotNull(message = "Preço Por: é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "Preço Por: deve ser maior que zero")
    private BigDecimal precoPor;

    @NotNull(message = "Classificação é obrigatória")
    private ClassificacaoProduto classificacao;
}
