package redeinova.jornalfacil.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Tema {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String descricao;

    private String nome;


    @Column(name = "caminho_imagem", nullable = false)
    private String caminhoImagem;
}
