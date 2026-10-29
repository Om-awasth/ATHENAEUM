package com.library.model;

/**
 * Interface defining search filtering capability for catalog items and persons.
 * [Concept: Interface]
 */
public interface Searchable {

    /**
     * Determines whether the entity matches a free-text search query.
     *
     * @param query case-insensitive search term
     * @return true if the entity matches, false otherwise
     */
    boolean matchesQuery(String query);
}
