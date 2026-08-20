package gonzaga.jornalfacil.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Rodape {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_loja", nullable = false, unique = true, length = 4)
    private Long codigoLoja;

    @Column(name = "caminho_imagem", nullable = false)
    private String caminhoImagem;
}
