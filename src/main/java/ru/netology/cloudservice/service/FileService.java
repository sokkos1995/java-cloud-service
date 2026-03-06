package ru.netology.cloudservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservice.dto.FileResponse;

import java.util.List;

public interface FileService {

    List<FileResponse> listFiles(String currentUserLogin, Integer limit);

    void uploadFile(String currentUserLogin, String filename, MultipartFile file);

    void deleteFile(String currentUserLogin, String filename);

    /**
     * Возвращает файл для скачивания.
     *
     * @return Resource (например, PathResource) или null, если файл не найден
     */
    Resource downloadFile(String currentUserLogin, String filename);

    /**
     * Переименование файла (меняется только логическое имя в БД).
     *
     * @param newName новое имя файла
     */
    void renameFile(String currentUserLogin, String filename, String newName);
}
