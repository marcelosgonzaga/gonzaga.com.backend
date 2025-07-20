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
                        // Extrai apenas o nome do arquivo
                        String nomeArquivo = tema.getCaminhoImagem();
                        // Remove qualquer caminho completo ou parcial
                        nomeArquivo = nomeArquivo.substring(nomeArquivo.lastIndexOf("/") + 1);
                        tema.setCaminhoImagem(nomeArquivo);
                    }
                    return tema;
                })
                .collect(Collectors.toList());
    }

//    public List<Tema> listarTodos() {
//        return temaRepository.findAll().stream()
//                .map(tema -> {
//                    if (tema.getCaminhoImagem() != null) {
//                        // Extrai apenas o nome do arquivo
//                        String nomeArquivo = tema.getCaminhoImagem().substring(tema.getCaminhoImagem().lastIndexOf("/") + 1);
//                        tema.setCaminhoImagem("http://localhost:8080/api/temas/imagens/" + nomeArquivo);
//                    }
//                    return tema;
//                })
//                .collect(Collectors.toList());
//    }

    public List<Tema> buscarTemasPorDescricao(String termoBusca) {
       return temaRepository.buscarPorDescricao(termoBusca);
    }

}