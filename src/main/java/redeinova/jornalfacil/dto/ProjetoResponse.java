package redeinova.jornalfacil.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ProjetoResponse {
    private Long id;
    private Long temaId;
    private String temaNome;
    private Long codigoLoja;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private List<Long> produtoIds;
    private String pdfPath; // Caminho para o PDF gerado (opcional)
}