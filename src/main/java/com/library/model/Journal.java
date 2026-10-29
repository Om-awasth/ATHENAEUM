package com.library.model;

/**
 * Concrete subclass representing academic, peer-reviewed journals.
 * [Concept: Inheritance - Subclass of LibraryItem]
 * [Concept: Method Overriding]
 */
public class Journal extends LibraryItem {

    public static final int JOURNAL_LOAN_DAYS = 5;
    public static final double DAILY_FINE_RATE = 5.0; // ₹5.00 per day (higher penalty for academic journals)

    private String volume;
    private boolean peerReviewed;
    private double impactFactor;

    public Journal(int id, String title, String publisher, int year,
                   int totalCopies, int availableCopies,
                   String volume, boolean peerReviewed, double impactFactor) {
        super(id, title, publisher, year, totalCopies, availableCopies, ItemType.JOURNAL);
        this.volume = volume;
        this.peerReviewed = peerReviewed;
        this.impactFactor = impactFactor;
    }

    // [Concept: Method Overriding] Short loan period due to high demand among researchers
    @Override
    public int getLoanPeriodDays() {
        return JOURNAL_LOAN_DAYS;
    }

    // [Concept: Method Overriding] Higher overdue penalty for specialized journals
    @Override
    public double calculateFine(long overdueDays) {
        if (overdueDays <= 0) return 0.0;
        return overdueDays * DAILY_FINE_RATE;
    }

    @Override
    public double getDailyLateFee() {
        return DAILY_FINE_RATE;
    }

    // [Concept: Method Overriding]
    @Override
    public String getDisplayDetails() {
        return String.format("%s | Peer-Reviewed: %s | Impact Factor: %.2f",
                volume, peerReviewed ? "Yes" : "No", impactFactor);
    }

    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) return true;
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (volume != null && volume.toLowerCase().contains(q));
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public boolean isPeerReviewed() {
        return peerReviewed;
    }

    public void setPeerReviewed(boolean peerReviewed) {
        this.peerReviewed = peerReviewed;
    }

    public double getImpactFactor() {
        return impactFactor;
    }

    public void setImpactFactor(double impactFactor) {
        this.impactFactor = impactFactor;
    }
}
