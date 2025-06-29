package org.example.exceptions;

public class BadRequestException extends ApiBaseException {
    public BadRequestException(String message) {
        super(400, message);
    }
}