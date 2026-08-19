package gonzaga.jornalfacil.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import gonzaga.jornalfacil.service.RelatorioService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@RestController
@RequestMapping("/api/relatorios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RelatorioController {

    private final RelatorioService relatorioService;

    @GetMapping("/diario/{codigoCliente}")
    public ResponseEntity<Map<String, Object>> relatorioDiario(
            @PathVariable String codigoCliente,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {

        Map<String, Object> relatorio = relatorioService.gerarRelatorioDiario(codigoCliente, data);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/semanal/{codigoCliente}")
    public ResponseEntity<Map<String, Object>> relatorioSemanal(
            @PathVariable String codigoCliente,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio) {

        Map<String, Object> relatorio = relatorioService.gerarRelatorioSemanal(codigoCliente, dataInicio);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/mensal/{codigoCliente}")
    public ResponseEntity<Map<String, Object>> relatorioMensal(
            @PathVariable String codigoCliente,
            @RequestParam int ano,
            @RequestParam int mes) {

        Map<String, Object> relatorio = relatorioService.gerarRelatorioMensal(codigoCliente, ano, mes);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/anual/{codigoCliente}")
    public ResponseEntity<Map<String, Object>> relatorioAnual(
            @PathVariable String codigoCliente,
            @RequestParam int ano) {

        Map<String, Object> relatorio = relatorioService.gerarRelatorioAnual(codigoCliente, ano);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/geral")
    public ResponseEntity<Map<String, Object>> relatorioGeral(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        Map<String, Object> relatorio = relatorioService.gerarRelatorioGeral(dataInicio, dataFim);
        return ResponseEntity.ok(relatorio);
    }

    @GetMapping("/exportar-excel")
    public ResponseEntity<byte[]> exportarRelatorioExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        try {
            Map<String, Object> relatorio = relatorioService.gerarRelatorioGeral(dataInicio, dataFim);
            byte[] excelBytes = relatorioService.exportarRelatorioExcel(relatorio);

            String fileName = String.format("relatorio_uso_%s_a_%s.xlsx",
                    dataInicio, dataFim);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(excelBytes);

        } catch (IOException e) {
            log.error("Erro ao exportar relatório Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/clientes")
    public ResponseEntity<List<Map<String, Object>>> listarClientes() {
        var clientes = relatorioService.listarClientes();

        // ✅ CORREÇÃO: Usar HashMap explicitamente para evitar problemas de inferência de tipos
        List<Map<String, Object>> clientesDTO = clientes.stream()
                .map(cliente -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("codigoCliente", cliente.getCodigoCliente());
                    map.put("nomeCliente", cliente.getNomeCliente());
                    map.put("totalUso", cliente.getTotalUso());
                    map.put("ultimoUso", cliente.getUltimoUso());
                    map.put("ativo", cliente.getAtivo());
                    return map;
                })
                .toList();

        return ResponseEntity.ok(clientesDTO);
    }
}