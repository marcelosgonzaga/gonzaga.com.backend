package gonzaga.jornalfacil.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import gonzaga.jornalfacil.model.ClassificacaoProduto;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponse {
    private Long id;
    private Long ean;
    private String descricao;
    private BigDecimal precoDe;
    private BigDecimal precoPor;
    private ClassificacaoProduto classificacao;
    private String caminhoImagem;
    private String informacoesObrigatorias;
    private String textoLegal;

}
