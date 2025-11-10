package com.moviebooking.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.ServletContext;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.time.*;
import java.util.stream.*;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

@WebServlet("/confirmBooking")
public class ConfirmBookingServlet extends HttpServlet {

    // If you later add DB booking persistence, replace simulation with real logic
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        String showId = req.getParameter("showId");
        String lockToken = req.getParameter("lockToken");
        String cardNumber = req.getParameter("cardNumber"); // simulated usage
        String cardName = req.getParameter("cardName");

        if (lockToken == null || lockToken.trim().isEmpty()) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"status\":\"error\",\"message\":\"Missing lockToken - reserve seats first\"}");
            return;
        }

        // (Optional) Basic simulated payment validation
        if (cardNumber == null || cardNumber.trim().length() < 6) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"status\":\"error\",\"message\":\"Invalid card details\"}");
            return;
        }

        // locate XML
        ServletContext ctx = getServletContext();
        String dir = (String) ctx.getAttribute("SHOWS_XML_DIR");
        if (dir == null || dir.isEmpty()) {
            sendJson(resp, 500, "{\"status\":\"error\",\"message\":\"Server misconfigured: SHOWS_XML_DIR not set\"}");
            return;
        }

        Path xmlPath = Paths.get(dir, "show_" + (showId==null? "": showId) + "_seats.xml");
        if (!Files.exists(xmlPath)) {
            sendJson(resp, 404, "{\"status\":\"error\",\"message\":\"Seat map file not found\"}");
            return;
        }

        // synchronize on the show lock object to avoid races with lockSeats
        Object showLock = LockSeatsServlet.class; // coarse lock - good enough for single JVM dev
        List<String> bookedSeats = new ArrayList<>();
        String bookingId = "BKG-" + System.currentTimeMillis();

        synchronized (showLock) {
            try (InputStream is = Files.newInputStream(xmlPath)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringComments(true);
                dbf.setIgnoringElementContentWhitespace(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(is);
                doc.getDocumentElement().normalize();

                NodeList locksList = doc.getElementsByTagName("locks");
                if (locksList.getLength() == 0) {
                    sendJson(resp, 409, "{\"status\":\"error\",\"message\":\"No active locks\"}");
                    return;
                }
                Element locksEl = (Element) locksList.item(0);

                // find lock node with token
                NodeList lockNodes = locksEl.getElementsByTagName("lock");
                Element targetLock = null;
                for (int i = 0; i < lockNodes.getLength(); i++) {
                    Element l = (Element) lockNodes.item(i);
                    if (lockToken.equals(l.getAttribute("token"))) {
                        targetLock = l;
                        break;
                    }
                }
                if (targetLock == null) {
                    sendJson(resp, 409, "{\"status\":\"error\",\"message\":\"Invalid or expired lock\"}");
                    return;
                }

                // check expiry if present
                String expiry = targetLock.getAttribute("expires");
                if (expiry != null && !expiry.isEmpty()) {
                    Instant exp = Instant.parse(expiry);
                    if (Instant.now().isAfter(exp)) {
                        sendJson(resp, 409, "{\"status\":\"error\",\"message\":\"Lock expired\"}");
                        return;
                    }
                }

                // parse seats CSV inside lock node
                String seatsCsv = targetLock.getTextContent();
                List<String> seats = Arrays.stream(seatsCsv.split(","))
                                           .map(String::trim)
                                           .filter(s -> !s.isEmpty())
                                           .collect(Collectors.toList());
                if (seats.isEmpty()) {
                    sendJson(resp, 400, "{\"status\":\"error\",\"message\":\"No seats found in lock\"}");
                    return;
                }

                // mark those seat elements as BOOKED
                NodeList seatNodes = doc.getElementsByTagName("seat");
                if (seatNodes.getLength() == 0) seatNodes = doc.getElementsByTagName("Seat");
                Set<String> target = seats.stream().map(String::toUpperCase).collect(Collectors.toSet());
                for (int i = 0; i < seatNodes.getLength(); i++) {
                    Element s = (Element) seatNodes.item(i);
                    String id = s.getAttribute("id");
                    if (id != null && target.contains(id.trim().toUpperCase())) {
                        s.setAttribute("status", "BOOKED");
                        bookedSeats.add(id.trim());
                    }
                }

                // remove the lock node
                locksEl.removeChild(targetLock);

                // write back atomically
                Path tmp = Files.createTempFile(xmlPath.getParent(), "confirmtmp-", ".xml");
                try (OutputStream os = Files.newOutputStream(tmp)) {
                    TransformerFactory tf = TransformerFactory.newInstance();
                    Transformer t = tf.newTransformer();
                    t.setOutputProperty(OutputKeys.INDENT, "yes");
                    t.transform(new DOMSource(doc), new StreamResult(os));
                }
                try {
                    Files.move(tmp, xmlPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(tmp, xmlPath, StandardCopyOption.REPLACE_EXISTING);
                }

            } catch (Exception ex) {
                ex.printStackTrace();
                sendJson(resp, 500, "{\"status\":\"error\",\"message\":\"Server error: " + escape(ex.getMessage()) + "\"}");
                return;
            }
        } // end synchronized

        // success response
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\":\"success\"");
        sb.append(",\"bookingId\":\"").append(bookingId).append("\"");
        sb.append(",\"seats\":").append(jsonArray(bookedSeats));
        sb.append("}");
        sendJson(resp, 200, sb.toString());
    }

    private void sendJson(HttpServletResponse resp, int code, String body) throws IOException {
        resp.setStatus(code);
        try (PrintWriter w = resp.getWriter()) {
            w.print(body);
            w.flush();
        }
    }

    private String jsonArray(Collection<String> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (String s : items) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(escape(s)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
