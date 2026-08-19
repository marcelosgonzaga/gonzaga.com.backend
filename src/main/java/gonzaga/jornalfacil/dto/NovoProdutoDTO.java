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
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;

    // REMOVA @DecimalMin para precoDe (pode ser zero para medicamentos isentos)
    private BigDecimal precoDe;

    @NotNull(message = "Preço Por é obrigatório")
    @DecimalMin(value = "0.0", inclusive = true, message = "Preço Por deve ser maior ou igual a zero") // Mude para inclusive=true
    private BigDecimal precoPor;

    @NotNull(message = "Classificação é obrigatória")
    private ClassificacaoProduto classificacao;

    // Informações Obrigatórias
    @Size(max = 500, message = "Informações obrigatórias deve ter no máximo 500 caracteres")
    private String informacoesObrigatorias;

    //  Texto Legal
    @Size(max = 500, message = "Texto legal deve ter no máximo 500 caracteres")
    private String textoLegal;
}
