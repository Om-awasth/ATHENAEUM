package com.library.dao;

import com.library.exception.BookNotAvailableException;
import com.library.exception.ItemNotFoundException;
import com.library.exception.LibraryException;
import com.library.exception.MemberLimitExceededException;
import com.library.model.ItemType;
import com.library.model.Transaction;
import com.library.model.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for book circulation records.
 * Implements full ACID transactions (commit/rollback) for issuing and returning items.
 *
 * [Concept: DAO Pattern]
 * [Concept: Transaction Management - commit / rollback]
 * [Concept: JDBC - Concurrency and Isolation]
 */
public class TransactionDAO implements Repository<Transaction, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(TransactionDAO.class);

    @Override
    public Optional<Transaction> findById(Integer id) {
        String sql = "SELECT t.*, i.title AS item_title, i.item_type, p.name AS member_name, p.email AS member_email " +
                     "FROM transactions t " +
                     "JOIN items i ON t.item_id = i.id " +
                     "JOIN persons p ON t.person_id = p.id " +
                     "WHERE t.id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding transaction by id: {}", id, e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findAll() {
        String sql = "SELECT t.*, i.title AS item_title, i.item_type, p.name AS member_name, p.email AS member_email " +
                     "FROM transactions t " +
                     "JOIN items i ON t.item_id = i.id " +
                     "JOIN persons p ON t.person_id = p.id " +
                     "ORDER BY t.id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving all transactions", e);
        }
        return list;
    }

    public List<Transaction> findActiveLoans() {
        String sql = "SELECT t.*, i.title AS item_title, i.item_type, p.name AS member_name, p.email AS member_email " +
                     "FROM transactions t " +
                     "JOIN items i ON t.item_id = i.id " +
                     "JOIN persons p ON t.person_id = p.id " +
                     "WHERE t.status IN ('ISSUED', 'OVERDUE') " +
                     "ORDER BY t.due_date ASC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.error("Error querying active loans", e);
        }
        return list;
    }

    public List<Transaction> findOverdueLoans() {
        String sql = "SELECT t.*, i.title AS item_title, i.item_type, p.name AS member_name, p.email AS member_email " +
                     "FROM transactions t " +
                     "JOIN items i ON t.item_id = i.id " +
                     "JOIN persons p ON t.person_id = p.id " +
                     "WHERE (t.status = 'OVERDUE' OR (t.status = 'ISSUED' AND t.due_date < date('now'))) " +
                     "ORDER BY t.due_date ASC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            logger.error("Error querying overdue loans", e);
        }
        return list;
    }

    public List<Transaction> findMemberHistory(int memberId) {
        String sql = "SELECT t.*, i.title AS item_title, i.item_type, p.name AS member_name, p.email AS member_email " +
                     "FROM transactions t " +
                     "JOIN items i ON t.item_id = i.id " +
                     "JOIN persons p ON t.person_id = p.id " +
                     "WHERE t.person_id = ? " +
                     "ORDER BY t.id DESC";

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToTransaction(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error querying loan history for member #{}", memberId, e);
        }
        return list;
    }

    /**
     * [Concept: Transaction Management with commit and rollback]
     * Issues an item to a member atomically.
     * Verifies item availability and member borrowing quota within the same transaction.
     */
    public Transaction issueItem(int itemId, int memberId, LocalDate issueDate, LocalDate dueDate) {
        String checkItemSql = "SELECT title, item_type, available_copies FROM items WHERE id = ?";
        String checkMemberSql = "SELECT name, email, max_borrow_limit, active_borrow_count FROM persons WHERE id = ? AND role = 'MEMBER'";
        String decrementItemSql = "UPDATE items SET available_copies = available_copies - 1 WHERE id = ?";
        String incrementMemberSql = "UPDATE persons SET active_borrow_count = active_borrow_count + 1 WHERE id = ?";
        String insertTxSql = "INSERT INTO transactions (item_id, person_id, issue_date, due_date, status) VALUES (?, ?, ?, ?, 'ISSUED')";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // [Concept: Begin Transaction]
            try {
                // 1. Check item availability
                String itemTitle;
                ItemType itemType;
                try (PreparedStatement ps = conn.prepareStatement(checkItemSql)) {
                    ps.setInt(1, itemId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ItemNotFoundException("Item #" + itemId, "Catalog item not found");
                        }
                        itemTitle = rs.getString("title");
                        itemType = ItemType.valueOf(rs.getString("item_type"));
                        if (itemType == ItemType.REFERENCE) {
                            throw new BookNotAvailableException(itemId, "Reference materials cannot be borrowed for home use.");
                        }
                        int available = rs.getInt("available_copies");
                        if (available <= 0) {
                            throw new BookNotAvailableException(itemId, "No copies of '" + itemTitle + "' are currently available.");
                        }
                    }
                }

                // 2. Check member quota
                String memberName;
                String memberEmail;
                try (PreparedStatement ps = conn.prepareStatement(checkMemberSql)) {
                    ps.setInt(1, memberId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ItemNotFoundException("Member #" + memberId, "Member account not found");
                        }
                        memberName = rs.getString("name");
                        memberEmail = rs.getString("email");
                        int maxLimit = rs.getInt("max_borrow_limit");
                        int activeCount = rs.getInt("active_borrow_count");
                        if (activeCount >= maxLimit) {
                            throw new MemberLimitExceededException(memberId, maxLimit,
                                    memberName + " has reached the maximum borrowing limit of " + maxLimit + " items.");
                        }
                    }
                }

                // 3. Decrement item copy count
                try (PreparedStatement ps = conn.prepareStatement(decrementItemSql)) {
                    ps.setInt(1, itemId);
                    ps.executeUpdate();
                }

                // 4. Increment member active borrow count
                try (PreparedStatement ps = conn.prepareStatement(incrementMemberSql)) {
                    ps.setInt(1, memberId);
                    ps.executeUpdate();
                }

                // 5. Insert transaction record
                int generatedTxId = 0;
                try (PreparedStatement ps = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, itemId);
                    ps.setInt(2, memberId);
                    ps.setString(3, issueDate.toString());
                    ps.setString(4, dueDate.toString());
                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            generatedTxId = keys.getInt(1);
                        }
                    }
                }

                conn.commit(); // [Concept: Commit Transaction]
                logger.info("Transaction #{} committed: Issued '{}' to {}", generatedTxId, itemTitle, memberName);

                return new Transaction(generatedTxId, itemId, itemTitle, itemType, memberId,
                        memberName, memberEmail, issueDate.toString(), dueDate.toString(),
                        null, 0.0, TransactionStatus.ISSUED);

            } catch (Exception ex) {
                conn.rollback(); // [Concept: Rollback Transaction on error]
                logger.warn("Transaction rolled back for item #{} to member #{}: {}", itemId, memberId, ex.getMessage());
                if (ex instanceof LibraryException) {
                    throw (LibraryException) ex;
                }
                throw new LibraryException("Circulation transaction failed: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database connection error during checkout: " + e.getMessage(), e);
        }
    }

    /**
     * [Concept: Transaction Management with commit and rollback]
     * Returns a borrowed book, settles fine, frees copy, and decrements member loan count.
     */
    public boolean returnItem(int transactionId, LocalDate returnDate, double fineAmount) {
        String findTxSql = "SELECT item_id, person_id, status FROM transactions WHERE id = ?";
        String updateTxSql = "UPDATE transactions SET return_date = ?, fine_amount = ?, status = 'RETURNED' WHERE id = ?";
        String incrementItemSql = "UPDATE items SET available_copies = available_copies + 1 WHERE id = ?";
        String decrementMemberSql = "UPDATE persons SET active_borrow_count = MAX(0, active_borrow_count - 1) WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // [Concept: Begin Transaction]
            try {
                int itemId;
                int personId;
                try (PreparedStatement ps = conn.prepareStatement(findTxSql)) {
                    ps.setInt(1, transactionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new ItemNotFoundException("Transaction #" + transactionId, "Transaction not found");
                        }
                        String statusStr = rs.getString("status");
                        if ("RETURNED".equalsIgnoreCase(statusStr)) {
                            throw new LibraryException("This loan has already been marked as returned.");
                        }
                        itemId = rs.getInt("item_id");
                        personId = rs.getInt("person_id");
                    }
                }

                // 1. Update transaction
                try (PreparedStatement ps = conn.prepareStatement(updateTxSql)) {
                    ps.setString(1, returnDate.toString());
                    ps.setDouble(2, fineAmount);
                    ps.setInt(3, transactionId);
                    ps.executeUpdate();
                }

                // 2. Increment item copy count
                try (PreparedStatement ps = conn.prepareStatement(incrementItemSql)) {
                    ps.setInt(1, itemId);
                    ps.executeUpdate();
                }

                // 3. Decrement member borrow count
                try (PreparedStatement ps = conn.prepareStatement(decrementMemberSql)) {
                    ps.setInt(1, personId);
                    ps.executeUpdate();
                }

                conn.commit(); // [Concept: Commit Transaction]
                logger.info("Transaction #{} returned successfully. Fine: ₹{}", transactionId, fineAmount);
                return true;

            } catch (Exception ex) {
                conn.rollback(); // [Concept: Rollback Transaction]
                logger.error("Return item transaction rolled back for tx #{}: {}", transactionId, ex.getMessage());
                if (ex instanceof LibraryException) throw (LibraryException) ex;
                throw new LibraryException("Failed to process book return: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error during return: " + e.getMessage(), e);
        }
    }

    /**
     * Marks an item as lost. Decrements member loan count and reduces total copies.
     */
    public boolean markLost(int transactionId) {
        String findTxSql = "SELECT item_id, person_id, status FROM transactions WHERE id = ?";
        String updateTxSql = "UPDATE transactions SET status = 'LOST' WHERE id = ?";
        String decrementTotalSql = "UPDATE items SET total_copies = MAX(0, total_copies - 1) WHERE id = ?";
        String decrementMemberSql = "UPDATE persons SET active_borrow_count = MAX(0, active_borrow_count - 1) WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int itemId;
                int personId;
                try (PreparedStatement ps = conn.prepareStatement(findTxSql)) {
                    ps.setInt(1, transactionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new ItemNotFoundException("Transaction #" + transactionId, "Transaction not found");
                        itemId = rs.getInt("item_id");
                        personId = rs.getInt("person_id");
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(updateTxSql)) {
                    ps.setInt(1, transactionId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(decrementTotalSql)) {
                    ps.setInt(1, itemId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(decrementMemberSql)) {
                    ps.setInt(1, personId);
                    ps.executeUpdate();
                }

                conn.commit();
                logger.info("Transaction #{} marked as LOST.", transactionId);
                return true;
            } catch (Exception e) {
                conn.rollback();
                throw new LibraryException("Failed marking loan as lost: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new LibraryException("Database error: " + e.getMessage(), e);
        }
    }

    /**
     * Updates overdue fine and status for scanner thread.
     */
    public void updateFineAndStatus(int transactionId, double fine, TransactionStatus status) {
        String sql = "UPDATE transactions SET fine_amount = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDouble(1, fine);
            ps.setString(2, status.name());
            ps.setInt(3, transactionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error updating fine on transaction #{}", transactionId, e);
        }
    }

    @Override
    public Transaction save(Transaction entity) {
        throw new UnsupportedOperationException("Use issueItem() for atomic circulation transactions");
    }

    @Override
    public boolean update(Transaction entity) {
        String sql = "UPDATE transactions SET due_date = ?, fine_amount = ?, status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entity.getDueDate());
            ps.setDouble(2, entity.getFineAmount());
            ps.setString(3, entity.getStatus().name());
            ps.setInt(4, entity.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Error updating transaction", e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Error deleting transaction #" + id, e);
        }
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("item_id"),
                rs.getString("item_title"),
                ItemType.valueOf(rs.getString("item_type")),
                rs.getInt("person_id"),
                rs.getString("member_name"),
                rs.getString("member_email"),
                rs.getString("issue_date"),
                rs.getString("due_date"),
                rs.getString("return_date"),
                rs.getDouble("fine_amount"),
                TransactionStatus.valueOf(rs.getString("status"))
        );
    }
}
