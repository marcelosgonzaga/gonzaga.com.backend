package gonzaga.jornalfacil.dto;

import lombok.Builder;
import lombok.Data;
import gonzaga.jornalfacil.model.ClassificacaoProduto;

import java.math.BigDecimal;

@Data
@Builder
public class ProdutoResponse {
    private Long id;
    private Long ean;
    private String descricao;
    private BigDecimal precoDe;
    private BigDecimal precoPor;
    private ClassificacaoProduto classificacao;
    private String caminhoImagem;

}
