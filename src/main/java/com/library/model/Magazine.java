package com.library.model;

/**
 * Concrete subclass representing periodicals and magazines.
 * [Concept: Inheritance - Subclass of LibraryItem]
 * [Concept: Method Overriding]
 */
public class Magazine extends LibraryItem {

    public static final int MAGAZINE_LOAN_DAYS = 7;
    public static final double DAILY_FINE_RATE = 1.0; // ₹1.00 per day overdue

    private int issueNumber;
    private String month;
    private String category;

    public Magazine(int id, String title, String publisher, int year,
                    int totalCopies, int availableCopies,
                    int issueNumber, String month, String category) {
        super(id, title, publisher, year, totalCopies, availableCopies, ItemType.MAGAZINE);
        this.issueNumber = issueNumber;
        this.month = month;
        this.category = category;
    }

    // [Concept: Method Overriding] Shorter loan period for fresh periodicals
    @Override
    public int getLoanPeriodDays() {
        return MAGAZINE_LOAN_DAYS;
    }

    // [Concept: Method Overriding] Lower late fee for magazines
    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) return 0.0;
        return overdueDays * DAILY_FINE_RATE;
    }

    // [Concept: Method Overriding]
    @Override
    public String getDisplayDetails() {
        return String.format("Issue #%d (%s) | Category: %s", issueNumber, month, category);
    }

    @Override
    public double getDailyLateFee() {
        return DAILY_FINE_RATE;
    }

    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) return true;
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (month != null && month.toLowerCase().contains(q)) ||
               (category != null && category.toLowerCase().contains(q)) ||
               String.valueOf(issueNumber).contains(q);
    }

    public int getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(int issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
