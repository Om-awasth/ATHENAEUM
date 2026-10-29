package com.library.dao;

import com.library.exception.DuplicateEntryException;
import com.library.exception.LibraryException;
import com.library.model.Librarian;
import com.library.model.Member;
import com.library.model.Person;
import com.library.model.Role;
import com.library.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Persons (Members and Librarians).
 * [Concept: DAO Pattern]
 * [Concept: JDBC - PreparedStatement and SQL Injection Prevention]
 */
public class MemberDAO implements Repository<Person, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(MemberDAO.class);

    @Override
    public Optional<Person> findById(Integer id) {
        String sql = "SELECT * FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPerson(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding person by id: {}", id, e);
        }
        return Optional.empty();
    }

    public Optional<Person> findByEmail(String email) {
        String sql = "SELECT * FROM persons WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPerson(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding person by email: {}", email, e);
        }
        return Optional.empty();
    }

    /**
     * Authenticates user with email and plain password.
     */
    public Optional<Person> authenticate(String email, String plainPassword) {
        Optional<Person> personOpt = findByEmail(email);
        if (personOpt.isPresent()) {
            Person person = personOpt.get();
            if (PasswordUtil.verify(plainPassword, person.getPasswordHash())) {
                return Optional.of(person);
            }
        }
        return Optional.empty();
    }

    @Override
    public List<Person> findAll() {
        String sql = "SELECT * FROM persons ORDER BY name ASC";
        List<Person> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToPerson(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding all persons", e);
        }
        return list;
    }

    public List<Member> findAllMembers() {
        String sql = "SELECT * FROM persons WHERE role = 'MEMBER' ORDER BY name ASC";
        List<Member> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Person p = mapResultSetToPerson(rs);
                if (p instanceof Member m) {
                    list.add(m);
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding all members", e);
        }
        return list;
    }

    @Override
    public Person save(Person person) {
        if (findByEmail(person.getEmail()).isPresent()) {
            throw new DuplicateEntryException("email", person.getEmail(), "Email address is already in use: " + person.getEmail());
        }

        String sql = "INSERT INTO persons (name, email, phone, role, password_hash, membership_id, max_borrow_limit, " +
                     "active_borrow_count, employee_id, shift, department) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, person.getName());
            ps.setString(2, person.getEmail());
            ps.setString(3, person.getPhone());
            ps.setString(4, person.getRole().name());
            ps.setString(5, person.getPasswordHash());

            if (person instanceof Member member) {
                ps.setString(6, member.getMembershipId());
                ps.setInt(7, member.getMaxBorrowLimit());
                ps.setInt(8, member.getActiveBorrowCount());
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.VARCHAR);
            } else if (person instanceof Librarian lib) {
                ps.setNull(6, Types.VARCHAR);
                ps.setNull(7, Types.INTEGER);
                ps.setNull(8, Types.INTEGER);
                ps.setString(9, lib.getEmployeeId());
                ps.setString(10, lib.getShift());
                ps.setString(11, lib.getDepartment());
            }

            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        person.setId(keys.getInt(1));
                    }
                }
            }
            return person;
        } catch (SQLException e) {
            throw new LibraryException("Database error saving person: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Person person) {
        String sql = "UPDATE persons SET name = ?, email = ?, phone = ?, max_borrow_limit = ?, " +
                     "shift = ?, department = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, person.getName());
            ps.setString(2, person.getEmail());
            ps.setString(3, person.getPhone());

            if (person instanceof Member member) {
                ps.setInt(4, member.getMaxBorrowLimit());
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
            } else if (person instanceof Librarian lib) {
                ps.setNull(4, Types.INTEGER);
                ps.setString(5, lib.getShift());
                ps.setString(6, lib.getDepartment());
            } else {
                ps.setNull(4, Types.INTEGER);
                ps.setNull(5, Types.VARCHAR);
                ps.setNull(6, Types.VARCHAR);
            }

            ps.setInt(7, person.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error updating person: " + e.getMessage(), e);
        }
    }

    /**
     * Increments or decrements active borrow count safely.
     */
    public boolean updateActiveBorrowCount(int memberId, int delta) {
        String sql = "UPDATE persons SET active_borrow_count = MAX(0, active_borrow_count + ?) WHERE id = ? AND role = 'MEMBER'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, delta);
            ps.setInt(2, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Error adjusting borrow count for member #" + memberId, e);
        }
    }

    @Override
    public boolean deleteById(Integer id) {
        String sql = "DELETE FROM persons WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new LibraryException("Database error deleting person #" + id, e);
        }
    }

    private Person mapResultSetToPerson(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String phone = rs.getString("phone");
        String roleStr = rs.getString("role");
        String passwordHash = rs.getString("password_hash");

        Role role = Role.valueOf(roleStr);

        if (role == Role.MEMBER) {
            return new Member(id, name, email, phone, passwordHash,
                    rs.getString("membership_id"),
                    rs.getInt("max_borrow_limit"),
                    rs.getInt("active_borrow_count"),
                    rs.getString("join_date"));
        } else {
            return new Librarian(id, name, email, phone, passwordHash,
                    rs.getString("employee_id"),
                    rs.getString("shift"),
                    rs.getString("department"));
        }
    }
}
