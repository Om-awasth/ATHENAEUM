package com.library.exception;

/**
 * Thrown when a queried entity (Book, Member, Transaction) does not exist in the database.
 * [Concept: Exception Handling - Custom Exception]
 */
public class ItemNotFoundException extends LibraryException {

    private final String identifier;

    public ItemNotFoundException(String identifier, String message) {
        super(message);
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
