package com.splitwise.exception;

/** Thrown when an authenticated user is not allowed to perform an action. Mapped to HTTP 403. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
