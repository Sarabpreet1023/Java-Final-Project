package com.moviebooking.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletContext;
import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;

@WebServlet("/seatmap")
public class SeatMapServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String showId = req.getParameter("showId");
        if (showId == null || showId.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing showId");
            return;
        }

        ServletContext ctx = getServletContext();
        String dir = (String) ctx.getAttribute("SHOWS_XML_DIR");
        if (dir == null || dir.trim().isEmpty()) {
            // fallback if AppContextListener wasn't set or different path
            dir = "C:\\Users\\komda\\shows_xml";
        }

        String fileName = "show_" + showId + "_seats.xml";
        File xmlFile = new File(dir, fileName);
        if (!xmlFile.exists()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Seat map file not found: " + xmlFile.getAbsolutePath());
            return;
        }

        // parse XML and build rows structure
        List<Map<String,Object>> rows = new ArrayList<>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList rowNodes = doc.getElementsByTagName("row");
            for (int i = 0; i < rowNodes.getLength(); i++) {
                Node rn = rowNodes.item(i);
                if (rn.getNodeType() != Node.ELEMENT_NODE) continue;
                Element rowEl = (Element) rn;
                String rowId = rowEl.getAttribute("id");

                List<Map<String,Object>> seats = new ArrayList<>();
                NodeList seatNodes = rowEl.getElementsByTagName("seat");
                for (int j = 0; j < seatNodes.getLength(); j++) {
                    Node sn = seatNodes.item(j);
                    if (sn.getNodeType() != Node.ELEMENT_NODE) continue;
                    Element seatEl = (Element) sn;
                    String seatId = seatEl.getAttribute("id");
                    String status = seatEl.getAttribute("status");
                    String priceStr = seatEl.getAttribute("price");
                    double price = 0;
                    try { price = Double.parseDouble(priceStr); } catch (Exception ex) { /* ignore */ }

                    Map<String,Object> seatMap = new HashMap<>();
                    seatMap.put("id", seatId);
                    seatMap.put("status", status != null ? status.toUpperCase() : "AVAILABLE");
                    seatMap.put("price", price);
                    seats.add(seatMap);
                }

                Map<String,Object> rowMap = new HashMap<>();
                rowMap.put("rowId", rowId);
                rowMap.put("seats", seats);
                rows.add(rowMap);
            }

        } catch (Exception e) {
            // log and return server error
            e.printStackTrace();
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to parse seat map XML: " + e.getMessage());
            return;
        }

        // set attributes for JSP and forward
        req.setAttribute("showId", showId);
        req.setAttribute("rows", rows);

        // Forward to JSP (seatmap.jsp in webapp root or /WEB-INF as you have)
        req.getRequestDispatcher("/seatmap.jsp").forward(req, resp);
    }
}
