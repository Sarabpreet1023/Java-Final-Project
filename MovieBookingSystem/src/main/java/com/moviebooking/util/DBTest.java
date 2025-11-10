package com.moviebooking.util;

import java.sql.*;

public class DBTest {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/movie_booking?serverTimezone=UTC";
        String user = "root";                 
        String pass = "root1234";  

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            try (Connection con = DriverManager.getConnection(url, user, pass)) {
                System.out.println("✅ Connected to DB: " + con.getCatalog());
                try (Statement s = con.createStatement();
                     ResultSet rs = s.executeQuery("SELECT id, title FROM movies")) {
                    while (rs.next()) {
                        System.out.println("Movie: " + rs.getInt("id") + " - " + rs.getString("title"));
                    }
                } catch (SQLException inner) {
                    System.out.println("Note: movies table may be empty or missing.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
