<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.Set, java.util.ArrayList,java.util.List,java.util.HashMap,
	java.util.Map,java.util.Map.Entry,com.google.api.client.util.ArrayMap,
	edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.model.entities.ProjectFiles" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Projects</title>
<link rel="stylesheet" type="text/css" href="resources/css/base.css">
	<script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>
</head>
<body>
<portal:header/>
<portal:menu active="projects"/>
<div id="content">
<html><head><title>Projects List!</title></head>
<body>
	<table border="1">
		<tr>
			<th>User Id</th>
		</tr>
		<tr>
		<% 
		List<String> rawFilesList = (List<String>)session.getAttribute("project_users");
		for(String file : rawFilesList){
			out.println("<tr>");
			out.println("<td>" + file + "</td>");
			out.println("</tr>");
		}
		%>
		</tr>
	</table>
</tr>
</table>
</body>
</html>