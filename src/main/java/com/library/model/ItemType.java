package com.library.model;

/**
 * Enumeration representing different categories of items in the library.
 * [Concept: Enum type]
 */
public enum ItemType {
    BOOK("Book"),
    MAGAZINE("Magazine"),
    REFERENCE("Reference Book"),
    JOURNAL("Academic Journal");

    private final String displayName;

    ItemType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
