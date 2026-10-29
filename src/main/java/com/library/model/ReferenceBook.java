package com.library.model;

/**
 * Concrete subclass representing strictly non-circulating reference materials
 * (e.g., Encyclopedias, Dictionaries, Handbooks).
 * Demonstrates polymorphic overriding where borrowing is disallowed.
 *
 * [Concept: Inheritance - Subclass of LibraryItem]
 * [Concept: Method Overriding]
 */
public class ReferenceBook extends LibraryItem {

    private String fieldOfStudy;
    private String edition;
    private String deweyDecimal;

    public ReferenceBook(int id, String title, String publisher, int year,
                         int totalCopies, int availableCopies,
                         String fieldOfStudy, String edition, String deweyDecimal) {
        super(id, title, publisher, year, totalCopies, availableCopies, ItemType.REFERENCE);
        this.fieldOfStudy = fieldOfStudy;
        this.edition = edition;
        this.deweyDecimal = deweyDecimal;
    }

    // [Concept: Method Overriding] Reference books CANNOT be loaned out
    @Override
    public boolean isBorrowable() {
        return false;
    }

    @Override
    public int getLoanPeriodDays() {
        return 0; // Strictly reading-room use only
    }

    @Override
    public double calculateFine(long overdueDays) {
        return 0.0; // Cannot be checked out, no standard loan fines
    }

    @Override
    public double getDailyLateFee() {
        return 0.0;
    }

    @Override
    public String getDisplayDetails() {
        return String.format("Reference Only | Discipline: %s | Edition: %s | Dewey: %s",
                fieldOfStudy, edition, deweyDecimal);
    }

    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) return true;
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (fieldOfStudy != null && fieldOfStudy.toLowerCase().contains(q)) ||
               (edition != null && edition.toLowerCase().contains(q)) ||
               (deweyDecimal != null && deweyDecimal.toLowerCase().contains(q));
    }

    public String getFieldOfStudy() {
        return fieldOfStudy;
    }

    public void setFieldOfStudy(String fieldOfStudy) {
        this.fieldOfStudy = fieldOfStudy;
    }

    public String getEdition() {
        return edition;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public String getDeweyDecimal() {
        return deweyDecimal;
    }

    public void setDeweyDecimal(String deweyDecimal) {
        this.deweyDecimal = deweyDecimal;
    }
}
