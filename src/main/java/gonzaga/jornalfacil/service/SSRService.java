package gonzaga.jornalfacil.service;

import gonzaga.jornalfacil.model.Projeto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SSRService {

    @Value("${ssr.server.url:http://localhost:3001}")
    private String ssrServerUrl;

    private final RestTemplate restTemplate;

    // ✅ REMOVER @PostConstruct - testar conexão apenas quando necessário
    // O serviço Node pode não estar pronto quando o Spring inicia

    public byte[] gerarPdfProjeto(Projeto projeto) {
        return gerarArquivoProjeto(projeto, "pdf");
    }

    public byte[] gerarJpgProjeto(Projeto projeto) {
        return gerarArquivoProjeto(projeto, "jpg");
    }

    private byte[] gerarArquivoProjeto(Projeto projeto, String tipo) {
        try {
            log.info("🔄 Enviando projeto para SSR Service - ID: {}, Tipo: {}", projeto.getId(), tipo.toUpperCase());

            // ✅ TESTAR CONEXÃO APENAS QUANDO FOR USAR
            testSSRConnection();

            // Converter Projeto para DTO compatível com o SSR
            Map<String, Object> projetoDTO = criarProjetoDTO(projeto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(projetoDTO, headers);

            String url = ssrServerUrl + "/generate-" + tipo.toLowerCase();

            log.info("📤 Enviando requisição para SSR: {}", url);
            log.debug("Dados enviados: {}", projetoDTO);

            // ✅ ADICIONAR TIMEOUT para evitar bloqueios
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    byte[].class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("✅ {} gerado via SSR - Tamanho: {} bytes",
                        tipo.toUpperCase(), response.getBody().length);
                return response.getBody();
            } else {
                throw new RuntimeException("Resposta inválida do SSR Service: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Erro ao gerar {} via SSR: {}", tipo.toUpperCase(), e.getMessage(), e);
            throw new RuntimeException("Falha na geração SSR do " + tipo.toUpperCase() + ": " + e.getMessage(), e);
        }
    }

    // ✅ MOVER teste de conexão para quando for realmente usado
    private void testSSRConnection() {
        try {
            log.info("🔧 Testando conexão com SSR Service: {}", ssrServerUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(ssrServerUrl + "/health", String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ SSR Service conectado com sucesso: {}", ssrServerUrl);
            } else {
                log.warn("⚠️ SSR Service retornou status: {}", response.getStatusCode());
                throw new RuntimeException("SSR Service não está respondendo corretamente");
            }
        } catch (Exception e) {
            log.error("❌ Não foi possível conectar ao SSR Service em: {}", ssrServerUrl);
            log.error("   Certifique-se de que o serviço Node está rodando na porta 3001");
            throw new RuntimeException("SSR Service indisponível: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> criarProjetoDTO(Projeto projeto) {
        Map<String, Object> dto = new HashMap<>();

        dto.put("id", projeto.getId());
        dto.put("nome", "Encarte Promocional");

        if (projeto.getDataInicio() != null) {
            dto.put("dataInicio", projeto.getDataInicio().toString());
        } else {
            dto.put("dataInicio", null);
        }

        if (projeto.getDataFim() != null) {
            dto.put("dataFim", projeto.getDataFim().toString());
        } else {
            dto.put("dataFim", null);
        }

        if (projeto.getTema() != null) {
            Map<String, Object> tema = new HashMap<>();
            tema.put("id", projeto.getTema().getId());
            tema.put("descricao", projeto.getTema().getDescricao());
            tema.put("caminhoImagem", projeto.getTema().getCaminhoImagem());
            dto.put("tema", tema);
        }

        if (projeto.getProdutos() != null && !projeto.getProdutos().isEmpty()) {
            dto.put("produtos", projeto.getProdutos().stream().map(produto -> {
                Map<String, Object> prodDTO = new HashMap<>();
                prodDTO.put("id", produto.getId());
                prodDTO.put("descricao", produto.getDescricao());
                prodDTO.put("caminhoImagem", produto.getCaminhoImagem());
                prodDTO.put("precoDe", produto.getPrecoDe());
                prodDTO.put("precoPor", produto.getPrecoPor());
                prodDTO.put("classificacao", produto.getClassificacao() != null ? produto.getClassificacao().name() : null);

                // ✅ CORREÇÃO CRÍTICA: Incluir campo isento no DTO do SSR
                prodDTO.put("isento", produto.isIsento());

                return prodDTO;
            }).toList());
        } else {
            dto.put("produtos", java.util.Collections.emptyList());
        }

        if (projeto.getRodape() != null) {
            Map<String, Object> rodape = new HashMap<>();
            rodape.put("id", projeto.getRodape().getId());
            rodape.put("codigoLoja", projeto.getRodape().getCodigoLoja());
            rodape.put("caminhoImagem", projeto.getRodape().getCaminhoImagem());
            dto.put("rodape", rodape);
        }

        log.debug("✅ DTO criado com sucesso para projeto ID: {}", projeto.getId());
        return dto;
    }
}