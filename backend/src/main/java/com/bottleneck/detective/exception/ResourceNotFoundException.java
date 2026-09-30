package com.bottleneck.detective.exception;

/**
 * Thrown when a requested resource (e.g., a ProductionLine by ID) does not exist.
 * Maps to HTTP 404 in the GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " not found with id: " + id);
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
