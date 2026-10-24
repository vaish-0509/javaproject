package com.kec.academic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String URL = getenvOr("DB_URL",
            "jdbc:mysql://localhost:3306/kec_academic");
    private static final String USER = getenvOr("DB_USER", "root");
    private static final String PASSWORD = getenvOr("DB_PASSWORD", "vaishu@05");

    private static String getenvOr(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isEmpty()) ? fallback : value;
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    public static void main(String[] args) {
        try (Connection con = getConnection()) {
            System.out.println("Connected to database");
        } catch (SQLException e) {
            System.out.println("Failed: " + e.getMessage());
        }
    }
}