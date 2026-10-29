package com.library.service;

import com.library.dao.BookDAO;
import com.library.dao.MemberDAO;
import com.library.dao.ReservationDAO;
import com.library.dao.TransactionDAO;
import com.library.model.LibraryItem;
import com.library.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Service generating heavy operational reports and live metrics asynchronously using thread pools.
 * [Concept: Multithreading - ExecutorService Thread Pool]
 * [Concept: Thread-Safety - ConcurrentHashMap]
 * [Concept: Thread-Safety - AtomicInteger]
 * [Concept: Collections Framework - Streams and Collectors]
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final TransactionDAO transactionDAO;
    private final ReservationDAO reservationDAO;

    // [Concept: Multithreading - Fixed Thread Pool for non-blocking report generation]
    private final ExecutorService reportExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "Report-Worker-" + threadNumber.getAndIncrement());
            t.setDaemon(true);
            return t;
        }
    });

    // [Concept: Thread-Safety - ConcurrentHashMap for thread-safe metrics cache]
    private final ConcurrentHashMap<String, Object> liveStatsCache = new ConcurrentHashMap<>();

    // [Concept: Thread-Safety - AtomicInteger for thread-safe counter]
    private final AtomicInteger visitorCounter = new AtomicInteger(0);

    public ReportService(BookDAO bookDAO, MemberDAO memberDAO, TransactionDAO transactionDAO, ReservationDAO reservationDAO) {
        this.bookDAO = Objects.requireNonNull(bookDAO, "bookDAO cannot be null");
        this.memberDAO = Objects.requireNonNull(memberDAO, "memberDAO cannot be null");
        this.transactionDAO = Objects.requireNonNull(transactionDAO, "transactionDAO cannot be null");
        this.reservationDAO = Objects.requireNonNull(reservationDAO, "reservationDAO cannot be null");
    }

    /**
     * Increments visitor count on web/API interactions in a thread-safe manner.
     */
    public int recordVisitorHit() {
        return visitorCounter.incrementAndGet();
    }

    /**
     * [Concept: Multithreading - Asynchronous Report Execution with CompletableFuture and ExecutorService]
     * Generates a report in the background without blocking the HTTP request thread or JavaFX UI.
     */
    public CompletableFuture<Map<String, Object>> generateReportAsync(String reportType) {
        return CompletableFuture.supplyAsync(() -> {
            logger.info("Executing background report '{}' on thread: {}", reportType, Thread.currentThread().getName());
            Map<String, Object> report = new HashMap<>();
            report.put("reportType", reportType);
            report.put("generatedAt", System.currentTimeMillis());
            report.put("generatedByThread", Thread.currentThread().getName());

            switch (reportType.toLowerCase()) {
                case "most-borrowed" -> {
                    List<Transaction> transactions = transactionDAO.findAll();
                    // [Concept: Streams - Grouping by item title and counting]
                    Map<String, Long> frequency = transactions.stream()
                            .collect(Collectors.groupingBy(Transaction::getItemTitle, Collectors.counting()));

                    List<Map<String, Object>> topItems = frequency.entrySet().stream()
                            .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                            .limit(10)
                            .map(e -> {
                                Map<String, Object> row = new HashMap<>();
                                row.put("title", e.getKey());
                                row.put("borrowCount", e.getValue());
                                return row;
                            })
                            .collect(Collectors.toList());

                    report.put("data", topItems);
                }
                case "overdue-audit" -> {
                    List<Transaction> overdue = transactionDAO.findOverdueLoans();
                    double totalFine = overdue.stream().mapToDouble(Transaction::getFineAmount).sum();
                    report.put("overdueCount", overdue.size());
                    report.put("totalOutstandingFines", totalFine);
                    report.put("data", overdue);
                }
                default -> {
                    report.put("data", getDashboardStatistics());
                }
            }

            return report;
        }, reportExecutor);
    }

    /**
     * Aggregates live system statistics for the dashboard.
     */
    public Map<String, Object> getDashboardStatistics() {
        List<LibraryItem> items = bookDAO.findAll();
        int totalBooks = items.size();
        int availableCopies = items.stream().mapToInt(LibraryItem::getAvailableCopies).sum();
        int totalCopies = items.stream().mapToInt(LibraryItem::getTotalCopies).sum();

        List<Transaction> activeLoans = transactionDAO.findActiveLoans();
        int issuedCount = activeLoans.size();

        List<Transaction> overdueLoans = transactionDAO.findOverdueLoans();
        int overdueCount = overdueLoans.size();

        int memberCount = memberDAO.findAllMembers().size();
        int reservationCount = reservationDAO.findAll().size();

        // [Concept: Streams - Grouping inventory by Genre/Type]
        Map<String, Long> categoryDistribution = items.stream()
                .collect(Collectors.groupingBy(
                        item -> item.getItemType().getDisplayName(),
                        Collectors.counting()
                ));

        // Store into thread-safe ConcurrentHashMap
        liveStatsCache.put("totalBooks", totalBooks);
        liveStatsCache.put("availableCopies", availableCopies);
        liveStatsCache.put("totalCopies", totalCopies);
        liveStatsCache.put("issuedCount", issuedCount);
        liveStatsCache.put("overdueCount", overdueCount);
        liveStatsCache.put("memberCount", memberCount);
        liveStatsCache.put("reservationCount", reservationCount);
        liveStatsCache.put("visitorHits", visitorCounter.get());
        liveStatsCache.put("categoryDistribution", categoryDistribution);

        return new HashMap<>(liveStatsCache);
    }

    public void shutdown() {
        reportExecutor.shutdown();
    }
}
