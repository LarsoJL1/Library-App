package dao;

import model.Loan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loan operations, including the JOIN and GROUP BY queries. */
public class LoanDAO {

    // 3-TABLE JOIN: loan + member + book
    private static final String SELECT_LOANS =
            "SELECT l.loan_id, l.member_id, l.book_id, l.checkout_date, l.due_date, " +
            "       l.return_date, m.name AS member_name, b.title AS book_title " +
            "FROM loan l " +
            "JOIN member m ON l.member_id = m.member_id " +
            "JOIN book b   ON l.book_id   = b.book_id ";

    // ---------- CREATE (check out a book) ----------
    public void checkout(int memberId, int bookId, int loanDays) throws SQLException {
        if (!isAvailable(bookId)) {
            throw new IllegalStateException("That book is already checked out.");
        }
        LocalDate today = LocalDate.now();
        String sql = "INSERT INTO loan (member_id, book_id, checkout_date, due_date) " +
                     "VALUES (?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            ps.setString(3, today.toString());
            ps.setString(4, today.plusDays(loanDays).toString());
            ps.executeUpdate();
        }
    }

    // ---------- UPDATE (return a book) ----------
    /** Returns false if the loan doesn't exist or was already returned. */
    public boolean returnBook(int loanId) throws SQLException {
        String sql = "UPDATE loan SET return_date = ? WHERE loan_id = ? AND return_date IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            ps.setInt(2, loanId);
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- READ ----------
    /** All loans that have not been returned yet (3-table JOIN). */
    public List<Loan> findActive() throws SQLException {
        return query(SELECT_LOANS + "WHERE l.return_date IS NULL ORDER BY l.due_date", null);
    }

    /** Unreturned loans whose due date has passed (3-table JOIN). */
    public List<Loan> findOverdue(LocalDate today) throws SQLException {
        return query(SELECT_LOANS +
                "WHERE l.return_date IS NULL AND l.due_date < ? ORDER BY l.due_date", today.toString());
    }

    /** Full borrowing history for one member (3-table JOIN, user-driven filter). */
    public List<Loan> findByMember(int memberId) throws SQLException {
        return query(SELECT_LOANS + "WHERE l.member_id = ? ORDER BY l.checkout_date DESC",
                String.valueOf(memberId));
    }

    public boolean isAvailable(int bookId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM loan WHERE book_id = ? AND return_date IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }

    /**
     * AGGREGATE QUERY: how many loans has each member made?
     * GROUP BY + COUNT + HAVING, with the minimum chosen by the user.
     * Returns member name -> number of loans, most active first.
     */
    public Map<String, Integer> loanCountsPerMember(int minimumLoans) throws SQLException {
        String sql =
                "SELECT m.name, COUNT(*) AS total " +
                "FROM loan l " +
                "JOIN member m ON l.member_id = m.member_id " +
                "GROUP BY m.member_id, m.name " +
                "HAVING COUNT(*) >= ? " +
                "ORDER BY total DESC, m.name";
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minimumLoans);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("name"), rs.getInt("total"));
                }
            }
        }
        return result;
    }

    // ---------- helpers ----------
    private List<Loan> query(String sql, String param) throws SQLException {
        List<Loan> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (param != null) {
                ps.setString(1, param);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String returned = rs.getString("return_date");
                    Loan loan = new Loan(
                            rs.getInt("loan_id"), rs.getInt("member_id"), rs.getInt("book_id"),
                            LocalDate.parse(rs.getString("checkout_date")),
                            LocalDate.parse(rs.getString("due_date")),
                            returned == null ? null : LocalDate.parse(returned));
                    loan.setMemberName(rs.getString("member_name"));
                    loan.setBookTitle(rs.getString("book_title"));
                    list.add(loan);
                }
            }
        }
        return list;
    }
}
