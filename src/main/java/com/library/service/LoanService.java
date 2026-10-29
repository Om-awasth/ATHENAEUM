package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.MemberDAO;
import com.library.dao.ReservationDAO;
import com.library.dao.TransactionDAO;
import com.library.exception.BookNotAvailableException;
import com.library.exception.ItemNotFoundException;
import com.library.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service orchestrating circulation transactions (lending, returns, fines, lost items, hold queues).
 * Demonstrates thread concurrency guards, FIFO Queues, PriorityQueues, and atomic counters.
 *
 * [Concept: Multithreading - ReentrantLock Concurrency Guard]
 * [Concept: Thread-Safety - AtomicInteger]
 * [Concept: Collections Framework - PriorityQueue]
 * [Concept: Collections Framework - Queue / LinkedList]
 */
public class LoanService {

    private static final Logger logger = LoggerFactory.getLogger(LoanService.class);

    private final TransactionDAO transactionDAO;
    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final ReservationDAO reservationDAO;

    // [Concept: Concurrency Control - ReentrantLock] Fair lock preventing race conditions when two users issue the last copy simultaneously
    private final ReentrantLock circulationLock = new ReentrantLock(true);

    // [Concept: Thread-Safety - AtomicInteger for live active loans count]
    private final AtomicInteger activeLoansCounter = new AtomicInteger(0);

    // [Concept: Collections Framework - LinkedList acting as FIFO Queue for reservations]
    private final Queue<Reservation> inMemoryReservationQueue = new LinkedList<>();

    public LoanService(TransactionDAO transactionDAO, BookDAO bookDAO, MemberDAO memberDAO, ReservationDAO reservationDAO) {
        this.transactionDAO = Objects.requireNonNull(transactionDAO, "transactionDAO cannot be null");
        this.bookDAO = Objects.requireNonNull(bookDAO, "bookDAO cannot be null");
        this.memberDAO = Objects.requireNonNull(memberDAO, "memberDAO cannot be null");
        this.reservationDAO = Objects.requireNonNull(reservationDAO, "reservationDAO cannot be null");
        refreshActiveCount();
    }

    private void refreshActiveCount() {
        int count = transactionDAO.findActiveLoans().size();
        activeLoansCounter.set(count);
    }

    /**
     * [Concept: Concurrency Control with ReentrantLock]
     * Issues an item to a member with lock acquisition to prevent race conditions.
     */
    public Transaction issueItem(int itemId, int memberId, Integer requestedLoanDays) {
        circulationLock.lock(); // [Concept: Multithreading - Lock Acquisition]
        try {
            LibraryItem item = bookDAO.findById(itemId)
                    .orElseThrow(() -> new ItemNotFoundException("Item #" + itemId, "Catalog item does not exist"));

            if (!item.isBorrowable()) {
                throw new BookNotAvailableException(itemId, "This material (" + item.getItemType().getDisplayName() + ") is strictly non-circulating.");
            }

            if (item.getAvailableCopies() <= 0) {
                throw new BookNotAvailableException(itemId, "All copies of '" + item.getTitle() + "' are currently issued. Please reserve a copy.");
            }

            int loanDays = (requestedLoanDays != null && requestedLoanDays > 0)
                    ? requestedLoanDays
                    : item.getLoanPeriodDays();

            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(loanDays);

            Transaction tx = transactionDAO.issueItem(itemId, memberId, issueDate, dueDate);
            activeLoansCounter.incrementAndGet(); // [Concept: AtomicInteger update]
            return tx;
        } finally {
            circulationLock.unlock(); // [Concept: Multithreading - Lock Release in finally]
        }
    }

    /**
     * [Concept: Concurrency Control with ReentrantLock]
     * Returns an issued item, computes late fees using the item's polymorphic fine rule.
     */
    public boolean returnItem(int transactionId) {
        circulationLock.lock();
        try {
            Transaction tx = transactionDAO.findById(transactionId)
                    .orElseThrow(() -> new ItemNotFoundException("Tx #" + transactionId, "Transaction not found"));

            LocalDate returnDate = LocalDate.now();
            long overdueDays = tx.getOverdueDays();
            double fine = 0.0;

            if (overdueDays > 0) {
                LibraryItem item = bookDAO.findById(tx.getItemId()).orElse(null);
                if (item != null) {
                    // [Concept: Polymorphism - calculateFine called on specific item type]
                    fine = item.calculateFine(overdueDays);
                }
            }

            boolean ok = transactionDAO.returnItem(transactionId, returnDate, fine);
            if (ok) {
                activeLoansCounter.decrementAndGet();
                checkAndFulfillReservation(tx.getItemId());
            }
            return ok;
        } finally {
            circulationLock.unlock();
        }
    }

    /**
     * Marks an item lost and decrements total count.
     */
    public boolean markLost(int transactionId) {
        circulationLock.lock();
        try {
            boolean ok = transactionDAO.markLost(transactionId);
            if (ok) {
                activeLoansCounter.decrementAndGet();
            }
            return ok;
        } finally {
            circulationLock.unlock();
        }
    }

    /**
     * [Concept: Collections Framework - PriorityQueue]
     * Returns active and overdue loans prioritized by fine amount (highest fine first).
     */
    public List<Transaction> getOverdueLoansPrioritizedByFine() {
        List<Transaction> overdueLoans = transactionDAO.findOverdueLoans();

        // [Concept: PriorityQueue ordering transactions by fine descending]
        PriorityQueue<Transaction> pq = new PriorityQueue<>(
                (t1, t2) -> Double.compare(t2.getFineAmount(), t1.getFineAmount())
        );

        pq.addAll(overdueLoans);

        List<Transaction> sorted = new ArrayList<>();
        while (!pq.isEmpty()) {
            sorted.add(pq.poll());
        }
        return sorted;
    }

    /**
     * [Concept: Collections Framework - Queue / LinkedList]
     * Places a patron on the reservation hold queue when no copies are in stock.
     */
    public Reservation reserveItem(int itemId, int memberId) {
        Reservation reservation = reservationDAO.addReservation(itemId, memberId);
        inMemoryReservationQueue.offer(reservation); // [Concept: Queue offer operation]
        return reservation;
    }

    private void checkAndFulfillReservation(int itemId) {
        List<Reservation> waiting = reservationDAO.findByItem(itemId);
        if (!waiting.isEmpty()) {
            Reservation next = waiting.get(0);
            reservationDAO.updateStatus(next.getId(), "NOTIFIED");
            logger.info("Reservation hold triggered for member #{} for item #{}", next.getPersonId(), itemId);
        }
    }

    public List<Transaction> getActiveLoans() {
        return transactionDAO.findActiveLoans();
    }

    public List<Transaction> getAllTransactions() {
        return transactionDAO.findAll();
    }

    public List<Transaction> getMemberHistory(int memberId) {
        return transactionDAO.findMemberHistory(memberId);
    }

    public List<Reservation> getAllReservations() {
        return reservationDAO.findAll();
    }

    public boolean cancelReservation(int reservationId) {
        return reservationDAO.updateStatus(reservationId, "CANCELLED");
    }

    public int getActiveLoansCount() {
        return activeLoansCounter.get();
    }
}
