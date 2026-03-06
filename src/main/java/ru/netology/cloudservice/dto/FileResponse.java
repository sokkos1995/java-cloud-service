package ru.netology.cloudservice.dto;

import java.time.Instant;

public class FileResponse {

    private String filename;
    private long size;
    private long editedAt;

    public FileResponse() {
    }

    public FileResponse(String filename, long size) {
        this.filename = filename;
        this.size = size;
        this.editedAt = System.currentTimeMillis();
    }

    public FileResponse(String filename, long size, Instant uploadTime) {
        this.filename = filename;
        this.size = size;
        this.editedAt = uploadTime != null ? uploadTime.toEpochMilli() : System.currentTimeMillis();
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getEditedAt() {
        return editedAt;
    }

    public void setEditedAt(long editedAt) {
        this.editedAt = editedAt;
    }
}
