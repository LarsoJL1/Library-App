package dao;

import model.Book;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Full CRUD for the book table, plus a user-driven search. */
public class BookDAO {

    // Shared SELECT: JOIN to category for the name, and a subquery for availability.
    private static final String SELECT_BOOKS =
            "SELECT b.book_id, b.title, b.author, b.category_id, c.name AS category_name, " +
            "  (SELECT COUNT(*) FROM loan l " +
            "    WHERE l.book_id = b.book_id AND l.return_date IS NULL) AS active_loans " +
            "FROM book b " +
            "LEFT JOIN category c ON b.category_id = c.category_id ";

    // ---------- CREATE ----------
    public int insert(String title, String author, Integer categoryId) throws SQLException {
        String sql = "INSERT INTO book (title, author, category_id) VALUES (?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, author);
            if (categoryId == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, categoryId);
            }
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    // ---------- READ ----------
    public List<Book> findAll() throws SQLException {
        return query(SELECT_BOOKS + "ORDER BY b.title", null);
    }

    /** Search by title OR author, driven by user input. Sorted by title. */
    public List<Book> search(String keyword) throws SQLException {
        String like = "%" + keyword + "%";
        return query(SELECT_BOOKS +
                "WHERE b.title LIKE ? OR b.author LIKE ? ORDER BY b.title", like);
    }

    /** Returns null if there is no book with that id. */
    public Book findById(int bookId) throws SQLException {
        String sql = SELECT_BOOKS + "WHERE b.book_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    // ---------- UPDATE ----------
    public boolean update(Book b) throws SQLException {
        String sql = "UPDATE book SET title = ?, author = ?, category_id = ? WHERE book_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            if (b.getCategoryId() == null) {
                ps.setNull(3, Types.INTEGER);
            } else {
                ps.setInt(3, b.getCategoryId());
            }
            ps.setInt(4, b.getBookId());
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- DELETE ----------
    /** Fails with a foreign key error if the book has loan history (ON DELETE RESTRICT). */
    public boolean delete(int bookId) throws SQLException {
        String sql = "DELETE FROM book WHERE book_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- helpers ----------
    private List<Book> query(String sql, String likeParam) throws SQLException {
        List<Book> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (likeParam != null) {
                ps.setString(1, likeParam);
                ps.setString(2, likeParam);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        int cat = rs.getInt("category_id");
        Integer categoryId = rs.wasNull() ? null : cat;
        Book b = new Book(rs.getInt("book_id"), rs.getString("title"),
                rs.getString("author"), categoryId);
        b.setCategoryName(rs.getString("category_name"));
        b.setAvailable(rs.getInt("active_loans") == 0);
        return b;
    }
}
