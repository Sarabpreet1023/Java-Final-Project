package com.moviebooking.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import org.mindrot.jbcrypt.BCrypt;
import com.moviebooking.dao.UserDAO;
import com.moviebooking.dao.User;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.getRequestDispatcher("/login.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String password = req.getParameter("password");

        if (username == null || password == null) {
            req.setAttribute("error", "Missing credentials");
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
            return;
        }

        try {
            User u = UserDAO.findByUsername(username);
            if (u == null) {
                req.setAttribute("error", "Invalid username or password");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
                return;
            }
            String hash = u.getPasswordHash();
            if (BCrypt.checkpw(password, hash)) {
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", u.getId());
                session.setAttribute("username", u.getUsername());
                session.setAttribute("isAdmin", u.isAdmin());
                // optional: session.setMaxInactiveInterval(30*60); // 30 minutes
                resp.sendRedirect(req.getContextPath() + "/"); // change to dashboard
            } else {
                req.setAttribute("error", "Invalid username or password");
                req.getRequestDispatcher("/login.jsp").forward(req, resp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            req.setAttribute("error", "Server error: " + ex.getMessage());
            req.getRequestDispatcher("/login.jsp").forward(req, resp);
        }
    }
}
