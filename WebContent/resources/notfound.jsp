<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page isErrorPage="true" trimDirectiveWhitespaces="true" %>
<% if (request.getHeader("Accept").contains("application/json")) { %>
{ error : 'not found' }
<% } else { %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Projects</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css">
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css">
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
</head>
<body>
<portal:header check_user="false"/>
<portal:menu/>
<div id="content">
<h1 style="color:red">Page not found</h1>
</div>
</body>
</html>
<% } %>