<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="edu.uic.rrc.portal.model.entities.Release,java.util.List,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.listener.ProjectSyncScheduleListener,edu.uic.rrc.portal.ProjectsServlet, java.text.DateFormat, java.text.SimpleDateFormat" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Activity</title>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/base.css">
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/bootstrap.min.css">
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/jquery-3.2.1.min.js"></script>
</head>
<body>
<portal:header/>
<portal:menu active="activity"/>
<h1 style="margin:30px">Activity</h1>
<table class="table" style="margin: 20px;">
<tr>
	<th>Project Name</th>
	<th>Released By</th>
	<th>Released At</th>
	<th>Description</th>
</tr>
<tr>
<%
	DateFormat df = new SimpleDateFormat("EEE, MMM d, yyyy hh:mm a");
boolean isAdmin = request.isUserInRole("admin");
List<Release> releaseList = ProjectsServlet.getMyActivity(request);
for ( Release release : releaseList ) {
	 String projectForRelease = release.getProjectID();
	 Group proj = ProjectSyncScheduleListener.getProject(projectForRelease);
%><tr>
<td><%= proj.getName()%></td>
<td><%= release.getRelease_by()%></td>
<td><a href="<%= request.getContextPath() %>/release/<%= release.getReleaseID() %>"><%= df.format(release.getRelease_date()) %></a></td>
<td><%= release.getDescription() %></td></tr>
<%	} %>
</table>
</body>
</html>