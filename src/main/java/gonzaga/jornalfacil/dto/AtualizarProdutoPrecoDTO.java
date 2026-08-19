package gonzaga.jornalfacil.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AtualizarProdutoPrecoDTO {
    private Long id;
    private BigDecimal precoDe;
    private BigDecimal precoPor;
    private Boolean isento;
    private String informacoesObrigatorias;
    private String textoLegal;
}