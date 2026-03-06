package ru.netology.cloudservice.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.netology.cloudservice.dto.FileResponse;
import ru.netology.cloudservice.dto.RenameFileRequest;
import ru.netology.cloudservice.security.AuthenticatedUser;
import ru.netology.cloudservice.service.FileService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public ResponseEntity<List<FileResponse>> listFiles(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(fileService.listFiles(user.getUsername(), limit));
    }

    @PostMapping("/file")
    public ResponseEntity<Void> uploadFile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("filename") String filename,
            @RequestParam("file") MultipartFile file
    ) {
        fileService.uploadFile(user.getUsername(), filename, file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/file")
    public ResponseEntity<Void> deleteFile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("filename") String filename
    ) {
        fileService.deleteFile(user.getUsername(), filename);
        return ResponseEntity.ok().build();
    }

    /**
     * Скачивание файла. Возвращает файл с заголовком Content-Disposition
     * для корректного имени при сохранении на клиенте.
     */
    @GetMapping("/file")
    public ResponseEntity<Resource> downloadFile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("filename") String filename
    ) {
        Resource resource = fileService.downloadFile(user.getUsername(), filename);

        // Content-Disposition заставляет браузер предложить сохранить с нужным именем
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                .replace("+", "%20");
        String contentDisposition = "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encodedFilename;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * Переименование файла. Тело: { "name": "..." } или { "filename": "..." } (для фронтенда)
     */
    @PutMapping("/file")
    public ResponseEntity<Void> renameFile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam("filename") String filename,
            @RequestBody RenameFileRequest request
    ) {
        String newName = request.getNewName();
        if (newName == null || newName.isBlank()) {
            throw new RuntimeException("Name is required");
        }
        fileService.renameFile(user.getUsername(), filename, newName);
        return ResponseEntity.ok().build();
    }
}
