package redeinova.jornalfacil.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import redeinova.jornalfacil.exception.FileStorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

        private final Path fileStorageLocation;

        public FileStorageService(@Value("${file.upload-dir}") String uploadDir) {
            try {
                this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
                Files.createDirectories(this.fileStorageLocation);
            } catch (Exception ex) {
                throw new FileStorageException("Não foi possível criar o diretório de upload", ex);
            }
        }

    private static final List<String> ALLOWED_EXTENSIONS = List.of(".png", ".jpg", ".jpeg");
    //Verifique se o diretório existe e tem permissões
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de upload", e);
        }
    }

    @Value("${file.upload-dir}")
    private String uploadDir;



    public String storeFile(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new FileStorageException("Arquivo não pode ser nulo ou vazio");
        }

        try {
            String originalFileName = Objects.requireNonNull(file.getOriginalFilename());
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();

            if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
                throw new FileStorageException("Tipo de arquivo não permitido. Extensões permitidas: " + ALLOWED_EXTENSIONS);
            }

            // Normaliza o caminho do diretório
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

            // Cria o diretório se não existir
            Files.createDirectories(uploadPath);

            // Gera um nome de arquivo único
            String fileName = prefix + "_" + System.currentTimeMillis() + fileExtension;

            // Verifica se o nome do arquivo contém caracteres inválidos
            Path targetLocation = uploadPath.resolve(fileName).normalize();
            if (!targetLocation.startsWith(uploadPath)) {
                throw new FileStorageException("Nome do arquivo contém sequência inválida");
            }

            // Copia o arquivo para o local de destino
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return fileName;
        } catch (IOException ex) {
            throw new FileStorageException("Falha ao armazenar arquivo: " + ex.getMessage(), ex);
        }
    }

    public List<String> listarArquivos(String subdirectory) {
        Path dirPath = Paths.get(uploadDir, subdirectory);

        try {
            // Verifica se o diretório existe
            if (!Files.exists(dirPath)) {
                return List.of(); // Retorna lista vazia se o diretório não existir
            }

            // Lista apenas arquivos (ignora subdiretórios)
            try (Stream<Path> paths = Files.list(dirPath)) {
                return paths.filter(Files::isRegularFile)  // Filtra apenas arquivos regulares
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .collect(Collectors.toList());
            }
        } catch (IOException ex) {
            throw new FileStorageException("Falha ao listar arquivos do diretório: " + subdirectory, ex);
        }
    }
}