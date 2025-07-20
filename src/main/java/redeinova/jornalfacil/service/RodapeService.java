package redeinova.jornalfacil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import redeinova.jornalfacil.model.Rodape;
import redeinova.jornalfacil.repository.RodapeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RodapeService {
    private final RodapeRepository rodapeRepository;

    public List<Rodape> listarTodos() {
        return rodapeRepository.findAll();
    }

    public List<Rodape> buscarPorCodigo(String codigo) {
        return rodapeRepository.findByCodigoLojaContaining(codigo);
    }
}