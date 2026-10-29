package com.library.exception;

/**
 * Thrown when attempting to issue an item that has 0 available copies or is non-circulating.
 * [Concept: Exception Handling - Custom Exception]
 */
public class BookNotAvailableException extends LibraryException {

    private final int itemId;

    public BookNotAvailableException(int itemId, String message) {
        super(message);
        this.itemId = itemId;
    }

    public int getItemId() {
        return itemId;
    }
}
