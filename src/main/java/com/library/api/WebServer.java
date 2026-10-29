package com.library.api;

import com.library.dao.*;
import com.library.exception.LibraryException;
import com.library.service.CatalogService;
import com.library.service.LoanService;
import com.library.service.ReportService;
import com.library.thread.OverdueScannerThread;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.library.util.ApiResponse;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * Main application entry point for the Library Management System Web Server.
 * Configures Javalin HTTP server, static asset hosting, REST routing, multithreading daemons,
 * and global exception handling.
 *
 * [Concept: Overview of Java - Main Class and Entry Point]
 * [Concept: Global Exception Handler with try/catch and clean JSON responses]
 * [Concept: Multithreading - Starting Daemon Thread]
 */
public class WebServer {

    private static final Logger logger = LoggerFactory.getLogger(WebServer.class);
    public static final int DEFAULT_PORT = 7070;

    private static Javalin app;
    private static OverdueScannerThread overdueScanner;
    private static ReportService reportService;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }
        startServer(port);
    }

    public static Javalin startServer(int port) {
        logger.info("Initializing Athenaeum Library Management System...");

        // 1. Initialize SQLite database schema and realistic seed data
        DatabaseManager.initializeDatabase();

        // 2. Instantiate Data Access Objects
        BookDAO bookDAO = new BookDAO();
        MemberDAO memberDAO = new MemberDAO();
        TransactionDAO transactionDAO = new TransactionDAO();
        ReservationDAO reservationDAO = new ReservationDAO();

        // 3. Instantiate Domain Services
        CatalogService catalogService = new CatalogService(bookDAO);
        LoanService loanService = new LoanService(transactionDAO, bookDAO, memberDAO, reservationDAO);
        reportService = new ReportService(bookDAO, memberDAO, transactionDAO, reservationDAO);

        // 4. [Concept: Multithreading] Start background daemon thread scanning overdue books
        overdueScanner = new OverdueScannerThread(transactionDAO, bookDAO, 20); // scans every 20s
        overdueScanner.start();

        // 5. Instantiate API Controller
        ApiController controller = new ApiController(catalogService, loanService, memberDAO, reportService, overdueScanner);

        // 6. Configure Gson JSON Mapper for Javalin
        Gson gson = new GsonBuilder().serializeNulls().create();
        JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return gson.toJson(obj, type);
            }

            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };

        // 7. Configure and start Javalin Web Server
        app = Javalin.create(config -> {
            // Configure Gson as the JSON mapper
            config.jsonMapper(gsonMapper);

            // Serve static assets from classpath /public directory
            config.staticFiles.add("/public", Location.CLASSPATH);

            // Enable CORS for cross-origin client apps
            config.plugins.enableCors(cors -> cors.add(it -> it.anyHost()));

            // Request logging
            config.requestLogger.http((ctx, ms) -> {
                logger.debug("{} {} -> {} ({} ms)", ctx.method(), ctx.path(), ctx.status(), ms);
            });
        });

        // =====================================================================
        // [Concept: Global Exception Handler returning clean JSON error responses]
        // =====================================================================
        app.exception(LibraryException.class, (e, ctx) -> {
            logger.warn("Domain exception handled: {}", e.getMessage());
            ctx.status(400).json(ApiResponse.error(e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            logger.error("Unhandled server exception: {}", e.getMessage(), e);
            ctx.status(500).json(ApiResponse.error("Internal Server Error: " + e.getMessage()));
        });

        // =====================================================================
        // Route Registrations
        // =====================================================================

        // Auth routes
        app.post("/api/auth/login", controller::handleLogin);
        app.post("/api/auth/logout", controller::handleLogout);
        app.get("/api/auth/me", controller::handleGetCurrentUser);

        // Dashboard & Stats
        app.get("/api/stats", controller::handleGetStats);
        app.get("/api/categories", controller::handleGetCategories);

        // Catalog (Books & Materials)
        app.get("/api/books", controller::handleGetBooks);
        app.get("/api/books/{id}", controller::handleGetBookById);
        app.post("/api/books", controller::handleCreateBook);
        app.put("/api/books/{id}", controller::handleUpdateBook);
        app.put("/api/books/{id}/copies", controller::handleUpdateCopies);
        app.delete("/api/books/{id}", controller::handleDeleteBook);

        // Members
        app.get("/api/members", controller::handleGetMembers);
        app.post("/api/members", controller::handleCreateMember);
        app.put("/api/members/{id}", controller::handleUpdateMember);
        app.get("/api/members/{id}/history", controller::handleGetMemberHistory);

        // Circulation (Loans, Returns, Lost)
        app.post("/api/loans/issue", controller::handleIssueItem);
        app.post("/api/loans/return", controller::handleReturnItem);
        app.post("/api/loans/lost", controller::handleMarkLost);
        app.get("/api/loans/active", controller::handleGetActiveLoans);
        app.get("/api/loans/overdue", controller::handleGetOverdueLoans);
        app.get("/api/loans/history", controller::handleGetAllTransactions);

        // Reservations
        app.get("/api/reservations", controller::handleGetReservations);
        app.post("/api/reservations", controller::handleCreateReservation);
        app.post("/api/reservations/{id}/cancel", controller::handleCancelReservation);

        // Background Reports & Admin tasks
        app.get("/api/reports/{type}", controller::handleGetReport);
        app.post("/api/admin/scan-overdue", controller::handleScanOverdue);

        // Graceful shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down Athenaeum server...");
            if (overdueScanner != null) overdueScanner.stopScanner();
            if (reportService != null) reportService.shutdown();
            if (app != null) app.stop();
        }));

        app.start(port);
        logger.info("==================================================================");
        logger.info(" Athenaeum Library Management System is RUNNING!");
        logger.info(" Web Application URL: http://localhost:{}", port);
        logger.info(" Default Librarian : admin@library.edu  |  Password: admin123");
        logger.info(" Default Member    : rahul.sharma@student.edu  |  Password: member123");
        logger.info("==================================================================");

        return app;
    }

    public static void stopServer() {
        if (overdueScanner != null) overdueScanner.stopScanner();
        if (reportService != null) reportService.shutdown();
        if (app != null) app.stop();
    }
}
