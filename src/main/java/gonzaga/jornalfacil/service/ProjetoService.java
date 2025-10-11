package gonzaga.jornalfacil.service;

import com.lowagie.text.DocumentException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import gonzaga.jornalfacil.dto.AtualizarProjetoDTO;
import gonzaga.jornalfacil.dto.NovoProjetoDTO;
import gonzaga.jornalfacil.dto.ProjetoDetalhesDTO;
import gonzaga.jornalfacil.exception.InvalidDataException;
import gonzaga.jornalfacil.exception.PdfGenerationException;
import gonzaga.jornalfacil.model.*;
import gonzaga.jornalfacil.repository.ProdutoRepository;
import gonzaga.jornalfacil.repository.ProjetoRepository;
import gonzaga.jornalfacil.repository.RodapeRepository;
import gonzaga.jornalfacil.repository.TemaRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjetoService {
    private static final Logger logger = LoggerFactory.getLogger(ProjetoService.class);
    private static final String PROJETO_NAO_ENCONTRADO = "Projeto não encontrado com ID: %d";
    private static final String ERRO_VALIDACAO = "Erro de validação: %s";
    private static final String ERRO_ARQUIVO = "Erro ao acessar arquivo: %s";

    // Injeção de dependências
    private final PdfService pdfService;
    private final ImageService imageService;
    private final ProjetoRepository projetoRepository;
    private final ProdutoRepository produtoRepository;
    private final TemaRepository temaRepository;
    private final RodapeRepository rodapeRepository;

    // Caminhos configuráveis
    @Value("${file.temas-dir}")
    private String temasDir;

    @Value("${file.produtos-dir}")
    private String produtosDir;

    @Value("${file.rodapes-dir}")
    private String rodapesDir;

    @Transactional
    public Projeto criarProjeto(NovoProjetoDTO dto) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            logger.info("Iniciando criação de novo projeto. Tema ID: {}, Produtos: {}",
                    dto.getTemaId(), dto.getProdutoIds().size());

            // Validação das datas
            validarDatas(dto.getDataInicio(), dto.getDataFim());

            // Obter e validar entidades relacionadas
            Tema tema = obterTemaValidado(dto.getTemaId());
            Rodape rodape = obterRodapeValidado(dto.getCodigoLoja());

            List<Produto> produtos = obterProdutosValidados(dto.getProdutoIds());

            // DEBUG: Log dos preços dos produtos
            for (Produto produto : produtos) {
                logger.debug("Produto ID {} - PreçoDe: {}, PreçoPor: {}, Classificacao: {}",
                        produto.getId(), produto.getPrecoDe(), produto.getPrecoPor(), produto.getClassificacao());
            }

            // Criar e salvar o projeto
            Projeto projeto = buildProjeto(dto, tema, rodape, produtos);
            Projeto projetoSalvo = projetoRepository.save(projeto);

            logger.info("Projeto criado com sucesso. ID: {}", projetoSalvo.getId());
            return projetoSalvo;

        } catch (EntityNotFoundException e) {
            logger.error("Erro ao criar projeto - Recurso não encontrado: {}", e.getMessage());
            throw e;
        } catch (InvalidDataException e) {
            logger.error("Erro de validação ao criar projeto: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao criar projeto: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao criar projeto", e);
        } finally {
            MDC.remove("requestId");
        }
    }

    @Cacheable(value = "projetos", key = "#id")
    @Transactional(readOnly = true)
    public Projeto buscarPorId(Long id) {
        MDC.put("requestId", UUID.randomUUID().toString());

        try {
            logger.debug("Buscando projeto por ID: {}", id);
            return projetoRepository.findById(id)
                    .orElseThrow(() -> {
                        String errorMsg = String.format(PROJETO_NAO_ENCONTRADO, id);
                        logger.error(errorMsg);
                        return new EntityNotFoundException(errorMsg);
                    });
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public Page<Projeto> listarTodos(Pageable pageable) {
        MDC.put("requestId", UUID.randomUUID().toString());

        try {
            logger.debug("Listando projetos - página: {}, tamanho: {}",
                    pageable.getPageNumber(), pageable.getPageSize());
            return projetoRepository.findAll(pageable);
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public List<Projeto> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        MDC.put("requestId", UUID.randomUUID().toString());

        try {
            logger.debug("Buscando projetos entre {} e {}", inicio, fim);
            validarDatasConsulta(inicio, fim);
            return projetoRepository.findByDataInicioBetween(inicio, fim);
        } catch (InvalidDataException e) {
            logger.error("Erro ao buscar por período: {}", e.getMessage());
            throw e;
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public ProjetoDetalhesDTO buscarDetalhesPorId(Long id) {
        MDC.put("requestId", UUID.randomUUID().toString());

        try {
            logger.debug("Buscando detalhes do projeto ID: {}", id);
            Projeto projeto = buscarPorId(id);
            return toDetalhesDTO(projeto);
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdfProjeto(Long projetoId) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            logger.info("Iniciando geração de PDF para projeto ID: {}", projetoId);
            Projeto projeto = buscarPorId(projetoId);

            // Log detalhado dos dados do projeto
            logger.debug("Projeto encontrado - ID: {}, Tema: {}, Produtos: {}",
                    projeto.getId(),
                    projeto.getTema() != null ? projeto.getTema().getId() : "null",
                    projeto.getProdutos() != null ? projeto.getProdutos().size() : 0);

            if (projeto.getProdutos() != null) {
                for (Produto produto : projeto.getProdutos()) {
                    logger.debug("Produto {} - PreçoDe: {}, PreçoPor: {}",
                            produto.getId(),
                            produto.getPrecoDe(),
                            produto.getPrecoPor());
                }
            }

            // Validação básica antes de tentar gerar
            if (projeto == null) {
                throw new EntityNotFoundException("Projeto não encontrado");
            }

            if (projeto.getTema() == null || projeto.getTema().getCaminhoImagem() == null) {
                throw new InvalidDataException("Tema não configurado para o projeto");
            }

            if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
                throw new InvalidDataException("Nenhum produto associado ao projeto");
            }

            logger.debug("Gerando PDF para projeto {} com {} produtos",
                    projeto.getId(), projeto.getProdutos().size());

            byte[] pdf = pdfService.gerarEncarte(projeto);
            logger.info("PDF gerado com sucesso para projeto {}", projeto.getId());
            return pdf;

        } catch (EntityNotFoundException e) {
            logger.error("Projeto não encontrado: {}", e.getMessage());
            throw e;
        } catch (InvalidDataException e) {
            logger.error("Dados inválidos para geração de PDF: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao gerar PDF: {}", e.getMessage(), e);
            throw new PdfGenerationException("Erro ao gerar PDF: " + e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarImagemProjeto(Long projetoId) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            logger.info("Iniciando geração de imagem JPG para projeto ID: {}", projetoId);
            Projeto projeto = buscarPorId(projetoId);

            // Log detalhado dos dados do projeto
            logger.debug("Projeto encontrado - ID: {}, Tema: {}, Produtos: {}",
                    projeto.getId(),
                    projeto.getTema() != null ? projeto.getTema().getId() : "null",
                    projeto.getProdutos() != null ? projeto.getProdutos().size() : 0);

            // Validação básica antes de tentar gerar
            if (projeto == null) {
                throw new EntityNotFoundException("Projeto não encontrado");
            }

            if (projeto.getTema() == null || projeto.getTema().getCaminhoImagem() == null) {
                throw new InvalidDataException("Tema não configurado para o projeto");
            }

            if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
                throw new InvalidDataException("Nenhum produto associado ao projeto");
            }

            logger.debug("Gerando imagem JPG para projeto {} com {} produtos",
                    projeto.getId(), projeto.getProdutos().size());

            byte[] jpg = imageService.gerarEncarteImagem(projeto);
            logger.info("Imagem JPG gerada com sucesso para projeto {}", projeto.getId());
            return jpg;

        } catch (EntityNotFoundException e) {
            logger.error("Projeto não encontrado: {}", e.getMessage());
            throw e;
        } catch (InvalidDataException e) {
            logger.error("Dados inválidos para geração de imagem: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Erro inesperado ao gerar imagem JPG: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao gerar imagem JPG: " + e.getMessage(), e);
        } finally {
            MDC.remove("requestId");
        }
    }

    // Métodos auxiliares privados
    private Tema obterTemaValidado(Long temaId) {
        try {
            Tema tema = temaRepository.findById(temaId)
                    .orElseThrow(() -> new EntityNotFoundException("Tema não encontrado com ID: " + temaId));

            validarImagemTema(tema);
            return tema;
        } catch (EntityNotFoundException e) {
            logger.error("Tema não encontrado com ID: {} - {}", temaId, e.getMessage());
            throw e;
        } catch (InvalidDataException e) {
            logger.error("Tema inválido: {} - {}", temaId, e.getMessage());
            throw e;
        }
    }

    private Rodape obterRodapeValidado(Long codigoLoja) {
        try {
            Rodape rodape = rodapeRepository.findByCodigoLoja(codigoLoja)
                    .orElseThrow(() -> new EntityNotFoundException("Rodapé não encontrado para código: " + codigoLoja));

            validarImagemRodape(rodape);
            return rodape;
        } catch (EntityNotFoundException e) {
            logger.error("Rodapé não encontrado para código: {} - {}", codigoLoja, e.getMessage());
            throw e;
        } catch (InvalidDataException e) {
            logger.error("Rodapé inválido: {} - {}", codigoLoja, e.getMessage());
            throw e;
        }
    }

    private List<Produto> obterProdutosValidados(List<Long> produtosIds) {
        try {
            if (produtosIds == null || produtosIds.isEmpty()) {
                throw new InvalidDataException("Lista de produtos não pode ser vazia");
            }

            List<Produto> produtos = produtoRepository.findAllById(produtosIds);
            logger.debug("Encontrados {} produtos de {} solicitados", produtos.size(), produtosIds.size());

            if (produtos.size() != produtosIds.size()) {
                List<Long> idsEncontrados = produtos.stream().map(Produto::getId).collect(Collectors.toList());
                List<Long> idsNaoEncontrados = produtosIds.stream()
                        .filter(id -> !idsEncontrados.contains(id))
                        .collect(Collectors.toList());

                String errorMsg = "Produtos não encontrados: " + idsNaoEncontrados;
                logger.error(errorMsg);
                throw new EntityNotFoundException(errorMsg);
            }

            // Validação mais flexível para preços
            for (Produto produto : produtos) {
                if (produto.getPrecoPor() == null) {
                    String errorMsg = "Produto ID " + produto.getId() + " não possui preço por definido";
                    logger.error(errorMsg);
                    throw new InvalidDataException(errorMsg);
                }

                if (produto.getPrecoPor().compareTo(BigDecimal.ZERO) < 0) {
                    String errorMsg = "Produto ID " + produto.getId() + " possui preço por negativo";
                    logger.error(errorMsg);
                    throw new InvalidDataException(errorMsg);
                }

                // Para medicamentos, permitir precoDe nulo (para isentos)
                if (produto.getClassificacao() == ClassificacaoProduto.MEDICAMENTO &&
                        produto.getPrecoDe() != null &&
                        produto.getPrecoDe().compareTo(BigDecimal.ZERO) < 0) {
                    String errorMsg = "Produto ID " + produto.getId() + " (medicamento) possui preço de negativo";
                    logger.error(errorMsg);
                    throw new InvalidDataException(errorMsg);
                }
            }

            return produtos;
        } catch (InvalidDataException | EntityNotFoundException e) {
            logger.error("Erro ao obter produtos: {}", e.getMessage());
            throw e;
        }
    }

    private Projeto buildProjeto(NovoProjetoDTO dto, Tema tema, Rodape rodape, List<Produto> produtos) {
        Projeto projeto = new Projeto();
        projeto.setDataInicio(dto.getDataInicio());
        projeto.setDataFim(dto.getDataFim());
        projeto.setTema(tema);
        projeto.setRodape(rodape);
        projeto.setProdutos(produtos);
        return projeto;
    }

    private void validarProjetoParaGeracao(Projeto projeto) {
        if (projeto.getTema() == null) {
            String errorMsg = "Tema não definido para o projeto";
            logger.error(errorMsg);
            throw new PdfGenerationException(errorMsg, null);
        }

        if (projeto.getTema().getCaminhoImagem() == null || projeto.getTema().getCaminhoImagem().isEmpty()) {
            String errorMsg = "Imagem do tema não configurada";
            logger.error(errorMsg);
            throw new PdfGenerationException(errorMsg, null);
        }

        if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
            String errorMsg = "Nenhum produto associado ao projeto";
            logger.error(errorMsg);
            throw new PdfGenerationException(errorMsg, null);
        }
    }

    private void verificarExistenciaImagens(Projeto projeto) throws IOException {
        try {
            // Verificar imagem do tema
            String temaFileName = extractFileName(projeto.getTema().getCaminhoImagem());
            Path temaPath = Paths.get(temasDir, temaFileName);

            if (!Files.exists(temaPath)) {
                String errorMsg = "Imagem do tema não encontrada: " + temaPath;
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Verificar imagens dos produtos
            for (Produto produto : projeto.getProdutos()) {
                String produtoFileName = extractFileName(produto.getCaminhoImagem());
                Path produtoPath = Paths.get(produtosDir, produtoFileName);
                if (!Files.exists(produtoPath)) {
                    String errorMsg = "Imagem do produto não encontrada: " + produtoPath;
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }
            }

            // Verificar imagem do rodapé
            if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
                String rodapeFileName = extractFileName(projeto.getRodape().getCaminhoImagem());
                Path rodapePath = Paths.get(rodapesDir, rodapeFileName);
                if (!Files.exists(rodapePath)) {
                    String errorMsg = "Imagem do rodapé não encontrada: " + rodapePath;
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }
            }
        } catch (IOException e) {
            logger.error("Erro ao verificar imagens: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void validarDatas(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            String errorMsg = "Datas não podem ser nulas";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }

        if (inicio.isBefore(LocalDate.now())) {
            String errorMsg = "Data inicial não pode ser no passado";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }

        if (fim.isBefore(inicio)) {
            String errorMsg = "Data final deve ser posterior à data inicial";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }
    }

    private void validarDatasConsulta(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            String errorMsg = "Datas de consulta não podem ser nulas";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }

        if (fim.isBefore(inicio)) {
            String errorMsg = "Data final deve ser posterior à data inicial na consulta";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }
    }

    private void validarImagemTema(Tema tema) {
        if (tema.getCaminhoImagem() == null || tema.getCaminhoImagem().isEmpty()) {
            String errorMsg = "Tema ID " + tema.getId() + " não possui imagem configurada";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }
    }

    private void validarImagemRodape(Rodape rodape) {
        if (rodape.getCaminhoImagem() == null || rodape.getCaminhoImagem().isEmpty()) {
            String errorMsg = "Rodapé ID " + rodape.getId() + " não possui imagem configurada";
            logger.error(errorMsg);
            throw new InvalidDataException(errorMsg);
        }
    }

    private void validarImagensProdutos(List<Produto> produtos) {
        for (Produto produto : produtos) {
            if (produto.getCaminhoImagem() == null || produto.getCaminhoImagem().isEmpty()) {
                String errorMsg = "Produto ID " + produto.getId() + " não possui imagem configurada";
                logger.error(errorMsg);
                throw new InvalidDataException(errorMsg);
            }
        }
    }

    private ProjetoDetalhesDTO toDetalhesDTO(Projeto projeto) {
        ProjetoDetalhesDTO dto = new ProjetoDetalhesDTO();
        dto.setId(projeto.getId());
        dto.setDataInicio(projeto.getDataInicio());
        dto.setDataFim(projeto.getDataFim());

        if(projeto.getTema() != null) {
            dto.setTemaId(projeto.getTema().getId());
            dto.setTemaNome(projeto.getTema().getNome());
        }

        if(projeto.getProdutos() != null) {
            dto.setQuantidadeProdutos(projeto.getProdutos().size());
        }

        return dto;
    }

    @Transactional
    public Projeto atualizarProjeto(Long id, AtualizarProjetoDTO dto) {
        Projeto projeto = buscarPorId(id);

        // Atualizar apenas se fornecido no DTO
        if (dto.getDataInicio() != null) {
            projeto.setDataInicio(dto.getDataInicio());
        }

        if (dto.getDataFim() != null) {
            projeto.setDataFim(dto.getDataFim());
        }

        // Validar datas apenas se ambas foram fornecidas
        if (dto.getDataInicio() != null && dto.getDataFim() != null) {
            validarDatas(dto.getDataInicio(), dto.getDataFim());
        } else if (dto.getDataInicio() != null && projeto.getDataFim() != null) {
            // Validar data início com data fim existente
            validarDatas(dto.getDataInicio(), projeto.getDataFim());
        } else if (dto.getDataFim() != null && projeto.getDataInicio() != null) {
            // Validar data fim com data início existente
            validarDatas(projeto.getDataInicio(), dto.getDataFim());
        }

        if (dto.getRodapeId() != null) {
            Rodape rodape = rodapeRepository.findById(dto.getRodapeId())
                    .orElseThrow(() -> new EntityNotFoundException("Rodapé não encontrado"));
            projeto.setRodape(rodape);
        }

        return projetoRepository.save(projeto);
    }

    // Método auxiliar para extrair apenas o nome do arquivo
    private String extractFileName(String filePath) {
        if (filePath == null) return "";
        if (filePath.contains("/")) {
            return filePath.substring(filePath.lastIndexOf("/") + 1);
        }
        if (filePath.contains("\\")) {
            return filePath.substring(filePath.lastIndexOf("\\") + 1);
        }
        return filePath;
    }
}

