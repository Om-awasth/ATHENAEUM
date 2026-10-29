package com.library.exception;

/**
 * Thrown when a patron attempts to borrow beyond their allowed quota.
 * [Concept: Exception Handling - Custom Exception]
 */
public class MemberLimitExceededException extends LibraryException {

    private final int memberId;
    private final int limit;

    public MemberLimitExceededException(int memberId, int limit, String message) {
        super(message);
        this.memberId = memberId;
        this.limit = limit;
    }

    public int getMemberId() {
        return memberId;
    }

    public int getLimit() {
        return limit;
    }
}
