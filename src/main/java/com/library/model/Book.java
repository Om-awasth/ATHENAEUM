package com.library.model;

/**
 * Concrete subclass representing standard circulating books.
 * [Concept: Inheritance - Subclass of LibraryItem]
 * [Concept: Use of super keyword]
 * [Concept: Method Overriding]
 */
public class Book extends LibraryItem {

    // [Concept: Static Constant] Standard rates for regular books
    public static final int STANDARD_LOAN_DAYS = 14;
    public static final double DAILY_FINE_RATE = 2.0; // ₹2.00 per day overdue

    private String isbn;
    private String author;
    private String genre;

    /**
     * Full constructor invoking super constructor.
     * [Concept: super keyword]
     */
    public Book(int id, String title, String publisher, int year,
                int totalCopies, int availableCopies,
                String isbn, String author, String genre) {
        // [Concept: super constructor invocation]
        super(id, title, publisher, year, totalCopies, availableCopies, ItemType.BOOK);
        this.isbn = isbn;
        this.author = author;
        this.genre = genre;
    }

    /**
     * Convenience constructor with default single copy.
     */
    public Book(String title, String publisher, int year, String isbn, String author, String genre) {
        super(title, publisher, year, ItemType.BOOK);
        this.isbn = isbn;
        this.author = author;
        this.genre = genre;
    }

    // [Concept: Method Overriding] Regular books get 14 days loan period
    @Override
    public int getLoanPeriodDays() {
        return STANDARD_LOAN_DAYS;
    }

    // [Concept: Method Overriding] Overdue fine calculation: ₹2/day
    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) return 0.0;
        return overdueDays * DAILY_FINE_RATE;
    }

    // [Concept: Method Overriding] Custom display details
    @Override
    public String getDisplayDetails() {
        return String.format("Author: %s | ISBN: %s | Genre: %s", author, isbn, genre);
    }

    @Override
    public double getDailyLateFee() {
        return DAILY_FINE_RATE;
    }

    // [Concept: Method Overriding] Extends query matching to author, isbn, and genre
    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) {
            return true;
        }
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (author != null && author.toLowerCase().contains(q)) ||
               (isbn != null && isbn.toLowerCase().contains(q)) ||
               (genre != null && genre.toLowerCase().contains(q));
    }

    // Getters and Setters
    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }
}
