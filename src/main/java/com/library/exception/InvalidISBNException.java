package com.library.exception;

/**
 * Thrown when an ISBN number fails standard length or checksum validation.
 * [Concept: Exception Handling - Custom Exception]
 */
public class InvalidISBNException extends LibraryException {

    private final String invalidIsbn;

    public InvalidISBNException(String invalidIsbn, String message) {
        super(message);
        this.invalidIsbn = invalidIsbn;
    }

    public String getInvalidIsbn() {
        return invalidIsbn;
    }
}
