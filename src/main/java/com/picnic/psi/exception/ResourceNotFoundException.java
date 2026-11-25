package com.picnic.psi.exception;

/**
 * Thrown when a requested domain resource (such as a page or product) does not exist.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
