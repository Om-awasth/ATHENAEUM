package com.library.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.library.dao.MemberDAO;
import com.library.exception.InvalidISBNException;
import com.library.exception.LibraryException;
import com.library.model.*;
import com.library.service.CatalogService;
import com.library.service.LoanService;
import com.library.service.ReportService;
import com.library.thread.OverdueScannerThread;
import com.library.util.ApiResponse;
import com.library.util.PasswordUtil;
import com.library.util.ValidationUtil;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API controller handling incoming HTTP requests, JSON serialization, and response dispatch.
 * [Concept: REST API Architecture with JSON]
 * [Concept: Exception Handling - Error Responses]
 */
public class ApiController {

    private static final Logger logger = LoggerFactory.getLogger(ApiController.class);
    private final Gson gson = new GsonBuilder().serializeNulls().create();

    private final CatalogService catalogService;
    private final LoanService loanService;
    private final MemberDAO memberDAO;
    private final ReportService reportService;
    private final OverdueScannerThread overdueScanner;

    public ApiController(CatalogService catalogService, LoanService loanService, MemberDAO memberDAO,
                         ReportService reportService, OverdueScannerThread overdueScanner) {
        this.catalogService = catalogService;
        this.loanService = loanService;
        this.memberDAO = memberDAO;
        this.reportService = reportService;
        this.overdueScanner = overdueScanner;
    }

    // =========================================================================
    // Auth Endpoints
    // =========================================================================

