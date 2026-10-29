package com.library.dao;

import com.library.exception.DuplicateEntryException;
import com.library.exception.LibraryException;
import com.library.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for LibraryItem and its polymorphic subclasses (Book, Magazine, ReferenceBook, Journal).
 * Demonstrates DAO pattern, PreparedStatement, ResultSet mapping, Method Overloading, and polymorphism.
 *
 * [Concept: DAO Pattern]
 * [Concept: JDBC - PreparedStatement and ResultSet]
 * [Concept: Polymorphism in Data Persistence]
 * [Concept: Method Overloading]
 */
public class BookDAO implements Repository<LibraryItem, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(BookDAO.class);

    @Override
    public Optional<LibraryItem> findById(Integer id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        // [Concept: Try-with-resources with PreparedStatement]
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving library item by id: {}", id, e);
        }
        return Optional.empty();
    }

    public Optional<Book> findByIsbn(String isbn) {
        String sql = "SELECT * FROM items WHERE isbn = ? AND item_type = 'BOOK'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LibraryItem item = mapResultSetToItem(rs);
                    if (item instanceof Book book) {
                        return Optional.of(book);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving book by ISBN: {}", isbn, e);
        }
        return Optional.empty();
    }

    @Override
    public List<LibraryItem> findAll() {
        String sql = "SELECT * FROM items ORDER BY title ASC";
        List<LibraryItem> items = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error querying all library items", e);
        }
        return items;
    }

    // =========================================================================
    // [Concept: Method Overloading] - Three variations of search()
    // =========================================================================

    /**
     * [Concept: Method Overloading - Variant 1] Searches items by title substring.
     */
    public List<LibraryItem> search(String title) {
        String sql = "SELECT * FROM items WHERE title LIKE ? ORDER BY title ASC";
        List<LibraryItem> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + (title != null ? title.trim() : "") + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error in search(title)", e);
        }
        return results;
    }

    /**
     * [Concept: Method Overloading - Variant 2] Searches items by title and author.
     */
    public List<LibraryItem> search(String title, String author) {
        String sql = "SELECT * FROM items WHERE title LIKE ? AND (author LIKE ? OR author IS NULL) ORDER BY title ASC";
        List<LibraryItem> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + (title != null ? title.trim() : "") + "%");
            ps.setString(2, "%" + (author != null ? author.trim() : "") + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error in search(title, author)", e);
        }
        return results;
    }

    /**
     * [Concept: Method Overloading - Variant 3] Searches items by title, category/genre, and availability flag.
     */
    public List<LibraryItem> search(String title, String category, boolean availableOnly) {
        StringBuilder sql = new StringBuilder("SELECT * FROM items WHERE 1=1 ");
        if (title != null && !title.isBlank()) {
            sql.append("AND (title LIKE ? OR author LIKE ? OR isbn LIKE ?) ");
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
            sql.append("AND (genre = ? OR category = ? OR item_type = ?) ");
        }
        if (availableOnly) {
            sql.append("AND available_copies > 0 ");
        }
        sql.append("ORDER BY title ASC");

        List<LibraryItem> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (title != null && !title.isBlank()) {
                String term = "%" + title.trim() + "%";
                ps.setString(paramIndex++, term);
                ps.setString(paramIndex++, term);
                ps.setString(paramIndex++, term);
            }
            if (category != null && !category.isBlank() && !category.equalsIgnoreCase("ALL")) {
                ps.setString(paramIndex++, category.trim());
                ps.setString(paramIndex++, category.trim());
                ps.setString(paramIndex++, category.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error in search(title, category, availableOnly)", e);
        }
        return results;
    }

    // =========================================================================
    // CRUD Operations
    // =========================================================================

    @Override
    public LibraryItem save(LibraryItem item) {
        // Check for duplicate ISBN if it's a book
        if (item instanceof Book book && book.getIsbn() != null && !book.getIsbn().isBlank()) {
            Optional<Book> existing = findByIsbn(book.getIsbn());
            if (existing.isPresent()) {
                throw new DuplicateEntryException("ISBN", book.getIsbn(), "A book with ISBN " + book.getIsbn() + " already exists");
            }
        }

        String sql = "INSERT INTO items (item_type, title, publisher, year, total_copies, available_copies, " +
                     "isbn, author, genre, issue_number, month, category, field_of_study, edition, dewey_decimal, " +
                     "volume, peer_reviewed, impact_factor) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            populateItemPreparedStatement(ps, item);

            int affectedRows = ps.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        item.setId(generatedKeys.getInt(1));
                    }
                }
            }
            return item;
        } catch (SQLException e) {
            throw new LibraryException("Database error saving item: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(LibraryItem item) {
        String sql = "UPDATE items SET item_type = ?, title = ?, publisher = ?, year = ?, total_copies = ?, available_copies = ?, " +
                     "isbn = ?, author = ?, genre = ?, issue_number = ?, month = ?, category = ?, " +
                     "field_of_study = ?, edition = ?, dewey_decimal = ?, volume = ?, peer_reviewed = ?, impact_factor = ? " +
                     "WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            populateItemPreparedStatement(ps, item);
            ps.setInt(19, item.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error updating item: " + e.getMessage(), e);
        }
    }

    public boolean updateCopies(int itemId, int totalCopies, int availableCopies) {
        String sql = "UPDATE items SET total_copies = ?, available_copies = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, totalCopies);
            ps.setInt(2, availableCopies);
            ps.setInt(3, itemId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Error updating copy count for item #" + itemId, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error deleting item #" + id, e);
        }
    }

    // Helper: Map ResultSet to concrete subtype of LibraryItem polymorphically
    private LibraryItem mapResultSetToItem(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String typeStr = rs.getString("item_type");
        String title = rs.getString("title");
        String publisher = rs.getString("publisher");
        int year = rs.getInt("year");
        int totalCopies = rs.getInt("total_copies");
        int availableCopies = rs.getInt("available_copies");

        ItemType type = ItemType.valueOf(typeStr);

        return switch (type) {
            case BOOK -> new Book(id, title, publisher, year, totalCopies, availableCopies,
                    rs.getString("isbn"), rs.getString("author"), rs.getString("genre"));
            case MAGAZINE -> new Magazine(id, title, publisher, year, totalCopies, availableCopies,
                    rs.getInt("issue_number"), rs.getString("month"), rs.getString("category"));
            case REFERENCE -> new ReferenceBook(id, title, publisher, year, totalCopies, availableCopies,
                    rs.getString("field_of_study"), rs.getString("edition"), rs.getString("dewey_decimal"));
            case JOURNAL -> new Journal(id, title, publisher, year, totalCopies, availableCopies,
                    rs.getString("volume"), rs.getInt("peer_reviewed") == 1, rs.getDouble("impact_factor"));
        };
    }

    // Helper: Populate PreparedStatement parameters
    private void populateItemPreparedStatement(PreparedStatement ps, LibraryItem item) throws SQLException {
        ps.setString(1, item.getItemType().name());
        ps.setString(2, item.getTitle());
        ps.setString(3, item.getPublisher());
        ps.setInt(4, item.getYear());
        ps.setInt(5, item.getTotalCopies());
        ps.setInt(6, item.getAvailableCopies());

        // Subclass-specific columns
        if (item instanceof Book book) {
            ps.setString(7, book.getIsbn());
            ps.setString(8, book.getAuthor());
            ps.setString(9, book.getGenre());
            ps.setNull(10, Types.INTEGER);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
            ps.setNull(13, Types.VARCHAR);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.VARCHAR);
            ps.setNull(16, Types.VARCHAR);
            ps.setNull(17, Types.INTEGER);
            ps.setNull(18, Types.DOUBLE);
        } else if (item instanceof Magazine mag) {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setInt(10, mag.getIssueNumber());
            ps.setString(11, mag.getMonth());
            ps.setString(12, mag.getCategory());
            ps.setNull(13, Types.VARCHAR);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.VARCHAR);
            ps.setNull(16, Types.VARCHAR);
            ps.setNull(17, Types.INTEGER);
            ps.setNull(18, Types.DOUBLE);
        } else if (item instanceof ReferenceBook ref) {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.INTEGER);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
            ps.setString(13, ref.getFieldOfStudy());
            ps.setString(14, ref.getEdition());
            ps.setString(15, ref.getDeweyDecimal());
            ps.setNull(16, Types.VARCHAR);
            ps.setNull(17, Types.INTEGER);
            ps.setNull(18, Types.DOUBLE);
        } else if (item instanceof Journal jnl) {
            ps.setNull(7, Types.VARCHAR);
            ps.setNull(8, Types.VARCHAR);
            ps.setNull(9, Types.VARCHAR);
            ps.setNull(10, Types.INTEGER);
            ps.setNull(11, Types.VARCHAR);
            ps.setNull(12, Types.VARCHAR);
            ps.setNull(13, Types.VARCHAR);
            ps.setNull(14, Types.VARCHAR);
            ps.setNull(15, Types.VARCHAR);
            ps.setString(16, jnl.getVolume());
            ps.setInt(17, jnl.isPeerReviewed() ? 1 : 0);
            ps.setDouble(18, jnl.getImpactFactor());
        }
    }
}
