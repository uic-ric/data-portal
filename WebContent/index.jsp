<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.User,
	edu.uic.rrc.portal.UsersServlet,edu.uic.rrc.portal.listener.AppConfigListener,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.listener.AppRequestListener,edu.uic.rrc.portal.model.entities.User" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><%= AppConfigListener.getBrand() %></title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/jstree.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jstree.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/moment.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/tables.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datetime.js"></script>
<script>
//Function to toggle icon for section header
function toggleSection(elem) {
	var icon = $(elem).find('span.glyphicon:first');
	if ( icon.hasClass("glyphicon-menu-down") ) {
		icon.removeClass('glyphicon-menu-down');
		icon.addClass('glyphicon-menu-right');
	} else {
		icon.removeClass('glyphicon-menu-right');
		icon.addClass('glyphicon-menu-down');
	}
}
</script>
</head>
<body>
<portal:header/>
<portal:menu/>
<div id="content">
<% User user = AppRequestListener.getSelf(request); 
	if ( user != null ) { %>
<h2>Welcome, <%= user.getName() %></h2>
<div class="page-header">
<h3><a href="<%= request.getContextPath() %>/projects" style="color:black;">Recent Projects</a></h3>
</div>
<portal:recentprojects/>

<div class="page-header">
<h3><a href="<%= request.getContextPath() %>/release" style="color:black;">Recent Activity</a></h3>
</div>
<portal:recentActivity/>
<% } %>
</div>
</body>
</html>
