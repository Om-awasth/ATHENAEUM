-- schema.sql: Database schema for the Library Management System
-- Demonstrates Relational Data Modeling with SQLite, foreign keys, and constraints.

PRAGMA foreign_keys = ON;

-- Table for all library inventory items (Polymorphic hierarchy table)
CREATE TABLE IF NOT EXISTS items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_type TEXT NOT NULL CHECK(item_type IN ('BOOK', 'MAGAZINE', 'REFERENCE', 'JOURNAL')),
    title TEXT NOT NULL,
    publisher TEXT NOT NULL,
    year INTEGER NOT NULL,
    total_copies INTEGER NOT NULL DEFAULT 1,
    available_copies INTEGER NOT NULL DEFAULT 1,
    -- Specific to Book
    isbn TEXT,
    author TEXT,
    genre TEXT,
    -- Specific to Magazine
    issue_number INTEGER,
    month TEXT,
    category TEXT,
    -- Specific to ReferenceBook
    field_of_study TEXT,
    edition TEXT,
    dewey_decimal TEXT,
    -- Specific to Journal
    volume TEXT,
    peer_reviewed INTEGER DEFAULT 1,
    impact_factor REAL DEFAULT 0.0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for library users (Librarians and Members)
CREATE TABLE IF NOT EXISTS persons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    phone TEXT NOT NULL,
    role TEXT NOT NULL CHECK(role IN ('LIBRARIAN', 'MEMBER')),
    password_hash TEXT NOT NULL,
    -- Member specific
    membership_id TEXT UNIQUE,
    max_borrow_limit INTEGER DEFAULT 3,
    active_borrow_count INTEGER DEFAULT 0,
    join_date TEXT DEFAULT (date('now')),
    -- Librarian specific
    employee_id TEXT UNIQUE,
    shift TEXT,
    department TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for circulation transactions (Borrow, Return, Overdue, Lost)
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE RESTRICT,
    person_id INTEGER NOT NULL REFERENCES persons(id) ON DELETE RESTRICT,
    issue_date TEXT NOT NULL,
    due_date TEXT NOT NULL,
    return_date TEXT,
    fine_amount REAL DEFAULT 0.0,
    status TEXT NOT NULL CHECK(status IN ('ISSUED', 'RETURNED', 'OVERDUE', 'LOST')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table for FIFO book reservation queue when copies are exhausted
CREATE TABLE IF NOT EXISTS reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
    person_id INTEGER NOT NULL REFERENCES persons(id) ON DELETE CASCADE,
    reservation_date TEXT NOT NULL,
    status TEXT NOT NULL CHECK(status IN ('WAITING', 'NOTIFIED', 'FULFILLED', 'CANCELLED')),
    queue_position INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high-frequency queries
CREATE INDEX IF NOT EXISTS idx_items_title ON items(title);
CREATE INDEX IF NOT EXISTS idx_items_isbn ON items(isbn);
CREATE INDEX IF NOT EXISTS idx_items_author ON items(author);
CREATE INDEX IF NOT EXISTS idx_persons_email ON persons(email);
CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
