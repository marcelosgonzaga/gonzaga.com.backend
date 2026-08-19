package gonzaga.jornalfacil.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "relatorio_uso")
public class RelatorioUso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cliente", nullable = false, length = 4)
    private String codigoCliente;

    @Column(name = "data_uso", nullable = false)
    private LocalDateTime dataUso;

    @ManyToOne
    @JoinColumn(name = "projeto_id")
    private Projeto projeto;

    @Column(name = "tipo_geracao") // PDF, JPG, PDF_SSR, JPG_SSR
    private String tipoGeracao;

    @Column(name = "quantidade_produtos")
    private Integer quantidadeProdutos;

    @Column(name = "nome_tema")
    private String nomeTema;

    @Column(name = "periodo_validade")
    private String periodoValidade;
}