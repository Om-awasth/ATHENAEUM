package com.library.thread;

import com.library.dao.BookDAO;
import com.library.dao.TransactionDAO;
import com.library.model.LibraryItem;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Background daemon thread periodically scanning for overdue loans and updating penalty amounts.
 * [Concept: Multithreading - Background Daemon Thread]
 * [Concept: Exception Handling - InterruptedException]
 * [Concept: Polymorphism - Dynamic Fine Calculation]
 */
public class OverdueScannerThread extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(OverdueScannerThread.class);

    private final TransactionDAO transactionDAO;
    private final BookDAO bookDAO;
    private final long intervalMillis;
    private volatile boolean running = true;

    /**
     * Constructs the daemon thread.
     *
     * @param transactionDAO transaction data access
     * @param bookDAO book data access
     * @param intervalSeconds scan frequency in seconds (e.g., 30s)
     */
    public OverdueScannerThread(TransactionDAO transactionDAO, BookDAO bookDAO, int intervalSeconds) {
        super("Overdue-Scanner-Daemon");
        this.transactionDAO = transactionDAO;
        this.bookDAO = bookDAO;
        this.intervalMillis = intervalSeconds * 1000L;
        // [Concept: Daemon Thread] Does not block JVM from shutting down
        this.setDaemon(true);
    }

    @Override
    public void run() {
        logger.info("OverdueScannerThread daemon started. Scanning frequency: {}s", intervalMillis / 1000);

        while (running) {
            try {
                scanAndUpdateOverdueLoans();
                // [Concept: Thread Sleep] Sleep until next periodic scan
                Thread.sleep(intervalMillis);
            } catch (InterruptedException e) {
                logger.info("OverdueScannerThread interrupted. Terminating loop.");
                break;
            } catch (Exception e) {
                logger.error("Unexpected error in OverdueScannerThread scan iteration", e);
            }
        }

        logger.info("OverdueScannerThread terminated cleanly.");
    }

    /**
     * Core scan logic: locates active loans past their due date, computes fines polymorphically, and updates records.
     */
    public void scanAndUpdateOverdueLoans() {
        List<Transaction> activeLoans = transactionDAO.findActiveLoans();
        LocalDate today = LocalDate.now();
        int updatedCount = 0;

        for (Transaction tx : activeLoans) {
            if (tx.getDueDate() == null) continue;

            try {
                LocalDate due = LocalDate.parse(tx.getDueDate());
                if (today.isAfter(due)) {
                    long overdueDays = ChronoUnit.DAYS.between(due, today);
                    if (overdueDays > 0) {
                        LibraryItem item = bookDAO.findById(tx.getItemId()).orElse(null);
                        double fine = 0.0;
                        if (item != null) {
                            // [Concept: Polymorphism] Subclass calculateFine invoked polymorphically
                            fine = item.calculateFine(overdueDays);
                        } else {
                            fine = overdueDays * 2.0; // fallback standard rate
                        }

                        // Update in database if status or fine changed
                        if (tx.getStatus() != TransactionStatus.OVERDUE || tx.getFineAmount() != fine) {
                            transactionDAO.updateFineAndStatus(tx.getId(), fine, TransactionStatus.OVERDUE);
                            updatedCount++;
                            logger.warn("Overdue Alert: Loan #{} (Book: '{}', Member: '{}') is {} days late. Accrued Fine: ₹{}",
                                    tx.getId(), tx.getItemTitle(), tx.getMemberName(), overdueDays, fine);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Error evaluating overdue status for transaction #{}", tx.getId(), e);
            }
        }

        if (updatedCount > 0) {
            logger.info("Overdue scanner cycle completed: Updated {} overdue loans.", updatedCount);
        }
    }

    public void stopScanner() {
        this.running = false;
        this.interrupt();
    }
}
