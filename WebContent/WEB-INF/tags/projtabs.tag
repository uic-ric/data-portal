<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ attribute name="active" required="false" %>
<nav>
<ul>
<%if(request.isUserInRole("admin")) {%>
<li<%= "all".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/manage-projects">All Projects</a></li>	
<li<%= "diff".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/manage-users">Arvados</a></li>
<%} %>
<jsp:doBody/>
</ul>
</nav>