package gonzaga.jornalfacil.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import gonzaga.jornalfacil.model.*;
import gonzaga.jornalfacil.repository.RelatorioUsoRepository;
import gonzaga.jornalfacil.repository.RelatorioClienteRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelatorioService {

    private final RelatorioUsoRepository relatorioUsoRepository;
    private final RelatorioClienteRepository relatorioClienteRepository;

    public void registrarUso(Projeto projeto, String tipoGeracao, String codigoCliente) {
        try {
            RelatorioUso registro = new RelatorioUso();
            registro.setCodigoCliente(codigoCliente);
            registro.setDataUso(LocalDateTime.now());
            registro.setProjeto(projeto);
            registro.setTipoGeracao(tipoGeracao);
            registro.setQuantidadeProdutos(projeto.getProdutos().size());
            registro.setNomeTema(projeto.getTema().getDescricao());
            registro.setPeriodoValidade(projeto.getDataInicio() + " a " + projeto.getDataFim());

            relatorioUsoRepository.save(registro);

            // Atualizar estatísticas do cliente
            atualizarEstatisticasCliente(codigoCliente);

            log.info("Registro de uso salvo para cliente: {}", codigoCliente);
        } catch (Exception e) {
            log.error("Erro ao registrar uso para cliente {}: {}", codigoCliente, e.getMessage());
        }
    }

    private void atualizarEstatisticasCliente(String codigoCliente) {
        RelatorioCliente cliente = relatorioClienteRepository.findByCodigoCliente(codigoCliente)
                .orElseGet(() -> {
                    RelatorioCliente novo = new RelatorioCliente();
                    novo.setCodigoCliente(codigoCliente);
                    novo.setNomeCliente("Cliente " + codigoCliente);
                    novo.setDataRegistro(LocalDate.now());
                    return novo;
                });

        cliente.setTotalUso(cliente.getTotalUso() + 1);
        cliente.setUltimoUso(LocalDateTime.now());
        cliente.setAtivo(true);

        relatorioClienteRepository.save(cliente);
    }

    public Map<String, Object> gerarRelatorioDiario(String codigoCliente, LocalDate data) {
        LocalDateTime inicio = data.atStartOfDay();
        LocalDateTime fim = data.atTime(LocalTime.MAX);

        List<RelatorioUso> registros = relatorioUsoRepository
                .findByCodigoClienteAndDataUsoBetween(codigoCliente, inicio, fim);

        return construirRelatorio(registros, "Diário", data.toString(), data.toString());
    }

    public Map<String, Object> gerarRelatorioSemanal(String codigoCliente, LocalDate dataInicio) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataInicio.plusDays(6).atTime(LocalTime.MAX);

        List<RelatorioUso> registros = relatorioUsoRepository
                .findByCodigoClienteAndDataUsoBetween(codigoCliente, inicio, fim);

        return construirRelatorio(registros, "Semanal",
                dataInicio.toString(), dataInicio.plusDays(6).toString());
    }

    public Map<String, Object> gerarRelatorioMensal(String codigoCliente, int ano, int mes) {
        LocalDateTime inicio = LocalDate.of(ano, mes, 1).atStartOfDay();
        LocalDateTime fim = LocalDate.of(ano, mes, 1).plusMonths(1).minusDays(1).atTime(LocalTime.MAX);

        List<RelatorioUso> registros = relatorioUsoRepository
                .findByCodigoClienteAndDataUsoBetween(codigoCliente, inicio, fim);

        return construirRelatorio(registros, "Mensal",
                String.format("%d/%02d", ano, mes),
                String.format("%d/%02d", ano, mes));
    }

    public Map<String, Object> gerarRelatorioAnual(String codigoCliente, int ano) {
        LocalDateTime inicio = LocalDate.of(ano, 1, 1).atStartOfDay();
        LocalDateTime fim = LocalDate.of(ano, 12, 31).atTime(LocalTime.MAX);

        List<RelatorioUso> registros = relatorioUsoRepository
                .findByCodigoClienteAndDataUsoBetween(codigoCliente, inicio, fim);

        return construirRelatorio(registros, "Anual",
                String.valueOf(ano), String.valueOf(ano));
    }

    public Map<String, Object> gerarRelatorioGeral(LocalDate dataInicio, LocalDate dataFim) {
        LocalDateTime inicio = dataInicio.atStartOfDay();
        LocalDateTime fim = dataFim.atTime(LocalTime.MAX);

        List<RelatorioUso> registros = relatorioUsoRepository.findByPeriodo(inicio, fim);
        List<Object[]> usoPorCliente = relatorioUsoRepository
                .findUsoPorClienteNoPeriodo(inicio, fim);
        List<Object[]> usoPorTipo = relatorioUsoRepository
                .findUsoPorTipoGeracao(inicio, fim);

        Map<String, Object> relatorio = new HashMap<>();
        relatorio.put("periodo", "Geral");
        relatorio.put("dataInicio", dataInicio.toString());
        relatorio.put("dataFim", dataFim.toString());
        relatorio.put("totalUsos", registros.size());
        relatorio.put("clientesAtivos", usoPorCliente.size());
        relatorio.put("usoPorCliente", usoPorCliente);
        relatorio.put("usoPorTipo", usoPorTipo);
        relatorio.put("registros", registros);

        return relatorio;
    }

    private Map<String, Object> construirRelatorio(List<RelatorioUso> registros, String tipo,
                                                   String inicio, String fim) {
        Map<String, Object> relatorio = new HashMap<>();
        relatorio.put("periodo", tipo);
        relatorio.put("dataInicio", inicio);
        relatorio.put("dataFim", fim);
        relatorio.put("totalUsos", registros.size());
        relatorio.put("registros", registros);

        // Estatísticas por tipo de geração
        Map<String, Long> statsTipo = new HashMap<>();
        for (RelatorioUso registro : registros) {
            statsTipo.merge(registro.getTipoGeracao(), 1L, Long::sum);
        }
        relatorio.put("usoPorTipo", statsTipo);

        return relatorio;
    }

    public byte[] exportarRelatorioExcel(Map<String, Object> relatorio) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Relatório de Uso");

            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Cabeçalho
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Data/Hora", "Código Cliente", "Tema", "Produtos", "Tipo Geração", "Período Validade"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dados
            @SuppressWarnings("unchecked")
            List<RelatorioUso> registros = (List<RelatorioUso>) relatorio.get("registros");
            int rowNum = 1;

            for (RelatorioUso registro : registros) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(registro.getDataUso()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                row.createCell(1).setCellValue(registro.getCodigoCliente());
                row.createCell(2).setCellValue(registro.getNomeTema());
                row.createCell(3).setCellValue(registro.getQuantidadeProdutos());
                row.createCell(4).setCellValue(registro.getTipoGeracao());
                row.createCell(5).setCellValue(registro.getPeriodoValidade());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Erro ao exportar relatório Excel: {}", e.getMessage(), e);
            throw new IOException("Falha ao exportar relatório Excel", e);
        }
    }

    public List<RelatorioCliente> listarClientes() {
        return relatorioClienteRepository.findByAtivoTrue();
    }

    public Optional<RelatorioCliente> buscarClientePorCodigo(String codigoCliente) {
        return relatorioClienteRepository.findByCodigoCliente(codigoCliente);
    }
}