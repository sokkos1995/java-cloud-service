package ru.netology.cloudservice.dto;

/**
 * Тело запроса PUT /file для переименования файла.
 * Спецификация: { "name": "новое_имя" }. Фронтенд отправляет { "filename": "..." } — поддерживаем оба варианта.
 */
public class RenameFileRequest {

    private String name;
    private String filename;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    /** Новое имя файла — из name или filename (для совместимости с фронтом) */
    public String getNewName() {
        if (name != null && !name.isBlank()) return name;
        if (filename != null && !filename.isBlank()) return filename;
        return null;
    }
}
