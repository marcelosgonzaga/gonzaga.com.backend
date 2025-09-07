package redeinova.jornalfacil.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO para representar os detalhes de um projeto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjetoDetalhesDTO {
    private Long id;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private Long temaId;
    private String temaNome;
    private Integer quantidadeProdutos;

    // Campos adicionais que podem ser úteis
    private String codigoLoja;
    private String nomeRodape;

    /**
     * Método estático para criação do builder com valores padrão
     */
    public static ProjetoDetalhesDTOBuilder builder() {
        return new ProjetoDetalhesDTOBuilder();
    }
}