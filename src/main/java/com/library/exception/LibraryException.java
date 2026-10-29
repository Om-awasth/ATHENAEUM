package com.library.exception;

/**
 * Base unchecked exception for all library domain errors.
 * [Concept: Exception Handling - Custom Exception Hierarchy]
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
