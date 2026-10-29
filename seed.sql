-- seed.sql: Realistic Seed Data for Library Management System
-- Contains 28 books, 3 magazines, 3 reference books, 3 journals, 10 persons (2 librarians, 8 members),
-- along with active, overdue, and returned circulation records.

-- 1. Inventory Items
-- Books
INSERT INTO items (item_type, title, publisher, year, total_copies, available_copies, isbn, author, genre) VALUES
('BOOK', 'Effective Java (3rd Edition)', 'Addison-Wesley', 2018, 5, 2, '978-0134685991', 'Joshua Bloch', 'Computer Science'),
('BOOK', 'Clean Code: A Handbook of Agile Software Craftsmanship', 'Prentice Hall', 2008, 4, 1, '978-0132350884', 'Robert C. Martin', 'Computer Science'),
('BOOK', 'Design Patterns: Elements of Reusable Object-Oriented Software', 'Addison-Wesley', 1994, 3, 2, '978-0201633610', 'Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides', 'Computer Science'),
('BOOK', 'Introduction to Algorithms (4th Edition)', 'MIT Press', 2022, 6, 3, '978-0262046305', 'Thomas H. Cormen, Charles E. Leiserson, Ronald L. Rivest, Clifford Stein', 'Computer Science'),
('BOOK', 'Operating System Concepts', 'John Wiley & Sons', 2018, 4, 2, '978-1119456339', 'Abraham Silberschatz, Peter B. Galvin, Greg Gagne', 'Computer Science'),
('BOOK', 'Database System Concepts', 'McGraw-Hill', 2019, 5, 4, '978-0078022159', 'Abraham Silberschatz, Henry F. Korth, S. Sudarshan', 'Computer Science'),
('BOOK', 'Computer Networks (6th Edition)', 'Pearson', 2021, 4, 2, '978-0136958505', 'Andrew S. Tanenbaum, David J. Wetherall', 'Computer Science'),
('BOOK', 'Artificial Intelligence: A Modern Approach', 'Pearson', 2020, 3, 1, '978-0134610993', 'Stuart Russell, Peter Norvig', 'Computer Science'),
('BOOK', 'The Pragmatic Programmer: 20th Anniversary Edition', 'Addison-Wesley', 2019, 4, 3, '978-0135957059', 'David Thomas, Andrew Hunt', 'Computer Science'),
('BOOK', 'Deep Learning', 'MIT Press', 2016, 2, 0, '978-0262035613', 'Ian Goodfellow, Yoshua Bengio, Aaron Courville', 'Computer Science'),
('BOOK', 'The God of Small Things', 'IndiaInk / Random House', 1997, 4, 3, '978-0812979657', 'Arundhati Roy', 'Literature'),
('BOOK', 'Midnight''s Children', 'Jonathan Cape', 1981, 3, 2, '978-0812976533', 'Salman Rushdie', 'Literature'),
('BOOK', 'A Fine Balance', 'McClelland & Stewart', 1995, 3, 2, '978-1400030651', 'Rohinton Mistry', 'Literature'),
('BOOK', 'The White Tiger', 'Free Press', 2008, 4, 3, '978-1416562603', 'Aravind Adiga', 'Fiction'),
('BOOK', 'Train to Pakistan', 'Grove Press', 1956, 3, 2, '978-0802132215', 'Khushwant Singh', 'Historical Fiction'),
('BOOK', 'Wings of Fire: An Autobiography', 'Universities Press', 1999, 6, 4, '978-8173711466', 'A.P.J. Abdul Kalam, Arun Tiwari', 'Biography'),
('BOOK', 'The Discovery of India', 'Signet Press', 1946, 3, 2, '978-0143031031', 'Jawaharlal Nehru', 'History'),
('BOOK', 'To Kill a Mockingbird', 'J. B. Lippincott & Co.', 1960, 5, 4, '978-0060935467', 'Harper Lee', 'Classics'),
('BOOK', '1984', 'Secker & Warburg', 1949, 5, 3, '978-0451524935', 'George Orwell', 'Classics'),
('BOOK', 'Pride and Prejudice', 'T. Egerton', 1813, 4, 3, '978-0141439518', 'Jane Austen', 'Classics'),
('BOOK', 'The Great Gatsby', 'Charles Scribner''s Sons', 1925, 4, 3, '978-0743273565', 'F. Scott Fitzgerald', 'Classics'),
('BOOK', 'Sapiens: A Brief History of Humankind', 'Harvill Secker', 2014, 5, 2, '978-0062316097', 'Yuval Noah Harari', 'Non-Fiction'),
('BOOK', 'A Brief History of Time', 'Bantam Books', 1988, 4, 3, '978-0553380163', 'Stephen Hawking', 'Science'),
('BOOK', 'Guns, Germs, and Steel', 'W. W. Norton & Co.', 1997, 3, 2, '978-0393317558', 'Jared Diamond', 'Anthropology'),
('BOOK', 'Thinking, Fast and Slow', 'Farrar, Straus and Giroux', 2011, 4, 3, '978-0374533557', 'Daniel Kahneman', 'Psychology'),
('BOOK', 'The Gene: An Intimate History', 'Scribner', 2016, 3, 2, '978-1476733524', 'Siddhartha Mukherjee', 'Science'),
('BOOK', 'Crime and Punishment', 'The Russian Messenger', 1866, 3, 2, '978-0140449136', 'Fyodor Dostoevsky', 'Classics'),
('BOOK', 'Structure and Interpretation of Computer Programs', 'MIT Press', 1996, 3, 2, '978-0262510875', 'Harold Abelson, Gerald Jay Sussman', 'Computer Science');

