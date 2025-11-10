package com.moviebooking.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;

@WebServlet("/admin/resetShow")
public class ResetShowServlet extends HttpServlet {

    // EDIT THIS if your DB helper class is located elsewhere.
    // The project you showed has com.moviebooking.util.DBConnection
    private Connection getConn() throws Exception {
        return com.moviebooking.util.DBConnection.getConnection();
    }

    // Edit this if your AppContextListener sets a different folder
    private static final String SHOWS_DIR = "C:\\movie-booking\\shows_xml";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String showIdParam = req.getParameter("showId");
        if (showIdParam == null || showIdParam.trim().isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"showId required\"}");
            return;
        }

        int showId;
        try {
            showId = Integer.parseInt(showIdParam);
        } catch (NumberFormatException ex) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"invalid showId\"}");
            return;
        }

        // 1) DB cleanup
        try (Connection conn = getConn()) {
            conn.setAutoCommit(false);
            try (PreparedStatement p1 = conn.prepareStatement(
                    "DELETE p FROM payments p JOIN bookings b ON p.booking_id = b.id WHERE b.show_id = ?")) {
                p1.setInt(1, showId);
                p1.executeUpdate();
            }
            try (PreparedStatement p2 = conn.prepareStatement(
                    "DELETE FROM bookings WHERE show_id = ?")) {
                p2.setInt(1, showId);
                p2.executeUpdate();
            }
            // if you have a seat locks table, try delete, ignore errors if not present
            try (PreparedStatement p3 = conn.prepareStatement(
                    "DELETE FROM seat_locks WHERE show_id = ?")) {
                p3.setInt(1, showId);
                p3.executeUpdate();
            } catch (SQLException ignored) {}

            conn.commit();
        } catch (Exception e) {
            // DB error -> return JSON error
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"DB error: " + escapeJson(e.getMessage()) + "\"}");
            return;
        }

        // 2) Overwrite XML file for the show with default 20-seat layout (A1..A10, B1..B10)
        Path xmlPath = Paths.get(SHOWS_DIR, "show_" + showId + "_seats.xml");
        try {
            Files.createDirectories(xmlPath.getParent());
        } catch (IOException ioe) {
            // cannot create dir
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"Cannot create shows dir: " + escapeJson(ioe.getMessage()) + "\"}");
            return;
        }

        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<show id=\"").append(showId).append("\">\n");
        xml.append("  <rows>\n");
        xml.append("    <row id=\"A\">\n");
        for (int i=1;i<=10;i++) {
            String id = "A" + i;
            // choose price for A3 as example: 250 else 150
            int price = (i==3) ? 250 : 150;
            xml.append("      <seat id=\"").append(id).append("\" status=\"AVAILABLE\" price=\"").append(price).append("\" />\n");
        }
        xml.append("    </row>\n");
        xml.append("    <row id=\"B\">\n");
        for (int i=1;i<=10;i++) {
            String id = "B" + i;
            xml.append("      <seat id=\"").append(id).append("\" status=\"AVAILABLE\" price=\"150\" />\n");
        }
        xml.append("    </row>\n");
        xml.append("  </rows>\n");
        xml.append("</show>\n");

        try {
            Files.write(xmlPath, xml.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ioe) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().print("{\"status\":\"error\",\"message\":\"Cannot write XML: " + escapeJson(ioe.getMessage()) + "\"}");
            return;
        }

        // 3) Success JSON
        resp.setContentType("application/json");
        resp.getWriter().print("{\"status\":\"ok\",\"showId\":" + showId + "}");
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n"," ").replace("\r"," ");
    }
}
