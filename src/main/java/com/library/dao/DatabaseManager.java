package com.library.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages SQLite database connection lifecycle, schema migrations, and batch seeding.
 * [Concept: JDBC - Connection Management]
 * [Concept: JDBC - Batch Processing with Statement]
 * [Concept: Try-with-resources]
 */
public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:library.db";
    private static String dbUrl = DEFAULT_DB_URL;

    // Static initialization
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load SQLite JDBC driver", e);
        }
    }

    private DatabaseManager() {
    }

    public static synchronized void setDatabaseUrl(String customUrl) {
        dbUrl = customUrl;
    }

    /**
     * Obtains a new JDBC connection to the SQLite database.
     *
     * @return Connection instance
     * @throws SQLException on connection failure
     */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        // Enable foreign key enforcement in SQLite
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Initializes tables and seeds initial catalog data if empty.
     * Demonstrates try-with-resources and batch statement execution.
     * [Concept: Try-with-resources]
     * [Concept: Batch Processing with executeBatch]
     */
    public static synchronized void initializeDatabase() {
        logger.info("Initializing database at: {}", dbUrl);

        // 1. Execute schema.sql
        executeSqlScript("schema.sql");

        // 2. Check if inventory is empty; if so, populate via seed.sql using batch inserts
        boolean needSeed = false;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM items")) {

            if (rs.next() && rs.getInt("total") == 0) {
                needSeed = true;
            }
        } catch (SQLException e) {
            logger.error("Error verifying database initialization status", e);
        }

        if (needSeed) {
            logger.info("Items table is empty. Executing seed data batch...");
            executeSqlScriptBatch("seed.sql");
            logger.info("Database successfully seeded with realistic holdings and circulation data.");
        } else {
            logger.info("Database already contains records. Skipping seed execution.");
        }
    }

    /**
     * Executes individual SQL statements from a file or classpath resource.
     */
    private static void executeSqlScript(String scriptFileName) {
        List<String> statements = readSqlStatements(scriptFileName);
        // [Concept: Try-with-resources] Automatically closes Connection and Statement
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            for (String sql : statements) {
                if (!sql.isBlank()) {
                    stmt.execute(sql);
                }
            }
        } catch (SQLException e) {
            logger.error("Failed executing SQL script: {}", scriptFileName, e);
        }
    }

    /**
     * [Concept: JDBC - Batch Processing]
     * Groups multiple INSERT statements into a batch for single round-trip execution.
     */
    private static void executeSqlScriptBatch(String scriptFileName) {
        List<String> statements = readSqlStatements(scriptFileName);
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            conn.setAutoCommit(false); // [Concept: Transaction Management]
            try {
                for (String sql : statements) {
                    if (!sql.isBlank()) {
                        stmt.addBatch(sql); // [Concept: Batch Processing]
                    }
                }
                int[] results = stmt.executeBatch(); // [Concept: Batch Processing]
                conn.commit();
                logger.info("Batch execution completed. Processed {} statements.", results.length);
            } catch (SQLException ex) {
                conn.rollback(); // [Concept: Transaction Rollback]
                logger.error("Failed batch insert. Rolled back transaction.", ex);
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("SQL error during batch seed script execution: {}", scriptFileName, e);
        }
    }

    /**
     * Reads SQL statements separated by semicolons from classpath or file system.
     */
    private static List<String> readSqlStatements(String fileName) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try {
            // First attempt to load from working directory, then classpath
            Path filePath = Paths.get(fileName);
            BufferedReader reader;
            if (Files.exists(filePath)) {
                reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
            } else {
                InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(fileName);
                if (in == null) {
                    logger.warn("Could not find script file: {}", fileName);
                    return statements;
                }
                reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            }

            try (reader) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Strip inline comment if present
                    int commentIdx = line.indexOf("--");
                    if (commentIdx != -1) {
                        line = line.substring(0, commentIdx);
                    }
                    String trimmed = line.trim();
                    if (trimmed.isEmpty()) {
                        continue; // Skip comments and blank lines
                    }
                    current.append(line).append(" ");
                    if (trimmed.endsWith(";")) {
                        statements.add(current.toString().trim());
                        current.setLength(0);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error reading SQL script: {}", fileName, e);
        }

        return statements;
    }
}
