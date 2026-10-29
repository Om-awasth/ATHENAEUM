package com.library.model;

import java.util.Objects;

/**
 * Root abstract class for all holdings in the library catalog.
 * Demonstrates abstract classes, encapsulation, inheritance, constructor chaining,
 * interfaces, static members, and Comparable contract.
 *
 * [Concept: Abstract Class]
 * [Concept: Inheritance]
 * [Concept: Access Modifiers]
 * [Concept: Static Keyword]
 * [Concept: Final Keyword]
 * [Concept: Comparable Interface]
 */
public abstract class LibraryItem implements Borrowable, Searchable, Comparable<LibraryItem> {

    // [Concept: Static variable] Shared default currency symbol
    public static final String CURRENCY_SYMBOL = "₹";

    // [Concept: Protected Access Modifier] Accessible to subclasses
    protected int id;
    protected String title;
    protected String publisher;
    protected int year;
    protected int totalCopies;
    protected int availableCopies;
    protected final ItemType itemType; // [Concept: Final keyword] Immutable item type

    /**
     * Primary constructor demonstrating constructor chaining.
     */
    public LibraryItem(int id, String title, String publisher, int year,
                       int totalCopies, int availableCopies, ItemType itemType) {
        this.id = id;
        this.title = Objects.requireNonNull(title, "Title cannot be null");
        this.publisher = Objects.requireNonNull(publisher, "Publisher cannot be null");
        this.year = year;
        this.totalCopies = Math.max(1, totalCopies);
        this.availableCopies = Math.min(this.totalCopies, Math.max(0, availableCopies));
        this.itemType = Objects.requireNonNull(itemType, "ItemType cannot be null");
    }

    /**
     * [Concept: Constructor Chaining] Secondary constructor defaulting copies to 1.
     */
    public LibraryItem(String title, String publisher, int year, ItemType itemType) {
        this(0, title, publisher, year, 1, 1, itemType);
    }

    // [Concept: Abstract Methods] Implemented polymorphically by subclasses
    /**
     * Calculates the loan period in days based on specific material type.
     * [Concept: Polymorphism]
     */
    public abstract int getLoanPeriodDays();

    /**
     * Calculates the overdue fine for a given number of late days.
     * [Concept: Polymorphism]
     *
     * @param overdueDays number of days elapsed past due date
     * @return calculated fine amount in INR
     */
    public abstract double calculateFine(long overdueDays);

    /**
     * Returns a human-readable summary of subclass-specific details.
     * [Concept: Polymorphism]
     */
    public abstract String getDisplayDetails();

    // Borrowable interface implementation defaults
    @Override
    public boolean isBorrowable() {
        return true;
    }

    @Override
    public int getMaxLoanDays() {
        return getLoanPeriodDays();
    }

    // Searchable interface implementation
    @Override
    public boolean matchesQuery(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String q = query.trim().toLowerCase();
        return title.toLowerCase().contains(q) ||
               publisher.toLowerCase().contains(q) ||
               String.valueOf(year).contains(q);
    }

    // [Concept: Comparable] Natural sorting order by title alphabetically
    @Override
    public int compareTo(LibraryItem other) {
        if (other == null) return 1;
        int titleCompare = this.title.compareToIgnoreCase(other.title);
        if (titleCompare != 0) return titleCompare;
        return Integer.compare(this.id, other.id);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    @Override
    public String toString() {
        return String.format("[%s #%d] %s (%d) - %d/%d available",
                itemType, id, title, year, availableCopies, totalCopies);
    }
}
