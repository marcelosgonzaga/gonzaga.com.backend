package redeinova.jornalfacil.service;

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
import redeinova.jornalfacil.dto.NovoProjetoDTO;
import redeinova.jornalfacil.dto.ProjetoDetalhesDTO;
import redeinova.jornalfacil.exception.InvalidDataException;
import redeinova.jornalfacil.exception.PdfGenerationException;
import redeinova.jornalfacil.model.Produto;
import redeinova.jornalfacil.model.Projeto;
import redeinova.jornalfacil.model.Rodape;
import redeinova.jornalfacil.model.Tema;
import redeinova.jornalfacil.repository.ProdutoRepository;
import redeinova.jornalfacil.repository.ProjetoRepository;
import redeinova.jornalfacil.repository.RodapeRepository;
import redeinova.jornalfacil.repository.TemaRepository;

import java.io.IOException;
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

            validarProjetoParaGeracao(projeto);
            verificarExistenciaImagens(projeto);

            logger.debug("Gerando PDF para projeto {} com {} produtos",
                    projeto.getId(), projeto.getProdutos().size());

            byte[] pdf = pdfService.gerarEncarte(projeto);
            logger.info("PDF gerado com sucesso para projeto {}", projeto.getId());
            return pdf;

        } catch (EntityNotFoundException e) {
            String errorMsg = String.format(PROJETO_NAO_ENCONTRADO, projetoId);
            logger.error("{} - {}", errorMsg, e.getMessage());
            throw new PdfGenerationException(errorMsg, e);
        } catch (IOException e) {
            String errorMsg = String.format(ERRO_ARQUIVO, e.getMessage());
            logger.error("Erro de IO ao gerar PDF: {}", errorMsg, e);
            throw new PdfGenerationException(errorMsg, e);
        } catch (DocumentException e) {
            String errorMsg = "Erro ao gerar documento PDF: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new PdfGenerationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Erro inesperado ao gerar PDF: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new PdfGenerationException(errorMsg, e);
        } finally {
            MDC.remove("requestId");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarImagemProjeto(Long projetoId) {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);

        try {
            logger.info("Iniciando geração de imagem para projeto ID: {}", projetoId);
            Projeto projeto = buscarPorId(projetoId);

            validarProjetoParaGeracao(projeto);
            verificarExistenciaImagens(projeto);

            logger.debug("Gerando imagem para projeto {} com {} produtos",
                    projeto.getId(), projeto.getProdutos().size());

            byte[] imagem = imageService.gerarEncarteImagem(projeto);
            logger.info("Imagem gerada com sucesso para projeto {}", projeto.getId());
            return imagem;

        } catch (EntityNotFoundException e) {
            String errorMsg = String.format(PROJETO_NAO_ENCONTRADO, projetoId);
            logger.error("{} - {}", errorMsg, e.getMessage());
            throw new PdfGenerationException(errorMsg, e);
        } catch (IOException e) {
            String errorMsg = String.format(ERRO_ARQUIVO, e.getMessage());
            logger.error("Erro de IO ao gerar imagem: {}", errorMsg, e);
            throw new PdfGenerationException(errorMsg, e);
        } catch (Exception e) {
            String errorMsg = "Erro inesperado ao gerar imagem: " + e.getMessage();
            logger.error(errorMsg, e);
            throw new PdfGenerationException(errorMsg, e);
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

            validarImagensProdutos(produtos);
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
            Path temaPath = Paths.get(temasDir, projeto.getTema().getCaminhoImagem());
            if (!Files.exists(temaPath)) {
                String errorMsg = "Imagem do tema não encontrada: " + temaPath;
                logger.error(errorMsg);
                throw new IOException(errorMsg);
            }

            // Verificar imagens dos produtos
            for (Produto produto : projeto.getProdutos()) {
                Path produtoPath = Paths.get(produtosDir, produto.getCaminhoImagem());
                if (!Files.exists(produtoPath)) {
                    String errorMsg = "Imagem do produto não encontrada: " + produtoPath;
                    logger.error(errorMsg);
                    throw new IOException(errorMsg);
                }
            }

            // Verificar imagem do rodapé
            if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
                Path rodapePath = Paths.get(rodapesDir, projeto.getRodape().getCaminhoImagem());
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
}





//package redeinova.jornalfacil.service;
//
//import com.lowagie.text.DocumentException;
//import jakarta.persistence.EntityNotFoundException;
//import lombok.RequiredArgsConstructor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//import redeinova.jornalfacil.dto.NovoProjetoDTO;
//import redeinova.jornalfacil.exception.InvalidDataException;
//import redeinova.jornalfacil.exception.PdfGenerationException;
//import redeinova.jornalfacil.model.Produto;
//import redeinova.jornalfacil.model.Projeto;
//import redeinova.jornalfacil.model.Rodape;
//import redeinova.jornalfacil.model.Tema;
//import redeinova.jornalfacil.repository.ProdutoRepository;
//import redeinova.jornalfacil.repository.ProjetoRepository;
//import redeinova.jornalfacil.repository.RodapeRepository;
//import redeinova.jornalfacil.repository.TemaRepository;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.time.LocalDate;
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class ProjetoService {
//    private static final Logger logger = LoggerFactory.getLogger(ProjetoService.class);
//
//    // Injeção de dependências
//    private final PdfService pdfService;
//    private final ImageService imageService;
//    private final ProjetoRepository projetoRepository;
//    private final ProdutoRepository produtoRepository;
//    private final TemaRepository temaRepository;
//    private final RodapeRepository rodapeRepository;
//
//    // Caminhos configuráveis
//    @Value("${file.temas-dir}")
//    private String temasDir;
//
//    @Value("${file.produtos-dir}")
//    private String produtosDir;
//
//    @Value("${file.rodapes-dir}")
//    private String rodapesDir;
//
//    /**
//     * Cria um novo projeto com os dados fornecidos
//     * @param dto DTO com os dados do novo projeto
//     * @return Projeto criado
//     */
//    @Transactional
//    public Projeto criarProjeto(NovoProjetoDTO dto) {
//        logger.info("Criando novo projeto com tema {} e {} produtos",
//                dto.getTemaId(), dto.getProdutoIds().size());
//
//        // Validação das datas
//        validarDatas(dto.getDataInicio(), dto.getDataFim());
//
//        // Obter e validar tema
//        Tema tema = temaRepository.findById(dto.getTemaId())
//                .orElseThrow(() -> {
//                    logger.error("Tema não encontrado com ID: {}", dto.getTemaId());
//                    return new EntityNotFoundException("Tema não encontrado");
//                });
//        validarImagemTema(tema);
//
//        // Obter e validar rodapé
//        Rodape rodape = rodapeRepository.findByCodigoLoja(dto.getCodigoLoja())
//                .orElseThrow(() -> {
//                    logger.error("Rodapé não encontrado para código: {}", dto.getCodigoLoja());
//                    return new EntityNotFoundException("Rodapé não encontrado para o código: " + dto.getCodigoLoja());
//                });
//        validarImagemRodape(rodape);
//
//        // Obter e validar produtos
//        List<Produto> produtos = validarEObterProdutos(dto.getProdutoIds());
//        validarImagensProdutos(produtos);
//
//        // Criar e salvar o projeto
//        Projeto projeto = new Projeto();
//        projeto.setDataInicio(dto.getDataInicio());
//        projeto.setDataFim(dto.getDataFim());
//        projeto.setTema(tema);
//        projeto.setRodape(rodape);
//        projeto.setProdutos(produtos);
//
//        Projeto projetoSalvo = projetoRepository.save(projeto);
//        logger.info("Projeto criado com ID: {}", projetoSalvo.getId());
//        return projetoSalvo;
//    }
//
//    /**
//     * Busca um projeto pelo ID
//     * @param id ID do projeto
//     * @return Projeto encontrado
//     */
//    @Transactional(readOnly = true)
//    public Projeto buscarPorId(Long id) {
//        logger.debug("Buscando projeto por ID: {}", id);
//        return projetoRepository.findById(id)
//                .orElseThrow(() -> {
//                    logger.error("Projeto não encontrado com ID: {}", id);
//                    return new EntityNotFoundException("Projeto não encontrado com ID: " + id);
//                });
//    }
//
//    /**
//     * Lista todos os projetos
//     * @return Lista de projetos
//     */
//    @Transactional(readOnly = true)
//    public List<Projeto> listarTodos() {
//        logger.debug("Listando todos os projetos");
//        return projetoRepository.findAll();
//    }
//
//    /**
//     * Busca projetos por período
//     * @param inicio Data inicial
//     * @param fim Data final
//     * @return Lista de projetos no período
//     */
//    @Transactional(readOnly = true)
//    public List<Projeto> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
//        logger.debug("Buscando projetos entre {} e {}", inicio, fim);
//        validarDatasConsulta(inicio, fim);
//        return projetoRepository.findByDataInicioBetween(inicio, fim);
//    }
//
//    /**
//     * Gera o PDF do projeto
//     * @param projetoId ID do projeto
//     * @return Byte array com o PDF
//     */
//    @Transactional(readOnly = true)
//    public byte[] gerarPdfProjeto(Long projetoId) {
//        logger.info("Iniciando geração de PDF para projeto ID: {}", projetoId);
//        try {
//            Projeto projeto = buscarPorId(projetoId);
//            logger.debug("Projeto encontrado: {}", projeto.getId());
//
//            validarProjetoParaGeracao(projeto);
//            verificarExistenciaImagens(projeto);
//
//            logger.info("Gerando PDF para projeto {} com {} produtos",
//                    projeto.getId(), projeto.getProdutos().size());
//
//            byte[] pdf = pdfService.gerarEncarte(projeto);
//            logger.info("PDF gerado com sucesso para projeto {}", projeto.getId());
//            return pdf;
//        } catch (EntityNotFoundException e) {
//            logger.error("Projeto não encontrado: {}", e.getMessage());
//            throw new PdfGenerationException("Projeto não encontrado: " + e.getMessage(), e);
//        } catch (IOException | DocumentException e) {
//            logger.error("Erro técnico ao gerar PDF: {}", e.getMessage());
//            throw new PdfGenerationException("Erro técnico ao gerar PDF: " + e.getMessage(), e);
//        } catch (Exception e) {
//            logger.error("Erro inesperado ao gerar PDF: {}", e.getMessage());
//            throw new PdfGenerationException("Erro inesperado ao gerar PDF: " + e.getMessage(), e);
//        }
//    }
//
//    /**
//     * Gera a imagem JPG do projeto
//     * @param projetoId ID do projeto
//     * @return Byte array com a imagem JPG
//     */
//    @Transactional(readOnly = true)
//    public byte[] gerarImagemProjeto(Long projetoId) {
//        logger.info("Iniciando geração de imagem para projeto ID: {}", projetoId);
//        try {
//            Projeto projeto = buscarPorId(projetoId);
//            logger.debug("Projeto encontrado: {}", projeto.getId());
//
//            validarProjetoParaGeracao(projeto);
//            verificarExistenciaImagens(projeto);
//
//            logger.info("Gerando imagem para projeto {} com {} produtos",
//                    projeto.getId(), projeto.getProdutos().size());
//
//            byte[] imagem = imageService.gerarEncarteImagem(projeto);
//            logger.info("Imagem gerada com sucesso para projeto {}", projeto.getId());
//            return imagem;
//        } catch (EntityNotFoundException e) {
//            logger.error("Projeto não encontrado: {}", e.getMessage());
//            throw new PdfGenerationException("Projeto não encontrado: " + e.getMessage(), e);
//        } catch (IOException e) {
//            logger.error("Erro técnico ao gerar imagem: {}", e.getMessage());
//            throw new PdfGenerationException("Erro técnico ao gerar imagem: " + e.getMessage(), e);
//        } catch (Exception e) {
//            logger.error("Erro inesperado ao gerar imagem: {}", e.getMessage());
//            throw new PdfGenerationException("Erro inesperado ao gerar imagem: " + e.getMessage(), e);
//        }
//    }
//
//    // Métodos auxiliares privados
//
//    private List<Produto> validarEObterProdutos(List<Long> produtosIds) {
//        if (produtosIds == null || produtosIds.isEmpty()) {
//            throw new InvalidDataException("Lista de produtos não pode ser vazia");
//        }
//
//        List<Produto> produtos = produtoRepository.findAllById(produtosIds);
//        logger.debug("Encontrados {} produtos de {} solicitados", produtos.size(), produtosIds.size());
//
//        if (produtos.size() != produtosIds.size()) {
//            throw new EntityNotFoundException("Um ou mais produtos não foram encontrados");
//        }
//
//        return produtos;
//    }
//
//    private void validarProjetoParaGeracao(Projeto projeto) {
//        if (projeto.getTema() == null) {
//            throw new PdfGenerationException("Tema não definido para o projeto", null);
//        }
//
//        if (projeto.getTema().getCaminhoImagem() == null || projeto.getTema().getCaminhoImagem().isEmpty()) {
//            throw new PdfGenerationException("Imagem do tema não configurada", null);
//        }
//
//        if (projeto.getProdutos() == null || projeto.getProdutos().isEmpty()) {
//            throw new PdfGenerationException("Nenhum produto associado ao projeto", null);
//        }
//    }
//
//    private void verificarExistenciaImagens(Projeto projeto) throws IOException {
//        // Verificar imagem do tema
//        Path temaPath = Paths.get(temasDir, projeto.getTema().getCaminhoImagem());
//        if (!Files.exists(temaPath)) {
//            throw new IOException("Imagem do tema não encontrada: " + temaPath);
//        }
//
//        // Verificar imagens dos produtos
//        for (Produto produto : projeto.getProdutos()) {
//            Path produtoPath = Paths.get(produtosDir, produto.getCaminhoImagem());
//            if (!Files.exists(produtoPath)) {
//                throw new IOException("Imagem do produto não encontrada: " + produtoPath);
//            }
//        }
//
//        // Verificar imagem do rodapé
//        if (projeto.getRodape() != null && projeto.getRodape().getCaminhoImagem() != null) {
//            Path rodapePath = Paths.get(rodapesDir, projeto.getRodape().getCaminhoImagem());
//            if (!Files.exists(rodapePath)) {
//                throw new IOException("Imagem do rodapé não encontrada: " + rodapePath);
//            }
//        }
//    }
//
//    private void validarDatas(LocalDate inicio, LocalDate fim) {
//        if (inicio == null || fim == null) {
//            throw new InvalidDataException("Datas não podem ser nulas");
//        }
//
//        if (inicio.isBefore(LocalDate.now())) {
//            throw new InvalidDataException("Data inicial não pode ser no passado");
//        }
//
//        if (fim.isBefore(inicio)) {
//            throw new InvalidDataException("Data final deve ser posterior à data inicial");
//        }
//    }
//
//    private void validarDatasConsulta(LocalDate inicio, LocalDate fim) {
//        if (inicio == null || fim == null) {
//            throw new InvalidDataException("Datas de consulta não podem ser nulas");
//        }
//
//        if (fim.isBefore(inicio)) {
//            throw new InvalidDataException("Data final deve ser posterior à data inicial na consulta");
//        }
//    }
//
//    private void validarImagemTema(Tema tema) {
//        if (tema.getCaminhoImagem() == null || tema.getCaminhoImagem().isEmpty()) {
//            throw new InvalidDataException("Tema não possui imagem configurada");
//        }
//    }
//
//    private void validarImagemRodape(Rodape rodape) {
//        if (rodape.getCaminhoImagem() == null || rodape.getCaminhoImagem().isEmpty()) {
//            throw new InvalidDataException("Rodapé não possui imagem configurada");
//        }
//    }
//
//    private void validarImagensProdutos(List<Produto> produtos) {
//        for (Produto produto : produtos) {
//            if (produto.getCaminhoImagem() == null || produto.getCaminhoImagem().isEmpty()) {
//                throw new InvalidDataException("Produto " + produto.getId() + " não possui imagem configurada");
//            }
//        }
//    }
//}