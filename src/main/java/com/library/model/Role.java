package com.library.model;

/**
 * Enumeration representing user security and operational roles.
 * [Concept: Enum type]
 */
public enum Role {
    LIBRARIAN("Librarian"),
    MEMBER("Library Member");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
