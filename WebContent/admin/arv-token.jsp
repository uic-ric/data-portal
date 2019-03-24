<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<%@ page import="edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.listener.AppRequestListener,edu.uic.rrc.portal.listener.AppConfigListener,edu.uic.rrc.portal.AdministrationServlet" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Users</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<script type="text/javascript" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
</head>
<body>
<% User self = AppRequestListener.getSelf(request); %>
<portal:header check_user="false"/>
<portal:menu/>
<div id="content">
<% if ( request.getParameter("api_token") != null ) { %>
<script type="text/javascript">
    $(window).on('load',function(){
        $('#add_token').modal('hide');
    });
</script>
<%	if ( self.getToken() != null ) {
		if ( request.getParameter("overwrite") != null ) {
			AdministrationServlet.setArvadosToken(request, request.getParameter("api_token"));
%><div class="alert alert-success"><strong>Success!</strong> Arvados API token set.</div><%			
		} else {
%><div class="alert alert-warning"><strong>Warning!</strong> Arvados API already set for your account. <a href="?api_token=<%= request.getParameter("api_token") %>&overwrite=1">Click here</a> to overwrite existing Arvados API token.</div><%			
		}
	} else {
		AdministrationServlet.setArvadosToken(request, request.getParameter("api_token"));
%><div class="alert alert-success"><strong>Success!</strong> Arvados API token set.</div><%
	}
} else { %>
<p style="text-align:center"><a href="<%= AppConfigListener.getArvadosAPIServer() %>/login?return_to=https://<%= request.getServerName() + request.getRequestURI() %>" class="btn btn-link">Register via Arvados</a></p>
<% } %>
</div>
</body>
</html>