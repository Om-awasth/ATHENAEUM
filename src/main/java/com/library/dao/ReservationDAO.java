package com.library.dao;

import com.library.exception.LibraryException;
import com.library.model.Reservation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for reservation hold queue.
 * [Concept: DAO Pattern]
 * [Concept: FIFO Queue Persistence]
 */
public class ReservationDAO {

    private static final Logger logger = LoggerFactory.getLogger(ReservationDAO.class);

    public List<Reservation> findAll() {
        String sql = "SELECT r.*, i.title AS item_title, p.name AS member_name, p.email AS member_email " +
                     "FROM reservations r " +
                     "JOIN items i ON r.item_id = i.id " +
                     "JOIN persons p ON r.person_id = p.id " +
                     "ORDER BY r.status ASC, r.queue_position ASC";

        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToReservation(rs));
            }
        } catch (SQLException e) {
            logger.error("Error retrieving reservations", e);
        }
        return list;
    }

    public List<Reservation> findByItem(int itemId) {
        String sql = "SELECT r.*, i.title AS item_title, p.name AS member_name, p.email AS member_email " +
                     "FROM reservations r " +
                     "JOIN items i ON r.item_id = i.id " +
                     "JOIN persons p ON r.person_id = p.id " +
                     "WHERE r.item_id = ? AND r.status = 'WAITING' " +
                     "ORDER BY r.queue_position ASC";

        List<Reservation> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToReservation(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error retrieving reservations for item #{}", itemId, e);
        }
        return list;
    }

    public Reservation addReservation(int itemId, int personId) {
        String countSql = "SELECT COALESCE(MAX(queue_position), 0) + 1 AS next_pos FROM reservations WHERE item_id = ? AND status = 'WAITING'";
        String insertSql = "INSERT INTO reservations (item_id, person_id, reservation_date, status, queue_position) VALUES (?, ?, ?, 'WAITING', ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            int nextPos = 1;
            try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                ps.setInt(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) nextPos = rs.getInt("next_pos");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, itemId);
                ps.setInt(2, personId);
                ps.setString(3, LocalDate.now().toString());
                ps.setInt(4, nextPos);
                ps.executeUpdate();

                int id = 0;
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) id = keys.getInt(1);
                }
                return new Reservation(id, itemId, "", personId, "", "", LocalDate.now().toString(), "WAITING", nextPos);
            }
        } catch (SQLException e) {
            throw new LibraryException("Failed adding reservation: " + e.getMessage(), e);
        }
    }

    public boolean updateStatus(int reservationId, String status) {
        String sql = "UPDATE reservations SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, reservationId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Failed updating reservation status", e);
        }
    }

    private Reservation mapResultSetToReservation(ResultSet rs) throws SQLException {
        return new Reservation(
                rs.getInt("id"),
                rs.getInt("item_id"),
                rs.getString("item_title"),
                rs.getInt("person_id"),
                rs.getString("member_name"),
                rs.getString("member_email"),
                rs.getString("reservation_date"),
                rs.getString("status"),
                rs.getInt("queue_position")
        );
    }
}
