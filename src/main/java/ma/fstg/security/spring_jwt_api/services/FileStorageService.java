package ma.fstg.security.spring_jwt_api.services;

import jakarta.annotation.PostConstruct;
import ma.fstg.security.spring_jwt_api.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(@Value("${app.upload.dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier d'upload", e);
        }
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("Le fichier doit être une image");
        }

        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = System.currentTimeMillis() + "_" + sanitized;
        Path target = root.resolve(filename).normalize();

        if (!target.startsWith(root)) {
            throw new BadRequestException("Chemin de fichier invalide");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Échec de l'enregistrement du fichier", e);
        }

        return "/uploads/" + filename;
    }
}
