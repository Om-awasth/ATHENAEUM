package com.library.model;

/**
 * Enumeration representing the lifecycle states of a book circulation record.
 * [Concept: Enum type]
 */
public enum TransactionStatus {
    ISSUED("Issued"),
    RETURNED("Returned"),
    OVERDUE("Overdue"),
    LOST("Marked Lost");

    private final String label;

    TransactionStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
