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

@WebServlet("/lockSeats")
public class LockSeatsServlet extends HttpServlet {

    // per-show lock map (concurrent)
    private static final Map<String, Object> showLocks = new java.util.concurrent.ConcurrentHashMap<>();

    // lock duration (seconds)
    private static final long LOCK_SECONDS = 5 * 60;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        String showId = req.getParameter("showId");
        String[] seatParams = req.getParameterValues("seat"); // seat=A1&seat=A2...
        List<String> requestedSeats = seatParams == null ? Collections.emptyList() : Arrays.asList(seatParams);

        // basic validation
        if (showId == null || showId.isEmpty()) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"status\":\"error\",\"message\":\"Missing showId\"}");
            return;
        }
        if (requestedSeats.isEmpty()) {
            sendJson(resp, HttpServletResponse.SC_BAD_REQUEST, "{\"status\":\"error\",\"message\":\"No seats requested\"}");
            return;
        }

        ServletContext ctx = getServletContext();
        String dir = (String) ctx.getAttribute("SHOWS_XML_DIR");
        if (dir == null || dir.isEmpty()) {
            sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "{\"status\":\"error\",\"message\":\"Server misconfigured: SHOWS_XML_DIR not set\"}");
            return;
        }

        Path xmlPath = Paths.get(dir, "show_" + showId + "_seats.xml");
        if (!Files.exists(xmlPath)) {
            sendJson(resp, HttpServletResponse.SC_NOT_FOUND,
                    "{\"status\":\"error\",\"message\":\"Seat map file not found: " + xmlPath.toString() + "\"}");
            return;
        }

        // per-show lock object
        Object showLock = showLocks.computeIfAbsent(showId, k -> new Object());

        List<String> lockedNow = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        String lockToken = null;
        String expiryIso = null;

        synchronized (showLock) {
            try (InputStream is = Files.newInputStream(xmlPath)) {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setIgnoringComments(true);
                dbf.setIgnoringElementContentWhitespace(true);
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(is);
                doc.getDocumentElement().normalize();

                // Build seat lookup map (case-insensitive)
                Map<String, Element> seatMap = new HashMap<>();
                NodeList seatNodes = doc.getElementsByTagName("seat");
                if (seatNodes.getLength() == 0) seatNodes = doc.getElementsByTagName("Seat");
                for (int i = 0; i < seatNodes.getLength(); i++) {
                    Element s = (Element) seatNodes.item(i);
                    String id = s.getAttribute("id");
                    if (id != null) seatMap.put(id.trim().toUpperCase(), s);
                }

                // normalize requested seat IDs and check
                for (String rawSeatId : requestedSeats) {
                    if (rawSeatId == null) continue;
                    String key = rawSeatId.trim().toUpperCase();
                    Element seatEl = seatMap.get(key);
                    if (seatEl == null) {
                        unavailable.add(rawSeatId);
                        continue;
                    }
                    String status = seatEl.getAttribute("status");
                    if (status == null) status = "";
                    status = status.trim().toUpperCase();
                    if ("AVAILABLE".equals(status)) {
                        // lock it
                        seatEl.setAttribute("status", "LOCKED");
                        lockedNow.add(key); // store as uppercase
                    } else {
                        unavailable.add(key);
                    }
                }

                if (!lockedNow.isEmpty()) {
                    // create lock token and expiry
                    lockToken = UUID.randomUUID().toString();
                    expiryIso = Instant.now().plusSeconds(LOCK_SECONDS).toString();

                    // create / append <locks><lock ...> CSV </lock></locks>
                    Element root = doc.getDocumentElement();
                    Element locksEl = null;
                    NodeList locksList = doc.getElementsByTagName("locks");
                    if (locksList.getLength() > 0) locksEl = (Element) locksList.item(0);
                    else {
                        locksEl = doc.createElement("locks");
                        root.appendChild(locksEl);
                    }
                    Element lockEl = doc.createElement("lock");
                    lockEl.setAttribute("token", lockToken);
                    lockEl.setAttribute("expires", expiryIso);
                    lockEl.setTextContent(String.join(",", lockedNow));
                    locksEl.appendChild(lockEl);

                    // write DOM to temp then move to original (attempt atomic move, fallback otherwise)
                    Path tmp = Files.createTempFile(xmlPath.getParent(), "showtmp-", ".xml");
                    try (OutputStream os = Files.newOutputStream(tmp)) {
                        TransformerFactory tf = TransformerFactory.newInstance();
                        Transformer t = tf.newTransformer();
                        t.setOutputProperty(OutputKeys.INDENT, "yes");
                        t.transform(new DOMSource(doc), new StreamResult(os));
                    }
                    try {
                        Files.move(tmp, xmlPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                    } catch (AtomicMoveNotSupportedException amnse) {
                        // try non-atomic move
                        Files.move(tmp, xmlPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                sendJson(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        "{\"status\":\"error\",\"message\":\"Server error: " + escapeForJson(ex.getMessage()) + "\"}");
                return;
            }
        } // end synchronized on showLock

        // Build response JSON string manually (no library required)
        StringBuilder sb = new StringBuilder();
        boolean someUnavailable = !unavailable.isEmpty();
        int statusCode = someUnavailable && lockedNow.isEmpty() ? HttpServletResponse.SC_CONFLICT : HttpServletResponse.SC_OK;
        sb.append("{");
        sb.append("\"status\":\"ok\"");
        sb.append(",\"lockedSeats\":").append(jsonArray(lockedNow));
        sb.append(",\"unavailableSeats\":").append(jsonArray(unavailable));
        if (lockToken != null) {
            sb.append(",\"lockToken\":\"").append(lockToken).append("\"");
            sb.append(",\"expiry\":\"").append(expiryIso).append("\"");
        }
        sb.append("}");

        sendJson(resp, statusCode, sb.toString());
    }

    // helper: send JSON with status
    private void sendJson(HttpServletResponse resp, int httpStatus, String jsonBody) throws IOException {
        resp.setStatus(httpStatus);
        try (PrintWriter w = resp.getWriter()) {
            w.print(jsonBody);
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
            sb.append("\"").append(escapeForJson(s)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeForJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