-- Magazines (Short borrowing period: 7 days)
INSERT INTO items (item_type, title, publisher, year, total_copies, available_copies, issue_number, month, category) VALUES
('MAGAZINE', 'IEEE Spectrum - AI Revolution Issue', 'IEEE', 2024, 2, 2, 612, 'October', 'Technology'),
('MAGAZINE', 'National Geographic: Hidden Wonders', 'National Geographic Partners', 2024, 3, 2, 248, 'September', 'Geography & Science'),
('MAGAZINE', 'Frontline - National Affairs', 'The Hindu Group', 2024, 2, 1, 104, 'October', 'Current Affairs');

-- Reference Books (Non-borrowable, strictly in-library reading)
INSERT INTO items (item_type, title, publisher, year, total_copies, available_copies, field_of_study, edition, dewey_decimal) VALUES
('REFERENCE', 'Encyclopaedia Britannica: World History Vol. 1', 'Encyclopaedia Britannica Inc.', 2020, 1, 1, 'General Knowledge', '15th Edition', '031.02'),
('REFERENCE', 'Oxford English Dictionary (Comprehensive)', 'Oxford University Press', 2021, 2, 2, 'Linguistics', '3rd Edition', '423.1'),
('REFERENCE', 'CRC Handbook of Chemistry and Physics', 'CRC Press', 2023, 1, 1, 'Physical Sciences', '104th Edition', '540.21');

-- Academic Journals (Short loan period: 5 days, peer-reviewed)
INSERT INTO items (item_type, title, publisher, year, total_copies, available_copies, volume, peer_reviewed, impact_factor) VALUES
('JOURNAL', 'ACM Transactions on Computer Systems (TOCS)', 'ACM', 2024, 2, 1, 'Vol. 42 No. 3', 1, 3.85),
('JOURNAL', 'IEEE Transactions on Software Engineering', 'IEEE Computer Society', 2024, 2, 2, 'Vol. 50 No. 8', 1, 7.42),
('JOURNAL', 'Nature: Machine Intelligence', 'Springer Nature', 2024, 2, 1, 'Vol. 6 Issue 9', 1, 18.8);

