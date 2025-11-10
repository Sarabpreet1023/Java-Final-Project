<%@ page contentType="text/html; charset=UTF-8" %>
<%
    // scriptlet-based header (no JSTL). Requires servlets to set session attributes:
    // session.setAttribute("username", username);
    // session.setAttribute("isAdmin", Boolean.TRUE/FALSE);
    String username = (String) session.getAttribute("username");
    Boolean isAdmin = (Boolean) session.getAttribute("isAdmin");
    if (isAdmin == null) isAdmin = Boolean.FALSE;
    String ctx = request.getContextPath();
%>
<style>
  /* small header styles that integrate visually with your dark UI */
  .topbar { display:flex; align-items:center; justify-content:space-between; padding:12px 28px; max-width:1220px; margin:0 auto 16px; color:#e6eef8; }
  .brand { font-size:20px; font-weight:800; letter-spacing:0.2px; color: #fff; text-decoration:none; }
  .userlinks { display:flex; gap:14px; align-items:center; }
  .userlinks a { color:#cfe8ff; text-decoration:none; font-weight:600; padding:6px 10px; border-radius:8px; background:transparent; }
  .userlinks a.btn { background: linear-gradient(90deg,#2563eb,#60a5fa); color:#fff; }
  .welcome { color:#cfe8ff; font-weight:600; padding:6px 8px; }
</style>

<div class="topbar" role="banner">
  <a class="brand" href="<%=ctx%>/">Sarna Movies</a>

  <div class="userlinks" role="navigation">
    <% if (username != null) { %>
        <div class="welcome">Welcome, <strong><%= username %></strong></div>
        <% if (isAdmin) { %>
           <a href="<%=ctx%>/admin/dashboard.jsp" class="">Admin</a>
        <% } %>
        <a href="<%=ctx%>/logout" class="btn">Logout</a>
    <% } else { %>
        <a href="<%=ctx%>/login" class="">Login</a>
        <a href="<%=ctx%>/register" class="btn">Register</a>
    <% } %>
  </div>
</div>
