package gonzaga.jornalfacil.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "relatorio_cliente")
public class RelatorioCliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_cliente", nullable = false, length = 4)
    private String codigoCliente;

    @Column(name = "nome_cliente")
    private String nomeCliente;

    @Column(name = "data_registro")
    private LocalDate dataRegistro;

    @Column(name = "total_uso")
    private Integer totalUso = 0;

    @Column(name = "ultimo_uso")
    private LocalDateTime ultimoUso;

    @Column(name = "ativo")
    private Boolean ativo = true;
}