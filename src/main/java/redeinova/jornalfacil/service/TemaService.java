package redeinova.jornalfacil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import redeinova.jornalfacil.model.Tema;
import redeinova.jornalfacil.repository.TemaRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TemaService {

    private final TemaRepository temaRepository;

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

}