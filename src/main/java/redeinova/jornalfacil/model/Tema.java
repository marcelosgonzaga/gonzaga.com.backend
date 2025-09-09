// Tema.java
package redeinova.jornalfacil.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tema")
public class Tema {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private String descricao;

    @Column(name = "caminho_imagem", nullable = false)
    private String caminhoImagem;
}