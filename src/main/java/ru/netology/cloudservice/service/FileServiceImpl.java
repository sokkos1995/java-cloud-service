package ru.netology.cloudservice.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservice.dto.FileResponse;
import ru.netology.cloudservice.entity.FileEntity;
import ru.netology.cloudservice.entity.UserEntity;
import ru.netology.cloudservice.repository.FileRepository;
import ru.netology.cloudservice.repository.UserRepository;
import ru.netology.cloudservice.storage.FileStorageService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Реализация сервиса работы с файлами.
 *
 * <p>Архитектура хранения:
 * <ul>
 *   <li>Метаданные (имя, размер, тип, владелец) — в БД (FileEntity)</li>
 *   <li>Сам файл — на диске в LocalFileStorageService (под storage.root-dir)</li>
 *   <li>storageFilename — внутреннее имя (UUID_filename) для избежания коллизий</li>
 * </ul>
 *
 * <p>Поток при загрузке:
 * 1. LocalFileStorageService.save() сохраняет файл на диск, возвращает storageFilename
 * 2. Создаём FileEntity и сохраняем в БД
 *
 * <p>Поток при скачивании:
 * 1. Находим FileEntity по user + filename
 * 2. По storageFilename получаем Path через FileStorageService
 * 3. Возвращаем Resource для отдачи клиенту
 */
public class FileServiceImpl implements FileService {

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public FileServiceImpl(FileRepository fileRepository,
                          UserRepository userRepository,
                          FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public List<FileResponse> listFiles(String currentUserLogin, Integer limit) {
        UserEntity user = getUser(currentUserLogin);

        var stream = fileRepository.findAllByUser(user).stream()
                .map(f -> new FileResponse(f.getFilename(), f.getSize(), f.getUploadTime()));
        return (limit != null && limit > 0)
                ? stream.limit(limit).collect(Collectors.toList())
                : stream.collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void uploadFile(String currentUserLogin, String filename, MultipartFile file) {
        validateFilename(filename);
        UserEntity user = getUser(currentUserLogin);

        // Если файл с таким именем уже есть — удаляем старый (перезапись)
        fileRepository.findByUserAndFilename(user, filename).ifPresent(existing -> {
            try {
                fileStorageService.delete(currentUserLogin, existing.getStorageFilename());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete existing file", e);
            }
            fileRepository.delete(existing);
        });

        try {
            String storageFilename = fileStorageService.save(currentUserLogin, filename, file);

            FileEntity entity = new FileEntity();
            entity.setUser(user);
            entity.setFilename(filename);
            entity.setStorageFilename(storageFilename);
            entity.setSize(file.getSize());
            entity.setContentType(file.getContentType());
            entity.setUploadTime(Instant.now());
            fileRepository.save(entity);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file", e);
        }
    }

    @Override
    @Transactional
    public void deleteFile(String currentUserLogin, String filename) {
        validateFilename(filename);
        FileEntity entity = getFileEntity(currentUserLogin, filename);

        try {
            fileStorageService.delete(currentUserLogin, entity.getStorageFilename());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file from storage", e);
        }
        fileRepository.delete(entity);
    }

    @Override
    public Resource downloadFile(String currentUserLogin, String filename) {
        validateFilename(filename);
        FileEntity entity = getFileEntity(currentUserLogin, filename);

        Path path = fileStorageService.getPath(currentUserLogin, entity.getStorageFilename());
        try {
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new RuntimeException("File not readable");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid file path", e);
        }
    }

    @Override
    @Transactional
    public void renameFile(String currentUserLogin, String filename, String newName) {
        validateFilename(filename);
        validateFilename(newName);

        FileEntity entity = getFileEntity(currentUserLogin, filename);
        UserEntity user = getUser(currentUserLogin);

        // Проверяем, что новое имя не занято
        if (fileRepository.findByUserAndFilename(user, newName).isPresent()) {
            throw new RuntimeException("File with name '" + newName + "' already exists");
        }

        entity.setFilename(newName);
        entity.setUploadTime(Instant.now()); // обновляем дату изменения при переименовании
        fileRepository.save(entity);
        // Физический файл на диске не трогаем — storageFilename остаётся тем же
    }

    private UserEntity getUser(String login) {
        return userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private FileEntity getFileEntity(String userLogin, String filename) {
        UserEntity user = getUser(userLogin);
        return fileRepository.findByUserAndFilename(user, filename)
                .orElseThrow(() -> new RuntimeException("File not found"));
    }

    private void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new RuntimeException("Filename is required");
        }
    }
}
