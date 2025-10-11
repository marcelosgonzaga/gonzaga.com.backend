package gonzaga.jornalfacil.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import gonzaga.jornalfacil.model.Rodape;
import gonzaga.jornalfacil.repository.RodapeRepository;

import java.util.List;
import java.util.Optional;

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

    // NOVOS MÉTODOS CRUD
    public Optional<Rodape> buscarPorId(Long id) {
        return rodapeRepository.findById(id);
    }

    public Rodape salvar(Rodape rodape) {
        return rodapeRepository.save(rodape);
    }

    public void deletarPorId(Long id) {
        rodapeRepository.deleteById(id);
    }

    public Optional<Rodape> buscarPorCodigoExato(Long codigoLoja) {
        return rodapeRepository.findByCodigoLoja(codigoLoja);
    }
}



//package gonzaga.jornalfacil.service;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import gonzaga.jornalfacil.model.Rodape;
//import gonzaga.jornalfacil.repository.RodapeRepository;
//
//import java.util.List;
//
//@Service
//@RequiredArgsConstructor
//public class RodapeService {
//    private final RodapeRepository rodapeRepository;
//
//    public List<Rodape> listarTodos() {
//        return rodapeRepository.findAll();
//    }
//
//    public List<Rodape> buscarPorCodigo(String codigo) {
//        return rodapeRepository.findByCodigoLojaContaining(codigo);
//    }
//}