    public void handleLogin(Context ctx) {
        reportService.recordVisitorHit();
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String email = body.has("email") ? body.get("email").getAsString() : "";
        String password = body.has("password") ? body.get("password").getAsString() : "";

        Optional<Person> personOpt = memberDAO.authenticate(email, password);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            // Store session
            ctx.sessionAttribute("currentUser", person);

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", person.getId());
            userData.put("name", person.getName());
            userData.put("email", person.getEmail());
            userData.put("role", person.getRole().name());
            userData.put("phone", person.getPhone());
            if (person instanceof Member m) {
                userData.put("membershipId", m.getMembershipId());
                userData.put("maxBorrowLimit", m.getMaxBorrowLimit());
                userData.put("activeBorrowCount", m.getActiveBorrowCount());
            } else if (person instanceof Librarian l) {
                userData.put("employeeId", l.getEmployeeId());
                userData.put("department", l.getDepartment());
            }

            ctx.json(ApiResponse.ok("Login successful", userData));
        } else {
            ctx.status(401).json(ApiResponse.error("Invalid email or password. Please verify credentials."));
        }
    }

    public void handleLogout(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.json(ApiResponse.ok("Logged out successfully", null));
    }

    public void handleGetCurrentUser(Context ctx) {
        Person person = ctx.sessionAttribute("currentUser");
        if (person != null) {
            // refresh data from DB
            Person refreshed = memberDAO.findById(person.getId()).orElse(person);
            Map<String, Object> userData = new HashMap<>();
            userData.put("id", refreshed.getId());
            userData.put("name", refreshed.getName());
            userData.put("email", refreshed.getEmail());
            userData.put("role", refreshed.getRole().name());
            userData.put("phone", refreshed.getPhone());
            ctx.json(ApiResponse.ok("User session active", userData));
        } else {
            ctx.status(401).json(ApiResponse.error("Not authenticated"));
        }
    }

    // =========================================================================
    // Dashboard Stats Endpoint
    // =========================================================================

    public void handleGetStats(Context ctx) {
        reportService.recordVisitorHit();
        Map<String, Object> stats = reportService.getDashboardStatistics();
        ctx.json(ApiResponse.ok("Statistics retrieved", stats));
    }

    // =========================================================================
    // Catalog (Books & Items) Endpoints
    // =========================================================================

    public void handleGetBooks(Context ctx) {
        reportService.recordVisitorHit();
        String query = ctx.queryParam("q");
        String category = ctx.queryParam("category");
        boolean availableOnly = Boolean.parseBoolean(ctx.queryParam("available"));
        String sort = ctx.queryParam("sort");

        List<LibraryItem> items;
        if ((query != null && !query.isBlank()) || (category != null && !category.isBlank()) || availableOnly) {
            items = catalogService.searchCatalog(query, category, availableOnly);
        } else {
            items = catalogService.getAllItems(sort);
        }

        ctx.json(ApiResponse.ok("Catalog retrieved", items));
    }

    public void handleGetBookById(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        LibraryItem item = catalogService.getItemById(id)
                .orElseThrow(() -> new LibraryException("Catalog item #" + id + " not found"));
        ctx.json(ApiResponse.ok("Item found", item));
    }

    public void handleCreateBook(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String typeStr = body.has("itemType") ? body.get("itemType").getAsString() : "BOOK";
        ItemType type = ItemType.valueOf(typeStr.toUpperCase());

        String title = body.get("title").getAsString();
        String publisher = body.get("publisher").getAsString();
        int year = body.get("year").getAsInt();
        int totalCopies = body.has("totalCopies") ? body.get("totalCopies").getAsInt() : 1;

        LibraryItem item;
        switch (type) {
            case BOOK -> {
                String isbn = body.has("isbn") ? body.get("isbn").getAsString() : "";
                String author = body.has("author") ? body.get("author").getAsString() : "";
                String genre = body.has("genre") ? body.get("genre").getAsString() : "General";

                // Validate ISBN
                ValidationUtil.validateISBN(isbn);
                item = new Book(0, title, publisher, year, totalCopies, totalCopies, isbn, author, genre);
            }
            case MAGAZINE -> {
                int issueNumber = body.has("issueNumber") ? body.get("issueNumber").getAsInt() : 1;
                String month = body.has("month") ? body.get("month").getAsString() : "Current";
                String category = body.has("category") ? body.get("category").getAsString() : "General";
                item = new Magazine(0, title, publisher, year, totalCopies, totalCopies, issueNumber, month, category);
            }
            case REFERENCE -> {
                String fieldOfStudy = body.has("fieldOfStudy") ? body.get("fieldOfStudy").getAsString() : "General";
                String edition = body.has("edition") ? body.get("edition").getAsString() : "1st Edition";
                String dewey = body.has("deweyDecimal") ? body.get("deweyDecimal").getAsString() : "000";
                item = new ReferenceBook(0, title, publisher, year, totalCopies, totalCopies, fieldOfStudy, edition, dewey);
            }
            case JOURNAL -> {
                String volume = body.has("volume") ? body.get("volume").getAsString() : "Vol. 1";
                boolean peerReviewed = !body.has("peerReviewed") || body.get("peerReviewed").getAsBoolean();
                double impactFactor = body.has("impactFactor") ? body.get("impactFactor").getAsDouble() : 1.0;
                item = new Journal(0, title, publisher, year, totalCopies, totalCopies, volume, peerReviewed, impactFactor);
            }
            default -> throw new LibraryException("Unsupported item type: " + typeStr);
        }

        LibraryItem created = catalogService.addItem(item);
        ctx.status(201).json(ApiResponse.ok("Item added successfully to catalog", created));
    }

    public void handleUpdateBook(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        LibraryItem existing = catalogService.getItemById(id)
                .orElseThrow(() -> new LibraryException("Item #" + id + " not found"));

        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (body.has("title")) existing.setTitle(body.get("title").getAsString());
        if (body.has("publisher")) existing.setPublisher(body.get("publisher").getAsString());
        if (body.has("year")) existing.setYear(body.get("year").getAsInt());
        if (body.has("totalCopies")) {
            int newTotal = body.get("totalCopies").getAsInt();
            int diff = newTotal - existing.getTotalCopies();
            existing.setTotalCopies(newTotal);
            existing.setAvailableCopies(Math.max(0, existing.getAvailableCopies() + diff));
        }

        if (existing instanceof Book book) {
            if (body.has("author")) book.setAuthor(body.get("author").getAsString());
            if (body.has("genre")) book.setGenre(body.get("genre").getAsString());
            if (body.has("isbn")) {
                String newIsbn = body.get("isbn").getAsString();
                ValidationUtil.validateISBN(newIsbn);
                book.setIsbn(newIsbn);
            }
        }

        boolean updated = catalogService.updateItem(existing);
        if (updated) {
            ctx.json(ApiResponse.ok("Item updated successfully", existing));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed to update item"));
        }
    }

    public void handleUpdateCopies(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        int total = body.get("totalCopies").getAsInt();
        int available = body.get("availableCopies").getAsInt();

        boolean ok = catalogService.updateCopies(id, total, available);
        if (ok) {
            ctx.json(ApiResponse.ok("Copy counts updated", null));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed to update copy count"));
        }
    }

    public void handleDeleteBook(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        boolean deleted = catalogService.deleteItem(id);
        if (deleted) {
            ctx.json(ApiResponse.ok("Item #" + id + " deleted successfully", null));
        } else {
            ctx.status(404).json(ApiResponse.error("Item #" + id + " not found or cannot be deleted"));
        }
    }

    public void handleGetCategories(Context ctx) {
        ctx.json(ApiResponse.ok("Categories retrieved", catalogService.getUniqueCategories()));
    }

    // =========================================================================
    // Member Management Endpoints
    // =========================================================================

    public void handleGetMembers(Context ctx) {
        List<Member> members = memberDAO.findAllMembers();
        ctx.json(ApiResponse.ok("Members retrieved", members));
    }

    public void handleCreateMember(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        String name = body.get("name").getAsString();
        String email = body.get("email").getAsString();
        String phone = body.get("phone").getAsString();
        String password = body.has("password") ? body.get("password").getAsString() : "member123";
        int maxLimit = body.has("maxBorrowLimit") ? body.get("maxBorrowLimit").getAsInt() : 3;

        if (!ValidationUtil.isValidEmail(email)) {
            throw new LibraryException("Invalid email format: " + email);
        }

        String membershipId = "MEM-" + (1000 + (int)(Math.random() * 9000));
        String passHash = PasswordUtil.hash(password);

        Member member = new Member(name, email, phone, passHash, membershipId, maxLimit);
        Person saved = memberDAO.save(member);
        ctx.status(201).json(ApiResponse.ok("Member created successfully", saved));
    }

    public void handleUpdateMember(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        Person existing = memberDAO.findById(id)
                .orElseThrow(() -> new LibraryException("Member #" + id + " not found"));

        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        if (body.has("name")) existing.setName(body.get("name").getAsString());
        if (body.has("email")) {
            String newEmail = body.get("email").getAsString();
            if (!ValidationUtil.isValidEmail(newEmail)) throw new LibraryException("Invalid email: " + newEmail);
            existing.setEmail(newEmail);
        }
        if (body.has("phone")) existing.setPhone(body.get("phone").getAsString());
        if (existing instanceof Member m && body.has("maxBorrowLimit")) {
            m.setMaxBorrowLimit(body.get("maxBorrowLimit").getAsInt());
        }

        boolean ok = memberDAO.update(existing);
        if (ok) {
            ctx.json(ApiResponse.ok("Member updated successfully", existing));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed to update member"));
        }
    }

    public void handleGetMemberHistory(Context ctx) {
        int memberId = Integer.parseInt(ctx.pathParam("id"));
        List<Transaction> history = loanService.getMemberHistory(memberId);
        ctx.json(ApiResponse.ok("Member history retrieved", history));
    }

    // =========================================================================
    // Circulation (Issue, Return, Lost) Endpoints
    // =========================================================================

    public void handleIssueItem(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        int itemId = body.get("itemId").getAsInt();
        int memberId = body.get("memberId").getAsInt();
        Integer days = body.has("days") ? body.get("days").getAsInt() : null;

        Transaction tx = loanService.issueItem(itemId, memberId, days);
        ctx.status(201).json(ApiResponse.ok("Book issued successfully to " + tx.getMemberName(), tx));
    }

    public void handleReturnItem(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        int txId = body.get("transactionId").getAsInt();

        boolean ok = loanService.returnItem(txId);
        if (ok) {
            ctx.json(ApiResponse.ok("Book returned successfully. Inventory updated.", null));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed returning book"));
        }
    }

    public void handleMarkLost(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        int txId = body.get("transactionId").getAsInt();

        boolean ok = loanService.markLost(txId);
        if (ok) {
            ctx.json(ApiResponse.ok("Item marked as LOST. Member loan count adjusted.", null));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed marking item as lost"));
        }
    }

    public void handleGetActiveLoans(Context ctx) {
        List<Transaction> loans = loanService.getActiveLoans();
        ctx.json(ApiResponse.ok("Active loans retrieved", loans));
    }

    public void handleGetOverdueLoans(Context ctx) {
        List<Transaction> overdue = loanService.getOverdueLoansPrioritizedByFine();
        ctx.json(ApiResponse.ok("Overdue loans retrieved (prioritized by fine)", overdue));
    }

    public void handleGetAllTransactions(Context ctx) {
        List<Transaction> list = loanService.getAllTransactions();
        ctx.json(ApiResponse.ok("All transactions retrieved", list));
    }

    // =========================================================================
    // Reservations Endpoints
    // =========================================================================

    public void handleGetReservations(Context ctx) {
        List<Reservation> list = loanService.getAllReservations();
        ctx.json(ApiResponse.ok("Reservations retrieved", list));
    }

    public void handleCreateReservation(Context ctx) {
        JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
        int itemId = body.get("itemId").getAsInt();
        int memberId = body.get("memberId").getAsInt();

        Reservation res = loanService.reserveItem(itemId, memberId);
        ctx.status(201).json(ApiResponse.ok("Reservation hold placed successfully at queue position #" + res.getQueuePosition(), res));
    }

    public void handleCancelReservation(Context ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        boolean ok = loanService.cancelReservation(id);
        if (ok) {
            ctx.json(ApiResponse.ok("Reservation #" + id + " cancelled", null));
        } else {
            ctx.status(400).json(ApiResponse.error("Failed to cancel reservation"));
        }
    }

    // =========================================================================
    // Reports and Background Tasks Endpoints
    // =========================================================================

    public void handleGetReport(Context ctx) {
        String type = ctx.pathParam("type");
        // [Concept: Multithreading - CompletableFuture with Javalin async response]
        ctx.future(() -> reportService.generateReportAsync(type).thenAccept(report -> {
            ctx.json(ApiResponse.ok("Report generated asynchronously", report));
        }));
    }

    public void handleScanOverdue(Context ctx) {
        overdueScanner.scanAndUpdateOverdueLoans();
        ctx.json(ApiResponse.ok("Overdue scan triggered manually. Fines recalculated.", null));
    }
}
