package com.bottleneck.detective.exception;

/**
 * Thrown when a request violates business rules.
 * E.g., trying to create a production line with a duplicate name.
 * Maps to HTTP 400 in the GlobalExceptionHandler.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
