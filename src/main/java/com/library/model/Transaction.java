package com.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Domain entity representing a circulation event (borrowing, returning, overdue monitoring).
 * [Concept: Encapsulation]
 * [Concept: Comparable Interface]
 */
public class Transaction implements Comparable<Transaction> {

    private int id;
    private int itemId;
    private String itemTitle;
    private ItemType itemType;
    private int personId;
    private String memberName;
    private String memberEmail;
    private String issueDate; // YYYY-MM-DD
    private String dueDate;   // YYYY-MM-DD
    private String returnDate; // YYYY-MM-DD or null
    private double fineAmount;
    private TransactionStatus status;

    public Transaction() {
    }

    public Transaction(int id, int itemId, String itemTitle, ItemType itemType,
                       int personId, String memberName, String memberEmail,
                       String issueDate, String dueDate, String returnDate,
                       double fineAmount, TransactionStatus status) {
        this.id = id;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.itemType = itemType;
        this.personId = personId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
    }

    /**
     * Calculates the number of days overdue based on the current date or return date.
     */
    public long getOverdueDays() {
        if (dueDate == null) return 0;
        try {
            LocalDate due = LocalDate.parse(dueDate);
            LocalDate comparisonDate = (returnDate != null) ? LocalDate.parse(returnDate) : LocalDate.now();
            long days = ChronoUnit.DAYS.between(due, comparisonDate);
            return Math.max(0, days);
        } catch (Exception e) {
            return 0;
        }
    }

    // [Concept: Comparable] Default sort orders transactions by due date (earliest first)
    @Override
    public int compareTo(Transaction other) {
        if (other == null) return 1;
        if (this.dueDate == null && other.dueDate == null) return 0;
        if (this.dueDate == null) return 1;
        if (other.dueDate == null) return -1;
        return this.dueDate.compareTo(other.dueDate);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public String getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(String issueDate) {
        this.issueDate = issueDate;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public String getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(String returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }
}
