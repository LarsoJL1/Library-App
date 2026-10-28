package dao;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Opens connections and (re)builds the database from schema.sql + seed.sql.
 * Every DAO gets its connection from here, so foreign keys are always on.
 */
public class Database {
    private static final String URL = "jdbc:sqlite:"
            + System.getProperty("library.database", "library.db");

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // SQLite ignores foreign keys unless this is run on EVERY connection.
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        return conn;
    }

    /** Builds the database the first time the app runs. */
    public static void ensureReady() throws SQLException, IOException {
        boolean ready;
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'loan'")) {
            ready = rs.next();
        }
        if (ready) {
            try (Connection conn = getConnection()) {
                ensureDefaultCategories(conn);
            }
            return;
        }
        rebuild();
    }

    private static void ensureDefaultCategories(Connection conn) throws SQLException {
        String[] names = {"Manga", "Romance", "Fantasy", "Horror", "Biography", "Poetry",
                "Children's", "Nonfiction"};
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR IGNORE INTO category (name) VALUES (?)")) {
            for (String name : names) {
                ps.setString(1, name);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Drops everything and reloads schema.sql and seed.sql. */
    public static void rebuild() throws SQLException, IOException {
        try (Connection conn = getConnection()) {
            runScript(conn, "schema.sql");
            runScript(conn, "seed.sql");
        }
    }

    private static void runScript(Connection conn, String fileName)
            throws SQLException, IOException {
        String text = new String(Files.readAllBytes(Paths.get(fileName)), StandardCharsets.UTF_8);

        // Remove "--" comments, then split into statements on ";"
        StringBuilder cleaned = new StringBuilder();
        for (String line : text.split("\n")) {
            int comment = line.indexOf("--");
            if (comment >= 0) {
                line = line.substring(0, comment);
            }
            cleaned.append(line).append("\n");
        }

        try (Statement st = conn.createStatement()) {
            for (String sql : cleaned.toString().split(";")) {
                if (!sql.trim().isEmpty()) {
                    st.execute(sql);
                }
            }
        }
    }
}
