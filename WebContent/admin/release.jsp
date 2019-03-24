<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.Release,edu.uic.rrc.portal.model.entities.ProjectFiles,edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.listener.ProjectSyncScheduleListener,edu.uic.rrc.portal.model.entities.arvados.Group,
java.text.DateFormat,java.text.SimpleDateFormat,java.util.List" %>
<%
	Release release = ProjectsServlet.getRelease(request);
	User user = release.getReleasedBy();
	DateFormat df = new SimpleDateFormat("EEE, MMM d, hh:mm a,yyyy.");
	Group proj = ProjectSyncScheduleListener.getProject(release.getProjectID());
%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Release - <%= release.getTitle() %></title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css">
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css">
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
</head>
<body>
<portal:header/>
<portal:menu/>
<div>
<h1>Release - <%= release.getTitle() %></h1>
<p><strong>Project:</strong><a class="btn btn-link" href="<%= request.getContextPath() %>/manage-projects/<%= proj.getUuid()%>"><%= proj.getName() %></a><br>
<strong>Released by:</strong> <%= user.getName() %> (<%= user.getUserid() %>)<br>
<strong>Date:</strong> <%= df.format(release.getRelease_date()) %></p>

<pre style="margin: 20px 0px; width:75%; overflow-wrap:break-word; word-break:normal; white-space: pre-wrap"><%= release.getDescription() %></pre>
<!-- remove this table later and use above code after code for releasing multiple types in one release is in place -->
<table class="table" style="width:70%">
<tr>
	<th>File Name</th>
	<th>File Type</th>
	<th>Description</th>
</tr>
<tr>
<% 
	for ( ProjectFiles afile : release.getFiles() ) {
		String[] filepath = afile.getID().split("/");
%>
	<tr><td><a href="<%=request.getContextPath()%>/file/<%= afile.getID() %>"><%=afile.getID().split("/",2)[1]%></a></td>
	<td><%= afile.getType() %></td>
	<td><%= afile.getDescription() %></td>
</tr>			
<%	}	%>
</table>
</div>
</body>
</html>
