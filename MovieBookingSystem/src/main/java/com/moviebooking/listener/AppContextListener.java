package com.moviebooking.listener;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import com.moviebooking.util.SeatLockManager;

@WebListener
public class AppContextListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext ctx = sce.getServletContext();
        // Set this to the folder you created earlier
        ctx.setAttribute("SHOWS_XML_DIR", "C:\\Users\\komda\\shows_xml");

        ctx.setAttribute("seatLockManager", new SeatLockManager());
        System.out.println("AppContextListener initialized. SHOWS_XML_DIR=" + "C:/Users/komda/shows_xml/show_1001_seats.xml");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) { }
}
