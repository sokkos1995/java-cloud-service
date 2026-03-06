package ru.netology.cloudservice.dto;

/**
 * Соответствует схеме Error из спецификации: { message, id }
 */
public class ErrorResponse {

    private String message;
    private int id;

    public ErrorResponse() {
    }

    public ErrorResponse(String message, int id) {
        this.message = message;
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public int getId() {
        return id;
    }
}
