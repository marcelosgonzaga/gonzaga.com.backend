package redeinova.jornalfacil.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TemaResponse {
    private Long id;
    private String nome;
    private String descricao;
    private String caminhoImagem;
}
