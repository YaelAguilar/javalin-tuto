package org.example.exceptions;

public class NotFoundException extends ApiBaseException {
    public NotFoundException(String message) {
        super(404, message);
    }
}