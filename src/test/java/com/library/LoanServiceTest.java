package com.library.service;

import com.library.dao.*;
import com.library.exception.BookNotAvailableException;
import com.library.exception.InvalidISBNException;
import com.library.exception.MemberLimitExceededException;
import com.library.model.*;
import com.library.util.GenericUtils;
import com.library.util.ValidationUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying core business logic, polymorphic fine calculations,
 * custom exceptions, and collections sorting.
 */
public class LoanServiceTest {

    @BeforeAll
    public static void setupDatabase() {
        DatabaseManager.setDatabaseUrl("jdbc:sqlite:test_library.db");
        DatabaseManager.initializeDatabase();
    }

    @Test
    @DisplayName("[Unit 1 & 2] Polymorphic Fine Calculation across Item Subtypes")
    public void testPolymorphicFineCalculation() {
        // Book: ₹2.0/day
        LibraryItem book = new Book(1, "Test Book", "Publisher", 2020, 2, 2, "978-0134685991", "Author", "Genre");
        assertEquals(10.0, book.calculateFine(5), 0.001);
        assertEquals(14, book.getLoanPeriodDays());
        assertTrue(book.isBorrowable());

        // Magazine: ₹1.0/day, 7 days loan
        LibraryItem magazine = new Magazine(2, "Tech Monthly", "IEEE", 2024, 2, 2, 42, "October", "Tech");
        assertEquals(5.0, magazine.calculateFine(5), 0.001);
        assertEquals(7, magazine.getLoanPeriodDays());
        assertTrue(magazine.isBorrowable());

        // Academic Journal: ₹5.0/day, 5 days loan
        LibraryItem journal = new Journal(3, "Nature Machine", "Springer", 2024, 2, 2, "Vol. 10", true, 18.5);
        assertEquals(25.0, journal.calculateFine(5), 0.001);
        assertEquals(5, journal.getLoanPeriodDays());
        assertTrue(journal.isBorrowable());

        // Reference Book: Non-circulating (cannot be checked out, 0 fine)
        LibraryItem refBook = new ReferenceBook(4, "Britannica", "Britannica Inc", 2022, 1, 1, "General", "15th", "031");
        assertEquals(0.0, refBook.calculateFine(5), 0.001);
        assertEquals(0, refBook.getLoanPeriodDays());
        assertFalse(refBook.isBorrowable());
    }

    @Test
    @DisplayName("[Unit 2] Defensive Input Validation throws InvalidISBNException")
    public void testInvalidIsbnThrowsException() {
        // Invalid check-digit or bad length
        assertThrows(InvalidISBNException.class, () -> ValidationUtil.validateISBN("123-456-789"));
        assertThrows(InvalidISBNException.class, () -> ValidationUtil.validateISBN("INVALID-ISBN-123"));

        // Valid ISBN-10 and ISBN-13 must pass without throwing
        assertDoesNotThrow(() -> ValidationUtil.validateISBN("0-201-63361-2")); // Design Patterns ISBN-10
        assertDoesNotThrow(() -> ValidationUtil.validateISBN("978-0134685991")); // Effective Java ISBN-13
    }

    @Test
    @DisplayName("[Unit 4] Collections Framework - PriorityQueue ordering by fine amount")
    public void testPriorityQueueOverdueOrdering() {
        PriorityQueue<Transaction> pq = new PriorityQueue<>(
                (t1, t2) -> Double.compare(t2.getFineAmount(), t1.getFineAmount())
        );

        Transaction t1 = new Transaction(1, 1, "Book A", ItemType.BOOK, 1, "Member 1", "m1@test.com", "2026-10-01", "2026-10-15", null, 14.0, TransactionStatus.OVERDUE);
        Transaction t2 = new Transaction(2, 2, "Book B", ItemType.BOOK, 2, "Member 2", "m2@test.com", "2026-10-01", "2026-10-15", null, 48.0, TransactionStatus.OVERDUE);
        Transaction t3 = new Transaction(3, 3, "Book C", ItemType.BOOK, 3, "Member 3", "m3@test.com", "2026-10-01", "2026-10-15", null, 26.0, TransactionStatus.OVERDUE);

        pq.add(t1);
        pq.add(t2);
        pq.add(t3);

        // Highest fine (₹48) must poll first
        assertEquals(48.0, pq.poll().getFineAmount());
        // Second highest (₹26)
        assertEquals(26.0, pq.poll().getFineAmount());
        // Lowest fine (₹14)
        assertEquals(14.0, pq.poll().getFineAmount());
    }

    @Test
    @DisplayName("[Unit 5] Generics - Bounded Wildcard filterAvailable()")
    public void testBoundedGenericsFilterAvailable() {
        List<Book> books = new ArrayList<>();
        books.add(new Book(1, "Available 1", "P1", 2020, 2, 1, "978-0134685991", "A1", "G1"));
        books.add(new Book(2, "Exhausted 2", "P2", 2021, 2, 0, "978-0132350884", "A2", "G2"));
        books.add(new Book(3, "Available 3", "P3", 2022, 3, 2, "978-0201633610", "A3", "G3"));

        List<Book> available = GenericUtils.filterAvailable(books);
        assertEquals(2, available.size());
        assertEquals("Available 1", available.get(0).getTitle());
        assertEquals("Available 3", available.get(1).getTitle());
    }
}
