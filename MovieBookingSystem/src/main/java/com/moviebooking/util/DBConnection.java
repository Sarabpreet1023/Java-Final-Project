package com.moviebooking.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/movie_booking?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";        // change if different
    private static final String PASS = "root1234";

    // NOTE: simple single Connection for demo. For production use a connection pool.
    private static Connection conn = null;

    public static synchronized Connection getConnection() throws SQLException {
        if (conn == null || conn.isClosed()) {
            try {
                // load driver (if missing this will throw ClassNotFoundException)
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                // wrap as unchecked — this is a fatal misconfiguration (driver missing)
                throw new RuntimeException("MySQL JDBC driver not found. Add the connector JAR to WEB-INF/lib", e);
            }
            conn = DriverManager.getConnection(URL, USER, PASS);
        }
        return conn;
    }
}
