package org.example.exceptions;

public class UnauthorizedException extends ApiBaseException {
    public UnauthorizedException(String message) {
        super(401, message);
    }
}