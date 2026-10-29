package com.library.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface declaring standard CRUD operations.
 * [Concept: Generic Interface]
 *
 * @param <T> Domain entity type
 * @param <ID> Primary key identifier type (e.g. Integer)
 */
public interface Repository<T, ID> {

    /**
     * Retrieves an entity by its unique identifier.
     *
     * @param id primary key
     * @return Optional containing found entity, or empty
     */
    Optional<T> findById(ID id);

    /**
     * Retrieves all stored entities.
     *
     * @return list of entities
     */
    List<T> findAll();

    /**
     * Persists a new entity into the database.
     *
     * @param entity entity to save
     * @return saved entity with generated ID populated
     */
    T save(T entity);

    /**
     * Updates an existing entity.
     *
     * @param entity entity with modified attributes
     * @return true if updated successfully, false otherwise
     */
    boolean update(T entity);

    /**
     * Deletes an entity by primary key.
     *
     * @param id entity identifier
     * @return true if deleted, false if not found
     */
    boolean deleteById(ID id);
}
