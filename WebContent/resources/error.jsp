<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" pageEncoding="UTF-8" isErrorPage="true" trimDirectiveWhitespaces="true" %>
<% if ( exception == null ) { exception = (Exception) request.getAttribute("javax.servlet.error.exception"); }
if (request.getHeader("Accept").contains("application/json")) { response.setContentType("application/json"); %>
{ error : '<%= exception.getMessage().replace("'", "\\'").replace("\n", "\\n") %>' }
<% } else { response.setContentType("text/html"); %>
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
<h1 style="color:red">An error has occurred.</h1>
<%= exception.getMessage() %>
</div>
</body>
</html>
<% } %>