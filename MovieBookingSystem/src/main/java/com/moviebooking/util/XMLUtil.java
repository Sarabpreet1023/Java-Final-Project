package com.moviebooking.util;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * XML helper utilities for seat map operations.
 * - robust for tag name "seat" or "Seat"
 * - atomic save to temp file
 */
public class XMLUtil {

    // Parse XML file into a DOM Document
    public static Document parse(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(true);
        factory.setIgnoringElementContentWhitespace(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        doc.getDocumentElement().normalize();
        return doc;
    }

    // Save XML Document safely (overwrite existing)
    public static void saveDocument(Document doc, File file) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    // Atomic save (writes to temp, then rename)
    public static void saveDocumentAtomic(Document doc, File file) throws Exception {
        File temp = new File(file.getAbsolutePath() + ".tmp");
        saveDocument(doc, temp);
        if (file.exists() && !file.delete()) {
            throw new IOException("Unable to delete original XML file: " + file.getAbsolutePath());
        }
        if (!temp.renameTo(file)) {
            throw new IOException("Unable to rename temp XML file to original: " + temp.getAbsolutePath());
        }
    }

    // Find a seat node by id attribute (case-insensitive tag name 'seat')
    public static Element findSeat(Document doc, String seatId) {
        // try both "seat" and "Seat" and also generic search
        List<String> tagCandidates = Arrays.asList("seat", "Seat");
        for (String tag : tagCandidates) {
            NodeList list = doc.getElementsByTagName(tag);
            for (int i = 0; i < list.getLength(); i++) {
                Element el = (Element) list.item(i);
                if (seatId.equalsIgnoreCase(el.getAttribute("id"))) {
                    return el;
                }
            }
        }
        // fallback: scan all elements and match attribute id
        NodeList all = doc.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            Node n = all.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                Element el = (Element) n;
                if (el.hasAttribute("id") && seatId.equalsIgnoreCase(el.getAttribute("id"))) {
                    return el;
                }
            }
        }
        return null;
    }

    // Clean up expired locks in the XML (keeps xml self-contained if you used per-seat lock attributes)
    public static void cleanupExpiredLocks(Document doc) {
        // find seats with status LOCKED and lockExpiry attribute
        NodeList list = doc.getElementsByTagName("seat");
        if (list.getLength() == 0) list = doc.getElementsByTagName("Seat");
        for (int i = 0; i < list.getLength(); i++) {
            Element el = (Element) list.item(i);
            if ("LOCKED".equalsIgnoreCase(el.getAttribute("status"))) {
                String expiry = el.getAttribute("lockExpiry");
                if (expiry != null && !expiry.isEmpty()) {
                    try {
                        Instant exp = Instant.parse(expiry);
                        if (Instant.now().isAfter(exp)) {
                            el.setAttribute("status", "AVAILABLE");
                            el.removeAttribute("lockedBy");
                            el.removeAttribute("lockExpiry");
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    /**
     * Set the status of multiple seats in the XML file.
     * Example status: "LOCKED", "AVAILABLE", "BOOKED"
     *
     * This method is synchronized to avoid concurrent writes from multiple threads/processes in the same JVM.
     */
    public static synchronized void setSeatsStatus(File xmlFile, List<String> seatIds, String status) throws Exception {
        if (seatIds == null || seatIds.isEmpty()) return;
        Document doc = parse(xmlFile);

        // normalize target ids to uppercase for comparison
        Set<String> target = seatIds.stream().map(String::toUpperCase).collect(Collectors.toSet());

        // pick tag name existing in file
        NodeList seats = doc.getElementsByTagName("seat");
        if (seats.getLength() == 0) seats = doc.getElementsByTagName("Seat");

        for (int i = 0; i < seats.getLength(); i++) {
            Element seatEl = (Element) seats.item(i);
            String id = seatEl.getAttribute("id");
            if (id != null && target.contains(id.toUpperCase())) {
                seatEl.setAttribute("status", status);
                // when locking, optionally set expiry metadata (ISO instant) — leave caller to set attributes if needed
                if ("LOCKED".equalsIgnoreCase(status)) {
                    // no default expiry here — lock expiry should be handled by DB scheduler
                } else {
                    // cleanup any lock attributes
                    seatEl.removeAttribute("lockedBy");
                    seatEl.removeAttribute("lockExpiry");
                }
            }
        }

        // write changes atomically
        saveDocumentAtomic(doc, xmlFile);
    }

    /**
     * Parse a comma-separated seat CSV into a list.
     * Accepts "A1,A2" or "A1, A2"
     */
    public static List<String> parseSeatCsv(String csv) {
        if (csv == null) return Collections.emptyList();
        return Arrays.stream(csv.split("\\s*,\\s*"))
                .filter(s -> s != null && !s.isEmpty())
                .collect(Collectors.toList());
    }
}
