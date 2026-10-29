package com.library.service;

import com.library.dao.BookDAO;
import com.library.model.Book;
import com.library.model.ItemType;
import com.library.model.LibraryItem;
import com.library.util.GenericUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing catalog operations, cached lookups, sorting, and stream aggregations.
 * [Concept: Collections Framework - ArrayList, HashMap, TreeMap, HashSet, Iterator, Streams]
 * [Concept: Generics - Bounded Type Parameters and Wildcards]
 */
public class CatalogService {

    private static final Logger logger = LoggerFactory.getLogger(CatalogService.class);

    private final BookDAO bookDAO;

    // [Concept: Collections Framework - HashMap for O(1) ISBN lookups]
    private final Map<String, Book> isbnCache = new HashMap<>();

    // [Concept: Collections Framework - TreeMap for naturally sorted catalog by title]
    private final TreeMap<String, LibraryItem> sortedCatalogCache = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    // [Concept: Collections Framework - HashSet for unique genre and category tags]
    private final Set<String> uniqueCategories = new HashSet<>();

    public CatalogService(BookDAO bookDAO) {
        this.bookDAO = Objects.requireNonNull(bookDAO, "bookDAO cannot be null");
        refreshCaches();
    }

    /**
     * Refreshes internal in-memory collections from database using Iterator and Streams.
     * [Concept: Collections Framework - Iterator]
     * [Concept: Collections Framework - Java Streams]
     */
    public synchronized void refreshCaches() {
        // [Concept: Collections Framework - ArrayList]
        List<LibraryItem> allItems = bookDAO.findAll();

        isbnCache.clear();
        sortedCatalogCache.clear();
        uniqueCategories.clear();

        // [Concept: Collections Framework - Iterator traversal]
        Iterator<LibraryItem> iterator = allItems.iterator();
        while (iterator.hasNext()) {
            LibraryItem item = iterator.next();

            // Populate TreeMap sorted cache
            sortedCatalogCache.put(item.getTitle() + "::" + item.getId(), item);

            // Populate HashMap for books
            if (item instanceof Book book && book.getIsbn() != null) {
                isbnCache.put(book.getIsbn().replaceAll("[-\\s]", ""), book);
                if (book.getGenre() != null && !book.getGenre().isBlank()) {
                    uniqueCategories.add(book.getGenre());
                }
            }
        }

        // Add standard item types to categories
        for (ItemType type : ItemType.values()) {
            uniqueCategories.add(type.getDisplayName());
        }

        logger.info("Catalog caches refreshed. Indexed {} items, {} ISBNs, {} categories.",
                sortedCatalogCache.size(), isbnCache.size(), uniqueCategories.size());
    }

    /**
     * [Concept: Generic Method with Bounded Type Parameter]
     * Filters available items using GenericUtils.
     */
    public <T extends LibraryItem> List<T> getAvailableOnly(List<T> items) {
        return GenericUtils.filterAvailable(items);
    }

    /**
     * [Concept: Generics - Wildcards]
     * Computes hypothetical fine simulation across all catalog holdings.
     */
    public double simulateInventoryOverdueFine(int simulatedDays) {
        List<LibraryItem> items = bookDAO.findAll();
        // Passing List<LibraryItem> to wildcard method accepting List<? extends LibraryItem>
        return GenericUtils.calculateSimulatedFines(items, simulatedDays);
    }

    /**
     * Retrieves all items, optionally sorted by year or title using Comparator.
     * [Concept: Collections Framework - Comparator]
     */
    public List<LibraryItem> getAllItems(String sortBy) {
        List<LibraryItem> items = bookDAO.findAll();

        if ("year".equalsIgnoreCase(sortBy)) {
            // [Concept: Collections.sort with Custom Comparator]
            items.sort(Comparator.comparingInt(LibraryItem::getYear).reversed());
        } else if ("available".equalsIgnoreCase(sortBy)) {
            items.sort(Comparator.comparingInt(LibraryItem::getAvailableCopies).reversed());
        } else {
            // [Concept: Comparable natural ordering via Collections.sort]
            Collections.sort(items);
        }
        return items;
    }

    public Optional<LibraryItem> getItemById(int id) {
        return bookDAO.findById(id);
    }

    public Optional<Book> findByIsbnFast(String isbn) {
        if (isbn == null) return Optional.empty();
        String clean = isbn.replaceAll("[-\\s]", "");
        if (isbnCache.containsKey(clean)) {
            return Optional.of(isbnCache.get(clean));
        }
        return bookDAO.findByIsbn(isbn);
    }

    /**
     * Searches items with filter criteria.
     */
    public List<LibraryItem> searchCatalog(String query, String category, boolean availableOnly) {
        return bookDAO.search(query, category, availableOnly);
    }

    /**
     * [Concept: Java Streams - Grouping by Category and Counting]
     * Aggregates inventory breakdown for dashboard visualization.
     */
    public Map<String, Long> getInventoryDistribution() {
        List<LibraryItem> items = bookDAO.findAll();
        return items.stream()
                .collect(Collectors.groupingBy(
                        item -> (item instanceof Book b && b.getGenre() != null) ? b.getGenre() : item.getItemType().getDisplayName(),
                        Collectors.counting()
                ));
    }

    public Set<String> getUniqueCategories() {
        return Collections.unmodifiableSet(uniqueCategories);
    }

    public LibraryItem addItem(LibraryItem item) {
        LibraryItem saved = bookDAO.save(item);
        refreshCaches();
        return saved;
    }

    public boolean updateItem(LibraryItem item) {
        boolean updated = bookDAO.update(item);
        if (updated) refreshCaches();
        return updated;
    }

    public boolean updateCopies(int id, int total, int available) {
        boolean updated = bookDAO.updateCopies(id, total, available);
        if (updated) refreshCaches();
        return updated;
    }

    public boolean deleteItem(int id) {
        boolean deleted = bookDAO.deleteById(id);
        if (deleted) refreshCaches();
        return deleted;
    }
}
