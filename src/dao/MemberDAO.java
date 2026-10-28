package dao;

import model.Member;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Full CRUD for the member table. Every query uses PreparedStatement. */
public class MemberDAO {

    // ---------- CREATE ----------
    public int insert(String name, String email) throws SQLException {
        String sql = "INSERT INTO member (name, email) VALUES (?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    // ---------- READ ----------
    public List<Member> findAll() throws SQLException {
        String sql = "SELECT member_id, name, email FROM member ORDER BY name";
        List<Member> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Member(rs.getInt("member_id"),
                        rs.getString("name"), rs.getString("email")));
            }
        }
        return list;
    }

    /** Returns null if there is no member with that id. */
    public Member findById(int memberId) throws SQLException {
        String sql = "SELECT member_id, name, email FROM member WHERE member_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Member(rs.getInt("member_id"),
                            rs.getString("name"), rs.getString("email"));
                }
                return null;
            }
        }
    }

    // ---------- UPDATE ----------
    public boolean update(Member m) throws SQLException {
        String sql = "UPDATE member SET name = ?, email = ? WHERE member_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getEmail());
            ps.setInt(3, m.getMemberId());
            return ps.executeUpdate() > 0;
        }
    }

    // ---------- DELETE ----------
    /** Fails with a foreign key error if the member still has loans (ON DELETE RESTRICT). */
    public boolean delete(int memberId) throws SQLException {
        String sql = "DELETE FROM member WHERE member_id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            return ps.executeUpdate() > 0;
        }
    }
}
