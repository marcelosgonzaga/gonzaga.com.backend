package gonzaga.jornalfacil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import gonzaga.jornalfacil.model.Tema;
import gonzaga.jornalfacil.repository.TemaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemaService {

    private final TemaRepository temaRepository;
    private final FileStorageService fileStorageService;

    public List<Tema> listarTodos() {
        return temaRepository.findAll().stream()
                .map(tema -> {
                    if (tema.getCaminhoImagem() != null) {
                        // Garante que só o nome do arquivo será usado
                        String nomeArquivo = tema.getCaminhoImagem();
                        nomeArquivo = nomeArquivo.substring(nomeArquivo.lastIndexOf("/") + 1);
                        tema.setCaminhoImagem(nomeArquivo);
                    }
                    return tema;
                })
                .collect(Collectors.toList());
    }

    public List<Tema> buscarTemasPorDescricao(String termoBusca) {
        return temaRepository.buscarPorDescricao(termoBusca);
    }

    // NOVO: Criar tema
    public Tema criarTema(String nome, String descricao, MultipartFile imagem) {
        try {
            // Verificar se já existe tema com mesma descrição
            Optional<Tema> temaExistente = temaRepository.findByDescricao(descricao);
            if (temaExistente.isPresent()) {
                throw new RuntimeException("Já existe um tema com esta descrição");
            }

            // Salvar a imagem
            String nomeArquivo = fileStorageService.storeFile(imagem, "tema");

            // Criar e salvar o tema
            Tema novoTema = new Tema();
            novoTema.setNome(nome);
            novoTema.setDescricao(descricao);
            novoTema.setCaminhoImagem(nomeArquivo);

            return temaRepository.save(novoTema);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao criar tema: " + e.getMessage(), e);
        }
    }

    // NOVO: Atualizar tema
    public Tema atualizarTema(Long id, String nome, String descricao, MultipartFile imagem) {
        try {
            Tema tema = temaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tema não encontrado"));

            tema.setNome(nome);
            tema.setDescricao(descricao);

            // Se uma nova imagem foi fornecida, atualizar
            if (imagem != null && !imagem.isEmpty()) {
                String nomeArquivo = fileStorageService.storeFile(imagem, "tema");
                tema.setCaminhoImagem(nomeArquivo);
            }

            return temaRepository.save(tema);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao atualizar tema: " + e.getMessage(), e);
        }
    }

    // NOVO: Deletar tema
    public void deletarTema(Long id) {
        try {
            Tema tema = temaRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Tema não encontrado"));

            // TODO: Adicionar lógica para deletar o arquivo de imagem se necessário
            temaRepository.delete(tema);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar tema: " + e.getMessage(), e);
        }
    }
}