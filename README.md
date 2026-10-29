# 🏛 Athenaeum — Library Management System

A comprehensive, academic **Library Management System** engineered as a college mini-project. Built strictly with standard Java 17, plain JDBC, SQLite, Javalin REST API, vanilla HTML5/CSS3/JavaScript, and a secondary JavaFX desktop admin client.

---

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Tech Stack](#tech-stack)
3. [Full Project Structure](#full-project-structure)
4. [Syllabus Concepts Coverage Matrix](#syllabus-concepts-coverage-matrix)
5. [Step-by-Step Run Instructions](#step-by-step-run-instructions)
6. [Default Authentication Credentials](#default-authentication-credentials)
7. [REST API Documentation](#rest-api-documentation)
8. [Likely Viva Questions & Model Answers](#likely-viva-questions--model-answers)
9. [Future Enhancements](#future-enhancements)

---

## 1. Project Overview
Athenaeum provides a dual-interface architecture:
- **Primary Deliverable (Web App):** A responsive, calm editorial library web application served from Javalin's static classpath with real-time analytics, debounced live search, dynamic SVG charts, book cover tiles, circulation desk, overdue tracking, hold queues, and member management.
- **Secondary Client (JavaFX Desktop):** A rich desktop administration panel sharing the exact same Java core (`model`, `dao`, `service`, `util`), demonstrating `TabPane`, `TableView`, `FilteredList`/`SortedList`, property bindings, `BarChart`, and modal dialogs.
- **Zero Configuration:** Automatically provisions SQLite tables from `schema.sql` and batch-seeds 37 realistic holdings (books, periodicals, academic journals, reference volumes), 8 student/faculty patrons, librarians, and transactions upon first launch.

---

## 2. Tech Stack
- **Language & Runtime:** Java 17 (LTS), Apache Maven 3.8+
- **Backend Framework:** Javalin 5.6.3 (Lightweight HTTP server, zero Spring Boot, zero JPA/Hibernate)
- **JSON Serialization:** Google Gson 2.10.1
- **Database:** SQLite 3.45 via plain JDBC (`sqlite-jdbc`)
- **Desktop UI:** JavaFX 17 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`)
- **Web Frontend:** Plain HTML5, Vanilla CSS3 (Custom Design System with CSS variables), ES6+ JavaScript
- **Logging & Testing:** SLF4J Simple, JUnit 5 Jupiter

---

## 3. Full Project Structure
```text
library-management-system/
├── pom.xml                                    # Maven configuration & dependencies
├── schema.sql                                 # Relational DDL definitions & indexes
├── seed.sql                                   # Realistic holdings, members & loans
├── README.md                                  # Complete documentation & viva guide
├── src/
│   ├── main/
│   │   ├── java/com/library/
│   │   │   ├── model/                         # Domain Entities & Polymorphic Hierarchy
│   │   │   │   ├── Borrowable.java            # [Interface] Lending rules contract
│   │   │   │   ├── Searchable.java            # [Interface] Search filter contract
│   │   │   │   ├── ItemType.java              # [Enum] BOOK, MAGAZINE, REFERENCE, JOURNAL
│   │   │   │   ├── LibraryItem.java           # [Abstract Class] Holdings root with Comparable
│   │   │   │   ├── Book.java                  # [Subclass] Standard circulating book
│   │   │   │   ├── Magazine.java              # [Subclass] Periodical with 7-day loan
│   │   │   │   ├── ReferenceBook.java         # [Subclass] Non-circulating reference holding
│   │   │   │   ├── Journal.java               # [Subclass] Academic peer-reviewed journal
│   │   │   │   ├── Role.java                  # [Enum] LIBRARIAN, MEMBER
│   │   │   │   ├── Person.java                # [Abstract Class] Human actors
│   │   │   │   ├── Member.java                # [Subclass] Student & Faculty patrons
│   │   │   │   ├── Librarian.java             # [Subclass] Library administrative staff
│   │   │   │   ├── TransactionStatus.java     # [Enum] ISSUED, RETURNED, OVERDUE, LOST
│   │   │   │   ├── Transaction.java           # [Entity] Circulation loans & fines
│   │   │   │   └── Reservation.java           # [Entity] FIFO waitlist hold requests
│   │   │   ├── dao/                           # Plain JDBC Data Access Objects
│   │   │   │   ├── Repository.java            # [Generic Interface] Repository<T, ID>
│   │   │   │   ├── DatabaseManager.java       # SQLite connection & batch seeding
│   │   │   │   ├── BookDAO.java               # Overloaded search & polymorphic mapping
│   │   │   │   ├── MemberDAO.java             # Patrons & authentication queries
│   │   │   │   ├── TransactionDAO.java        # ACID Transactions (commit / rollback)
│   │   │   │   └── ReservationDAO.java        # FIFO waitlist persistence
│   │   │   ├── service/                       # Business Logic Layer
│   │   │   │   ├── CatalogService.java        # Caching (HashMap, TreeMap, HashSet, Streams)
│   │   │   │   ├── LoanService.java           # Concurrency (ReentrantLock, PriorityQueue)
│   │   │   │   └── ReportService.java         # Asynchronous report worker pool
│   │   │   ├── thread/                        # Multithreading Components
│   │   │   │   └── OverdueScannerThread.java  # Daemon thread auto-calculating fines
│   │   │   ├── exception/                     # Custom Exception Hierarchy
│   │   │   │   ├── LibraryException.java      # Base domain runtime exception
│   │   │   │   ├── BookNotAvailableException.java
│   │   │   │   ├── MemberLimitExceededException.java
│   │   │   │   ├── InvalidISBNException.java
│   │   │   │   ├── ItemNotFoundException.java
│   │   │   │   └── DuplicateEntryException.java
│   │   │   ├── util/                          # Generic Utilities & Helpers
│   │   │   │   ├── ApiResponse.java           # [Generic Class] ApiResponse<T>
│   │   │   │   ├── Pair.java                  # [Generic Class] Pair<K, V> tuple
│   │   │   │   ├── GenericUtils.java          # Bounded types & wildcards (? extends)
│   │   │   │   ├── PasswordUtil.java          # SHA-256 cryptographic hashing
│   │   │   │   └── ValidationUtil.java        # ISBN-10/13 checksum algorithms
│   │   │   ├── api/                           # Web REST API & Javalin Entry Point
│   │   │   │   ├── ApiController.java         # JSON HTTP route handlers
│   │   │   │   └── WebServer.java             # Main class, static hosting, exceptions
│   │   │   └── fx/                            # Desktop Admin GUI
│   │   │       └── AdminApp.java              # JavaFX application (TableView, Charts)
│   │   └── resources/
│   │       ├── com/library/fx/
│   │       │   └── javafx.css                 # Desktop styling matching editorial theme
│   │       └── public/                        # Web Application Static Assets
│   │           ├── index.html                 # Semantic HTML5 single-page application
│   │           ├── css/
│   │           │   └── style.css              # Calm editorial theme, typography, layout
│   │           └── js/
│   │               └── app.js                 # Vanilla ES6 event handling & SVG charts
│   └── test/java/com/library/service/
│       └── LoanServiceTest.java               # JUnit 5 unit test suite
```

---

## 4. Syllabus Concepts Coverage Matrix

| Unit | Syllabus Concept | Where Used (File & Class) | Implementation & Pedagogical Explanation |
| :--- | :--- | :--- | :--- |
| **Unit 1** | Abstract Classes & Inheritance | `model/LibraryItem.java`, `model/Person.java` | Base abstract classes defining protected fields (`id`, `title`, `name`) and polymorphic abstract contracts (`calculateFine()`, `getDisplayDetails()`). |
| **Unit 1** | Subclasses & Specialization | `model/Book.java`, `Magazine.java`, `ReferenceBook.java`, `Journal.java`, `Member.java`, `Librarian.java` | Subclasses extend abstract parents with domain attributes (e.g. `isbn`, `impactFactor`, `membershipId`, `shift`). |
| **Unit 1** | `super` & Constructor Chaining | `model/Book.java`, `model/LibraryItem.java`, `model/Member.java` | Multi-argument constructors chain to overloaded constructors and invoke `super(...)` to enforce base initialization. |
| **Unit 1** | Interfaces | `model/Borrowable.java`, `model/Searchable.java` | Decoupled contracts defining loan eligibility (`isBorrowable()`) and case-insensitive substring searching (`matchesQuery()`). |
| **Unit 1** | `final` & `static` Keywords | `model/LibraryItem.java`, `util/PasswordUtil.java`, `util/GenericUtils.java` | Static constants (`CURRENCY_SYMBOL`), static factory utilities, and `final` immutable fields (`itemType`). |
| **Unit 2** | Method Overriding (Runtime Polymorphism) | `model/Book.java`, `Magazine.java`, `ReferenceBook.java`, `Journal.java` | `calculateFine()` and `getLoanPeriodDays()` overridden per material type (e.g., Reference books return 0/non-borrowable; Journals charge ₹5/day). |
| **Unit 2** | Method Overloading (Compile-time Polymorphism) | `dao/BookDAO.java` | Overloaded search variations: `search(title)`, `search(title, author)`, `search(title, category, availableOnly)`. |
| **Unit 2** | Uniform Polymorphic Processing | `service/CatalogService.java`, `thread/OverdueScannerThread.java` | A heterogeneous `List<LibraryItem>` processed uniformly during inventory scans and overdue fine recalculation. |
| **Unit 2** | Custom Exception Hierarchy | `exception/*` | Domain-specific exceptions (`BookNotAvailableException`, `MemberLimitExceededException`, `InvalidISBNException`, `DuplicateEntryException`). |
| **Unit 2** | try-with-resources & Global Exception Handler | `dao/*`, `api/WebServer.java` | Automatic resource disposal for JDBC `Connection`, `Statement`, `ResultSet`; central Javalin exception handlers returning structured JSON error payloads. |
| **Unit 3** | Background Daemon Thread | `thread/OverdueScannerThread.java` | An active background daemon thread (`setDaemon(true)`) executing periodic scans every 20 seconds to compute fines. |
| **Unit 3** | Thread Pool (`ExecutorService`) | `service/ReportService.java` | Fixed thread pool (`Executors.newFixedThreadPool(3)`) asynchronously executing audit reports without blocking the main web or GUI threads. |
| **Unit 3** | Concurrency Locks (`ReentrantLock`) | `service/LoanService.java` | A fair `ReentrantLock` guarding checkout and return operations to prevent race conditions when two users claim the last copy. |
| **Unit 3** | Thread-Safe Types | `service/ReportService.java`, `service/LoanService.java` | `ConcurrentHashMap` for metrics caching and `AtomicInteger` for lock-free live counter increments. |
| **Unit 3** | Event Handling (JavaFX & DOM) | `fx/AdminApp.java`, `public/js/app.js` | JavaFX `ActionEvent`, `KeyEvent`, `MouseEvent`, selection listeners on TableView; JavaScript DOM `submit`, `click`, and debounced `input` events. |
| **Unit 4** | DAO Pattern & Plain JDBC | `dao/BookDAO.java`, `MemberDAO.java`, `TransactionDAO.java` | Clean architectural separation using `Connection`, `PreparedStatement` with bind parameters (SQL-injection safe), and `ResultSet`. |
| **Unit 4** | ACID Transactions (`commit` / `rollback`) | `dao/TransactionDAO.java` | `conn.setAutoCommit(false)`, multi-table updates (item copy decrement, member quota increment, transaction insert), with `conn.rollback()` on failure. |
| **Unit 4** | JDBC Batch Inserts | `dao/DatabaseManager.java` | `stmt.addBatch()` and `stmt.executeBatch()` for rapid relational seed initialization. |
| **Unit 4** | Collections: `HashMap`, `TreeMap`, `HashSet` | `service/CatalogService.java` | `HashMap` for $O(1)$ ISBN lookups, `TreeMap` for title-sorted in-memory catalog, `HashSet` for unique genre tags. |
| **Unit 4** | Collections: `Queue` & `PriorityQueue` | `service/LoanService.java` | `LinkedList` acting as FIFO waitlist reservation queue; `PriorityQueue` ordering overdue transactions by penalty amount descending. |
| **Unit 4** | Collections: `Iterator`, `Comparable`, `Comparator`, Streams | `service/CatalogService.java`, `model/LibraryItem.java` | `Iterator` traversal, `Comparable<LibraryItem>`, `Collections.sort()` with `Comparator`, and Java Streams for `groupingBy` and `counting`. |
| **Unit 5** | Generic Interface | `dao/Repository.java` | `Repository<T, ID>` defining generic CRUD methods (`Optional<T> findById(ID id)`, `List<T> findAll()`, `T save(T entity)`). |
| **Unit 5** | Generic Classes | `util/ApiResponse.java`, `util/Pair.java` | Parameterized classes: `ApiResponse<T>` wrapping REST JSON payloads; `Pair<K, V>` representing immutable tuples. |
| **Unit 5** | Bounded Generics & Wildcards | `util/GenericUtils.java` | Upper-bounded type parameter `<T extends LibraryItem> List<T> filterAvailable(List<T> items)` and wildcards `List<? extends LibraryItem>`. |
| **Unit 5** | Advanced JavaFX GUI Programming | `fx/AdminApp.java` | Desktop administration application with `TabPane`, `TableView<T>` with `ObservableList` and property bindings, `FilteredList`, `SortedList`, `ComboBox`, `DatePicker`, `BarChart`, and CSS styling. |

---

## 5. Step-by-Step Run Instructions

### Prerequisites
- JDK 17 (verified with OpenJDK 17 or Temurin 17)
- Apache Maven 3.8+
- Port 7070 available (or set custom port via argument)

### Step 1: Clone / Navigate to Workspace
```bash
cd "/Users/omawasthi/Developer/java project 5th sem"
```

### Step 2: Build and Run Automated Tests
```bash
mvn clean test
```
*Expected Output:* `Tests run: 4, Failures: 0, Errors: 0, BUILD SUCCESS`.

### Step 3: Run the Web Server Application
Execute the Maven exec plugin:
```bash
mvn exec:java -Dexec.mainClass="com.library.api.WebServer"
```
The server will start in $\approx 150\text{ ms}$, automatically initialize `library.db`, execute `schema.sql`, batch-insert 37 holdings from `seed.sql`, and launch the background overdue scanner daemon.

### Step 4: Open the Web Application
Open any web browser and navigate to:
```text
http://localhost:7070
```
- Click **"Librarian: admin@library.edu"** or **"Member: rahul.sharma@student.edu"** to test 1-click credential filling.
- Click **"Sign In to Repository"**.
- Explore the **Dashboard**, **Catalog Holdings** (toggle between Grid Cover Tiles and Data Table), **Issue & Return Desk**, **Overdue & Fines**, **Members Directory**, **Hold Reservations**, and **Async Reports**.

### Step 5: Run the JavaFX Desktop Client (Concurrent Execution)
In a second terminal window (while the web server is running, or standalone as it connects to the same shared SQLite database):
```bash
mvn javafx:run
```
A desktop window titled **"Athenaeum Library System — Desktop Administration"** will launch, rendering the editorial JavaFX theme with reactive `TableView`, search filtering via `FilteredList`, and category `BarChart`.

---

## 6. Default Authentication Credentials

| Role | Name | Email | Password | Access Rights |
| :--- | :--- | :--- | :--- | :--- |
| **Librarian (Staff)** | Dr. Shashi Bhushan | `admin@library.edu` | `admin123` | Full Access: Cataloging, Add/Edit/Delete Books, Member Management, Issue/Return, Lost item marking, Daemon triggers. |
| **Librarian (Staff)** | Ms. Ananya Sen | `ananya.sen@library.edu` | `admin123` | Full Access: Circulation and Cataloging. |
| **Member (Student)** | Rahul Sharma | `rahul.sharma@student.edu` | `member123` | Catalog browsing, Place waitlist reservations, View borrowing history. |
| **Member (Student)** | Priya Verma | `priya.verma@student.edu` | `member123` | Catalog browsing, Reserve books, View loans. |
| **Member (Faculty)** | Prof. Aarav Patel | `aarav.patel@faculty.edu` | `member123` | Higher borrow limit (6 books), Catalog browsing. |

---

## 7. REST API Documentation

### Authentication
- `POST /api/auth/login`: Authenticate with institutional email & password (returns user profile).
- `POST /api/auth/logout`: Invalidate HTTP session.
- `GET /api/auth/me`: Retrieve current active session user.

### Analytics & Diagnostics
- `GET /api/stats`: Retrieve live dashboard counters (total holdings, copies, active loans, overdue count, category distribution).
- `GET /api/categories`: Fetch unique genres and material types.

### Catalog (Holdings Management)
- `GET /api/books`: Search catalog with query params (`q`, `category`, `available`, `sort`).
- `GET /api/books/{id}`: Retrieve single holding details.
- `POST /api/books`: Catalog a new Book, Magazine, Journal, or Reference item (with ISBN check).
- `PUT /api/books/{id}`: Update holding attributes.
- `PUT /api/books/{id}/copies`: Adjust total and available inventory counts.
- `DELETE /api/books/{id}`: Remove holding from catalog.

### Circulation & Loans
- `POST /api/loans/issue`: Issue book to member (`itemId`, `memberId`, `days`).
- `POST /api/loans/return`: Process book return and finalize fine calculation (`transactionId`).
- `POST /api/loans/lost`: Mark item lost, adjust member loan count, decrement total inventory.
- `GET /api/loans/active`: List all currently circulating items.
- `GET /api/loans/overdue`: List overdue loans prioritized by fine (Unit 4 PriorityQueue).
- `GET /api/loans/history`: Retrieve global transaction activity audit trail.

### Members Directory
- `GET /api/members`: List all registered student and faculty members.
- `POST /api/members`: Register new member with quota and membership ID.
- `PUT /api/members/{id}`: Update member profile and borrow limit.
- `GET /api/members/{id}/history`: Retrieve member-specific loan history.

### Reservations (FIFO Queue)
- `GET /api/reservations`: List all waitlist hold reservations.
- `POST /api/reservations`: Place hold reservation on out-of-stock item.
- `POST /api/reservations/{id}/cancel`: Cancel hold request.

### Background Tasks & Reports
- `GET /api/reports/{type}`: Asynchronously execute report (`most-borrowed`, `overdue-audit`, `system-health`) on Java thread pool.
- `POST /api/admin/scan-overdue`: Manually trigger overdue scanner thread iteration.

---

## 8. Likely Viva Questions & Model Answers

### Q1: Why did you choose an abstract class for `LibraryItem` instead of a regular class or pure interface?
**Answer:** `LibraryItem` captures both shared state (`id`, `title`, `publisher`, `year`, `totalCopies`, `availableCopies`) and common behavior (such as `isAvailable()` and `compareTo()`), while requiring concrete subclasses (`Book`, `Magazine`, `Journal`, `ReferenceBook`) to provide specialized implementations for `getLoanPeriodDays()`, `calculateFine()`, and `getDisplayDetails()`. An interface cannot hold mutable instance state or constructor logic, and a concrete class would allow instantiating a generic "item" that has no defined lending or fine rules.

### Q2: How is runtime polymorphism demonstrated in the overdue fine calculation?
**Answer:** Each subclass overrides `calculateFine(long overdueDays)`. When the `OverdueScannerThread` iterates through a `List<Transaction>`, it retrieves the `LibraryItem` reference and invokes `item.calculateFine(days)`. At runtime, Java dynamically binds the call to the appropriate subclass:
- For `Book`: ₹2.00/day.
- For `Magazine`: ₹1.00/day.
- For `Journal`: ₹5.00/day (higher rate for academic research materials).
- For `ReferenceBook`: ₹0.00 (non-circulating, disallowed from checkout via `isBorrowable()`).

### Q3: How did you ensure thread safety when two users attempt to issue the last copy simultaneously?
**Answer:** In `LoanService.java`, checkout and return operations are synchronized using a fair `ReentrantLock` (`circulationLock`). When a checkout request arrives, the thread acquires the lock, queries the database within an ACID transaction (`conn.setAutoCommit(false)`), verifies that `available_copies > 0` and that the member's `active_borrow_count < max_borrow_limit`, decrements the copy count, commits the transaction, and finally releases the lock in a `finally` block. This prevents race conditions and dirty reads.

### Q4: Why is SQLite suitable for this project, and how does it prevent SQL injection?
**Answer:** SQLite requires zero server setup, runs in-process via JDBC, and persists all data into a local `library.db` file. SQL injection is completely prevented across all DAOs by strictly using `PreparedStatement` with parameterized placeholders (`?`) rather than string concatenation.

### Q5: How is ACID transaction management implemented in `TransactionDAO`?
**Answer:** When issuing a book, multiple interdependent operations must succeed or fail together. We disable auto-commit via `conn.setAutoCommit(false)`. We execute:
1. Availability and member quota verification.
2. Decrementing `available_copies` in `items`.
3. Incrementing `active_borrow_count` in `persons`.
4. Inserting a new record into `transactions`.
If any step throws an exception, `conn.rollback()` is invoked in the `catch` block to restore database consistency. If all steps succeed, `conn.commit()` is called.

### Q6: Where and why is `PriorityQueue` utilized in the Collections Framework?
**Answer:** In `LoanService.getOverdueLoansPrioritizedByFine()`, a `PriorityQueue<Transaction>` is instantiated with a custom comparator: `(t1, t2) -> Double.compare(t2.getFineAmount(), t1.getFineAmount())`. This orders overdue loans so that library administrators immediately see the highest-delinquency accounts at the head of the queue ($O(\log n)$ insertion).

### Q7: Explain the bounded generic method `<T extends LibraryItem> List<T> filterAvailable(List<T> items)`.
**Answer:** The bounded type parameter `<T extends LibraryItem>` restricts the method to `LibraryItem` or any of its subclasses (`Book`, `Journal`, etc.). It guarantees type safety at compile time without requiring unsafe casting (`(Book)`), ensuring that the returned list maintains the exact subtype passed in.

### Q8: What role does the `OverdueScannerThread` play, and why is it marked as a daemon thread?
**Answer:** `OverdueScannerThread` is a background worker thread extending `Thread`. It sleeps for an interval (e.g. 20s) and periodically checks the database for loans where `dueDate < LocalDate.now()`, automatically computing late fees polymorphically. It is marked `setDaemon(true)` so that it does not prevent the JVM from shutting down cleanly when the main web server terminates.

### Q9: How does the JavaFX client share the same core with the Web application?
**Answer:** Both `WebServer` and JavaFX `AdminApp` reside in the same project and instantiate the exact same DAOs (`BookDAO`, `MemberDAO`, `TransactionDAO`) and services (`CatalogService`, `LoanService`). They read and write to the same relational SQLite database (`library.db`). JavaFX binds this shared data to reactive `ObservableList` collections.

### Q10: How are passwords secured in the database?
**Answer:** Passwords are never stored in plain text. `PasswordUtil.hash()` computes a SHA-256 cryptographic digest of the user's password, converting it to a 64-character hexadecimal string before saving to the `password_hash` column in the `persons` table. During login, the candidate password is hashed and compared using `PasswordUtil.verify()`.

---

## 9. Future Enhancements
1. **RFID / Physical Barcode Scanner Integration:** Extend the circulation desk with USB barcode and RFID reader listeners for automated shelf inventory audits and express self-checkout kiosks.
2. **Automated Email / SMS Overdue Reminders:** Integrate JavaMail API or an SMS gateway (e.g. Twilio) inside `OverdueScannerThread` to automatically dispatch overdue reminder notices to patrons 2 days before the due date.
3. **Fine Online Payment Gateway Integration:** Add UPI and card payment webhooks (e.g., Razorpay / Stripe) to the web portal so students can settle overdue fines directly online.
