package com.library.exception;

/**
 * Thrown when attempting to insert a record violating unique constraints (e.g., duplicate email or ISBN).
 * [Concept: Exception Handling - Custom Exception]
 */
public class DuplicateEntryException extends LibraryException {

    private final String field;
    private final String value;

    public DuplicateEntryException(String field, String value, String message) {
        super(message);
        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }
}
