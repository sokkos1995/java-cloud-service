package ru.netology.cloudservice.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservice.config.AppProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDir;

    public LocalFileStorageService(AppProperties appProperties) {
        this.rootDir = Path.of(appProperties.getRootDir());
    }

    @Override
    public String save(String userLogin, String filename, MultipartFile file) throws IOException {
        String storageFilename = UUID.randomUUID() + "_" + filename;
        Path userDir = rootDir.resolve(userLogin);
        Files.createDirectories(userDir);
        Files.copy(file.getInputStream(), userDir.resolve(storageFilename));
        return storageFilename;
    }

    @Override
    public void delete(String userLogin, String storageFilename) throws IOException {
        Path path = getPath(userLogin, storageFilename);
        Files.deleteIfExists(path);
    }

    @Override
    public Path getPath(String userLogin, String storageFilename) {
        return rootDir.resolve(userLogin).resolve(storageFilename);
    }
}
