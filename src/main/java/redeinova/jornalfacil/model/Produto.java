package redeinova.jornalfacil.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Produto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ean", nullable = false, unique = true)
    private Long ean;

    @Column(name = "descricao", nullable = false)
    private String descricao;

    @Column(name = "preco_de", precision = 10, scale = 2)
    private BigDecimal precoDe;

    @Column(name = "preco_por", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoPor;

    @Enumerated(EnumType.STRING)
    private ClassificacaoProduto classificacao;

    private String caminhoImagem;
}