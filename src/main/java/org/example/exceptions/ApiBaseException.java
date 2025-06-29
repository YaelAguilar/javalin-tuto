package org.example.exceptions;

public abstract class ApiBaseException extends RuntimeException {
    private final int statusCode;

    public ApiBaseException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}