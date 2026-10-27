package com.splitwise.exception;

/** Thrown when the request is well-formed but violates a business rule. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
