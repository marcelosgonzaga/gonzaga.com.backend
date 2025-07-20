package redeinova.jornalfacil.service;

import com.lowagie.text.DocumentException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redeinova.jornalfacil.dto.NovoProjetoDTO;
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
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjetoService {
    private final PdfService pdfService;

    private final ProjetoRepository projetoRepository;
    private final ProdutoRepository produtoRepository;
    private final TemaRepository temaRepository;
    private final RodapeRepository rodapeRepository;

    @Transactional
    public Projeto criarProjeto(NovoProjetoDTO dto) {

        validarDatas(dto.getDataInicio(), dto.getDataFim());

        Tema tema = temaRepository.findById(dto.getTemaId())
                .orElseThrow(() -> new EntityNotFoundException("Tema não encontrado"));

        Rodape rodape = rodapeRepository.findByCodigoLoja(dto.getCodigoLoja())
                .orElseThrow(() -> new EntityNotFoundException("Rodapé não encontrado para o código: " + dto.getCodigoLoja()));

        List<Produto> produtos = validarEObterProdutos(dto.getProdutoIds());

        Projeto projeto = new Projeto();
        projeto.setDataInicio(dto.getDataInicio());
        projeto.setDataFim(dto.getDataFim());
        projeto.setTema(tema);
        projeto.setRodape(rodape);
        projeto.setProdutos(produtos);

        return projetoRepository.save(projeto);
    }
    @Transactional(readOnly = true)
    public Projeto buscarPorId(Long id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<Projeto> listarTodos() {
        return projetoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Projeto> buscarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return projetoRepository.findByDataInicioBetween(inicio, fim);
    }

    private List<Produto> validarEObterProdutos(List<Long> produtosIds){
        if (produtosIds == null || produtosIds.isEmpty()) {
            throw new InvalidDataException("Lista de produtos não pode ser vazia");
        }
        List<Produto> produtos = produtoRepository.findAllById(produtosIds);

        if (produtos.size() != produtosIds.size()) {
            throw new EntityNotFoundException("Um ou mais produtos não foram encontrados");
        }
        return produtos;
    }

    private void validarQuantidadeProdutos(List<Produto> produtos) {
        if (produtos == null || produtos.isEmpty()) {
            throw new InvalidDataException("Projeto deve conter pelo menos um produto");
        }

        if (produtos.size() % 16 != 0) {
            throw new InvalidDataException("Quantidade de produtos deve ser múltipla de 16 (16 por página)");
        }
    }

    private void validarDatas(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null) {
            throw new InvalidDataException("Datas não podem ser nulas");
        }

        if (inicio.isBefore(LocalDate.now())) {
            throw new InvalidDataException("Data inicial não pode ser no passado");
        }

        if (fim.isBefore(inicio)) {
            throw new InvalidDataException("Data final deve ser posterior à data inicial");
        }
    }

    @Transactional(readOnly = true)
    public byte[] gerarPdfProjeto(Long projetoId) {
        Projeto projeto = buscarPorId(projetoId);
        validarQuantidadeProdutos(projeto.getProdutos());
        try {
            return pdfService.gerarEncarte(projeto);
        } catch (IOException | DocumentException e) {
            throw new PdfGenerationException("Falha ao gerar PDF", e);
        }
    }


}