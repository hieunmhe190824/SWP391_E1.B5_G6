package com.group3.carrental.dbConnection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class InitiateDB {
    private static final String URL = "jdbc:mysql://localhost:3306/carrental_project";
    private static final String USER = "root";
    private static final String PASS = getEnv();

    private InitiateDB() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static String getEnv() {
        String password = System.getenv("MYSQL_PASS");
        if (password != null) {
            return password;
        } else {
            return null;
        }
    }
}

