package ru.netology.cloudservice.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    String save(String userLogin, String filename, MultipartFile file) throws IOException;

    void delete(String userLogin, String storageFilename) throws IOException;

    Path getPath(String userLogin, String storageFilename);
}
