package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import common.Member;

public class MemberRepository {

    // Retrieves all members from DB (with contact info)
    public ArrayList<Member> getAllMembers() {
        ArrayList<Member> membersList = new ArrayList<>();

        String sql =
            "SELECT m.member_id, m.qr_code, m.user_name, m.full_name, c.phone, c.email " +
            "FROM bistro.member m " +
            "LEFT JOIN bistro.member_contact c ON c.member_id = m.member_id";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            if (pConn == null) return null;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                membersList.add(new Member(
                    rs.getString("member_id"),
                    rs.getString("qr_code"),
                    rs.getString("user_name"),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("email")
                ));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            pool.releaseConnection(pConn);
        }

        return membersList;
    }

    // Checks if username exists
    public boolean usernameExists(String userName) {
        String sql = "SELECT 1 FROM bistro.member WHERE user_name = ? LIMIT 1";
        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            if (pConn == null) return false;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, userName);

            ResultSet rs = ps.executeQuery();
            boolean exists = rs.next();

            rs.close();
            ps.close();

            return exists;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            pool.releaseConnection(pConn);
        }
    }
    
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM member_contact WHERE email = ? LIMIT 1";

        try {
            PooledConnection pConn = MySQLConnectionPool.getInstance().getConnection();
            PreparedStatement stmt = pConn.getConnection().prepareStatement(sql);
            stmt.setString(1, email);

            ResultSet rs = stmt.executeQuery();

            boolean exists = rs.next(); // אם יש שורה → קיים

            rs.close();
            stmt.close();
            MySQLConnectionPool.getInstance().releaseConnection(pConn);

            return exists;

        } catch (Exception e) {
            e.printStackTrace();
            return false; // במקרה של שגיאה – נתייחס כ"לא קיים"
        }
    }
    
    public boolean phoneExists(String phone) {
        String sql = "SELECT 1 FROM member_contact WHERE phone = ? LIMIT 1";

        try {
            PooledConnection pConn = MySQLConnectionPool.getInstance().getConnection();
            PreparedStatement stmt = pConn.getConnection().prepareStatement(sql);
            stmt.setString(1, phone);

            ResultSet rs = stmt.executeQuery();

            boolean exists = rs.next();

            rs.close();
            stmt.close();
            MySQLConnectionPool.getInstance().releaseConnection(pConn);

            return exists;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    
    

    // Inserts member + contact (2 inserts)
    public String insertMember(String memberId, String qrCode, String userName, String fullName, String phone, String email) {

        String sqlMember =
            "INSERT INTO bistro.member (member_id, qr_code, user_name, full_name) VALUES (?, ?, ?, ?)";
        String sqlContact =
            "INSERT INTO bistro.member_contact (member_id, phone, email) VALUES (?, ?, ?)";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            if (pConn == null) return "Error: Database Down";

            Connection conn = pConn.getConnection();

            PreparedStatement ps1 = conn.prepareStatement(sqlMember);
            ps1.setString(1, memberId);
            ps1.setString(2, qrCode);
            ps1.setString(3, userName);
            ps1.setString(4, fullName);
            ps1.executeUpdate();
            ps1.close();

            PreparedStatement ps2 = conn.prepareStatement(sqlContact);
            ps2.setString(1, memberId);
            ps2.setString(2, phone);
            ps2.setString(3, email);
            ps2.executeUpdate();
            ps2.close();

            return "OK";

        } catch (SQLException e) {
            return "DB Error: " + e.getMessage();
        } finally {
            pool.releaseConnection(pConn);
        }
    }

    // Updates contact info (member_contact)
    public String updateContact(String memberId, String newPhone, String newEmail) {
        String sql = "UPDATE bistro.member_contact SET phone = ?, email = ? WHERE member_id = ?";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            if (pConn == null) return "Error: Database Down";

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, newPhone);
            ps.setString(2, newEmail);
            ps.setString(3, memberId);

            int updated = ps.executeUpdate();
            ps.close();

            if (updated == 0) return "ERROR: member_id not found";
            return "OK";

        } catch (SQLException e) {
            return "DB Error: " + e.getMessage();
        } finally {
            pool.releaseConnection(pConn);
        }
    }

    // Fetches one member by id (JOIN)
    public Member getMemberById(String memberId) {
        Member member = null;

        String sql =
            "SELECT m.member_id, m.qr_code, m.user_name, m.full_name, c.phone, c.email " +
            "FROM bistro.member m " +
            "LEFT JOIN bistro.member_contact c ON c.member_id = m.member_id " +
            "WHERE m.member_id = ? LIMIT 1";

        MySQLConnectionPool pool = MySQLConnectionPool.getInstance();
        PooledConnection pConn = null;

        try {
            pConn = pool.getConnection();
            if (pConn == null) return null;

            Connection conn = pConn.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, memberId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                member = new Member(
                    rs.getString("member_id"),
                    rs.getString("qr_code"),
                    rs.getString("user_name"),
                    rs.getString("full_name"),
                    rs.getString("phone"),
                    rs.getString("email")
                );
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            pool.releaseConnection(pConn);
        }

        return member;
    }
    
}
