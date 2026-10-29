package com.library.model;

/**
 * Interface defining borrowing rules and constraints for inventory items.
 * [Concept: Interface]
 * [Concept: Polymorphism via Interface Contracts]
 */
public interface Borrowable {

    /**
     * Checks if this item can be loaned out to patrons.
     * Reference books, for example, return false.
     *
     * @return true if borrowable, false otherwise
     */
    boolean isBorrowable();

    /**
     * Gets the standard loan duration in days.
     *
     * @return number of days patron can retain item
     */
    int getMaxLoanDays();

    /**
     * Gets the daily overdue penalty rate in currency units (INR).
     *
     * @return daily late fee
     */
    double getDailyLateFee();
}