-- 2. Persons: Librarians and Members
-- Password for all librarians is "admin123" (SHA-256 hash below)
-- Password for all members is "member123" (SHA-256 hash below)
INSERT INTO persons (name, email, phone, role, password_hash, employee_id, shift, department) VALUES
('Dr. Shashi Bhushan', 'admin@library.edu', '+91 98765 43210', 'LIBRARIAN', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'LIB-001', 'Morning', 'Head Librarian'),
('Ms. Ananya Sen', 'ananya.sen@library.edu', '+91 98765 43211', 'LIBRARIAN', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'LIB-002', 'Evening', 'Circulation & Catalog');

INSERT INTO persons (name, email, phone, role, password_hash, membership_id, max_borrow_limit, active_borrow_count, join_date) VALUES
('Rahul Sharma', 'rahul.sharma@student.edu', '+91 91234 56780', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-001', 3, 2, '2024-01-15'),
('Priya Verma', 'priya.verma@student.edu', '+91 91234 56781', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-002', 3, 1, '2024-01-20'),
('Prof. Aarav Patel', 'aarav.patel@faculty.edu', '+91 91234 56782', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-003', 6, 2, '2023-08-10'),
('Sneha Rao', 'sneha.rao@student.edu', '+91 91234 56783', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-004', 3, 1, '2024-02-01'),
('Vikram Iyer', 'vikram.iyer@student.edu', '+91 91234 56784', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-005', 3, 1, '2024-02-15'),
('Neha Kulkarni', 'neha.kulkarni@student.edu', '+91 91234 56785', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-006', 3, 0, '2024-03-01'),
('Arjun Gupta', 'arjun.gupta@student.edu', '+91 91234 56786', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-007', 3, 1, '2024-03-10'),
('Dr. Divya Nair', 'divya.nair@faculty.edu', '+91 91234 56787', 'MEMBER', '5600376e863d2f57a053518f324ad3840b0bc2348b573af281a7b7cbe7a228c6', 'MEM-2024-008', 6, 0, '2023-07-22');

-- 3. Transactions (Issued, Overdue, and Returned records)
-- Active loans
INSERT INTO transactions (item_id, person_id, issue_date, due_date, status) VALUES
(1, 3, '2026-10-20', '2026-11-03', 'ISSUED'),
(2, 3, '2026-10-22', '2026-11-05', 'ISSUED'),
(4, 4, '2026-10-24', '2026-11-07', 'ISSUED'),
(7, 5, '2026-10-18', '2026-11-01', 'ISSUED'),
(22, 6, '2026-10-25', '2026-11-08', 'ISSUED');

-- Overdue loans (Due date has passed; fines computed automatically: ₹2/day for books, ₹5/day for journals)
INSERT INTO transactions (item_id, person_id, issue_date, due_date, fine_amount, status) VALUES
(8, 7, '2026-10-01', '2026-10-15', 26.0, 'OVERDUE'),
(10, 5, '2026-09-28', '2026-10-12', 32.0, 'OVERDUE'),
(36, 9, '2026-10-15', '2026-10-20', 40.0, 'OVERDUE');

-- Returned loans (Historical activity)
INSERT INTO transactions (item_id, person_id, issue_date, due_date, return_date, fine_amount, status) VALUES
(18, 3, '2026-09-01', '2026-09-15', '2026-09-14', 0.0, 'RETURNED'),
(19, 4, '2026-09-05', '2026-09-19', '2026-09-18', 0.0, 'RETURNED'),
(3, 8, '2026-09-10', '2026-09-24', '2026-09-26', 4.0, 'RETURNED'),
(5, 10, '2026-09-15', '2026-09-29', '2026-09-28', 0.0, 'RETURNED');

-- 4. Reservations (Waiting queue for exhausted books)
INSERT INTO reservations (item_id, person_id, reservation_date, status, queue_position) VALUES
(10, 8, '2026-10-16', 'WAITING', 1),
(10, 10, '2026-10-18', 'WAITING', 2),
(2, 9, '2026-10-26', 'WAITING', 1);

