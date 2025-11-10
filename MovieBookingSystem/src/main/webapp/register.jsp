<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head><title>Register</title></head>
<body>
<h2>Register</h2>
<c:if test="${not empty error}">
    <div style="color:red">${error}</div>
</c:if>

<form method="post" action="${pageContext.request.contextPath}/register">
    <label>Username: <input type="text" name="username" required></label><br/>
    <label>Email (optional): <input type="email" name="email"></label><br/>
    <label>Password: <input type="password" name="password" required></label><br/>
    <label>Confirm Password: <input type="password" name="password2" required></label><br/>
    <button type="submit">Register</button>
</form>

<p>Already have an account? <a href="${pageContext.request.contextPath}/login">Login</a></p>
</body>
</html>
