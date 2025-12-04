package com.group3.carrental.dal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.group3.carrental.dbConnection.InitiateDB;
import com.group3.carrental.model.User;

public class UserDAO {

    

    public void createUserinDatabase(String username, String password, String firstname, String lastname, String email) {
        String sql = """
                    INSERT INTO Users (username, email, password, full_name, role)
                    VALUES (?, ?, ?, ?, ?)
                    """;
        String fullname = firstname + lastname;
        try (Connection conn = InitiateDB.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            stmt.setString(3, password);
            stmt.setString(4, fullname); 
            stmt.setString(5, "Customer");
            int rows = stmt.executeUpdate(); //rows affected
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public User findUser(String username, String password, String email) {
    StringBuilder sql = new StringBuilder("SELECT user_id, username, email, password, full_name, phone FROM Users WHERE 1=1"); //1=1 to avoid null inputs (never happens ;v)
    
    if (username != null) sql.append(" AND username=?");
    if (password != null) sql.append(" AND password=?");
    if (email != null) sql.append(" AND email=?");

    try (Connection conn = InitiateDB.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
        int index = 1;
        if (username != null) stmt.setString(index++, username); //post-increment: x++ ;pre-increment: ++x
        if (password != null) stmt.setString(index++, password);
        if (email != null) stmt.setString(index++, email);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new User(
                rs.getInt("user_id"),
                rs.getString("username"),
                rs.getString("email"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getString("phone"),
                "www.facebook.com"
            );
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return null;
    }
}
