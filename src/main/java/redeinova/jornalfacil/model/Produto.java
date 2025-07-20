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

    private BigDecimal precoDe;

    @Column(name = "precoPor", nullable = false)
    private BigDecimal precoPor;

    @Enumerated(EnumType.STRING)
    private ClassificacaoProduto classificacao;

    private String caminhoImagem;


}
