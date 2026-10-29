package com.library.model;

/**
 * Concrete subclass representing library patrons (students, faculty, public readers).
 * [Concept: Inheritance - Subclass of Person]
 * [Concept: super keyword]
 * [Concept: Method Overriding]
 */
public class Member extends Person {

    private String membershipId;
    private int maxBorrowLimit;
    private int activeBorrowCount;
    private String joinDate;

    public Member(int id, String name, String email, String phone, String passwordHash,
                  String membershipId, int maxBorrowLimit, int activeBorrowCount, String joinDate) {
        // [Concept: super constructor invocation]
        super(id, name, email, phone, Role.MEMBER, passwordHash);
        this.membershipId = membershipId;
        this.maxBorrowLimit = Math.max(1, maxBorrowLimit);
        this.activeBorrowCount = Math.max(0, activeBorrowCount);
        this.joinDate = joinDate;
    }

    public Member(String name, String email, String phone, String passwordHash,
                  String membershipId, int maxBorrowLimit) {
        this(0, name, email, phone, passwordHash, membershipId, maxBorrowLimit, 0, null);
    }

    // [Concept: Method Overriding]
    @Override
    public String getProfileSummary() {
        return String.format("Member ID: %s | Active Loans: %d/%d | Joined: %s",
                membershipId, activeBorrowCount, maxBorrowLimit, joinDate);
    }

    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) return true;
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return membershipId != null && membershipId.toLowerCase().contains(q);
    }

    public boolean canBorrow() {
        return activeBorrowCount < maxBorrowLimit;
    }

    public String getMembershipId() {
        return membershipId;
    }

    public void setMembershipId(String membershipId) {
        this.membershipId = membershipId;
    }

    public int getMaxBorrowLimit() {
        return maxBorrowLimit;
    }

    public void setMaxBorrowLimit(int maxBorrowLimit) {
        this.maxBorrowLimit = maxBorrowLimit;
    }

    public int getActiveBorrowCount() {
        return activeBorrowCount;
    }

    public void setActiveBorrowCount(int activeBorrowCount) {
        this.activeBorrowCount = activeBorrowCount;
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate;
    }
}
