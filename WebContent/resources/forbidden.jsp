<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true" %>
<% if (request.getHeader("Accept").contains("application/json")) { response.setContentType("application/json"); %>
{ error : 'not authorized' }
<% } else { response.setContentType("text/html"); %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Forbidden</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css">
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css">
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
</head>
<body>
<portal:header check_user="false"/>
<portal:menu/>
<div id="content">
<h1 style="color:red">Access to specified resource is denied</h1>
<p>Current login: <%= request.getRemoteUser() %></p>
</div>
</body>
</html>
<% } %>