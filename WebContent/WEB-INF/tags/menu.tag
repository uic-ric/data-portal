<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ attribute name="active" required="false" %>
<nav>
<ul  class="nav nav-tabs" role="tablist">
<%if(request.isUserInRole("admin")) {%>
<li role="presentation" <%= "projects".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/manage-projects">Projects</a></li>	
<li role="presentation" <%= "users".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/manage-users">Users</a></li>
<%} else { %>
<li role="presentation" <%= "projects".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/projects">Projects</a></li>
<% } %>
<li role="presentation" <%= "releases".equals(active) ? " class='active'" : ""%>><a href="<%=request.getContextPath() %>/release">Releases</a></li>
<li><a href="<%= request.getContextPath() %>/logout.jsp">Logout</a></li>
<jsp:doBody/>
</ul>
</nav>

<!-- Notices -->
<!--
<div style="padding-top:10px">
<div class="alert alert-danger alert-dismissable">
  <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
  <strong>Notice!</strong> The data portal will not be available on Friday February 2, 2018 from 4:30 to 6:00 pm CST for maintenance. Please plan your work accordingly.
</div>
</div>
-->