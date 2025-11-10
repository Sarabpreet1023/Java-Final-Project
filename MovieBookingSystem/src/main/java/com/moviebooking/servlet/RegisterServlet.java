package com.moviebooking.servlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.*;
import org.mindrot.jbcrypt.BCrypt;
import com.moviebooking.dao.UserDAO;

@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // show register.jsp
        req.getRequestDispatcher("/register.jsp").forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String password = req.getParameter("password");
        String password2 = req.getParameter("password2");

        // simple server-side validation
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            !password.equals(password2)) {
            req.setAttribute("error", "Invalid input or passwords do not match.");
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
            return;
        }

        try {
            if (UserDAO.usernameExists(username)) {
                req.setAttribute("error", "Username already exists. Choose another.");
                req.getRequestDispatcher("/register.jsp").forward(req, resp);
                return;
            }

            String hashed = BCrypt.hashpw(password, BCrypt.gensalt(12));
            int userId = UserDAO.createUser(username, email, hashed);
            if (userId > 0) {
                // auto-login after registration
                HttpSession session = req.getSession(true);
                session.setAttribute("userId", userId);
                session.setAttribute("username", username);
                session.setAttribute("isAdmin", false); // default
                resp.sendRedirect(req.getContextPath() + "/"); // or user dashboard
            } else {
                req.setAttribute("error", "Registration failed. Try again.");
                req.getRequestDispatcher("/register.jsp").forward(req, resp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            req.setAttribute("error", "Server error: " + ex.getMessage());
            req.getRequestDispatcher("/register.jsp").forward(req, resp);
        }
    }
}
