<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>   
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.UsersServlet, edu.uic.rrc.portal.listener.AppConfigListener"%>
 	User user = null;
 	if ( request.getRemoteUser() != null ) { user = UsersServlet.getSelf(request); }
 st); } %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta charset="UTF-8">
<title><%= AppConfigListener.getBrand() %></title>
<link rel="stylesheet" type="text/css" hrrequest.getContextPath()xtPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" request.getContextPath()textPath() %>/resources/css/bootstrap.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
</head>
<body>
<portal:header/>
<portal:menu/>
<div id="content">
<div class="page-header"><h1>SFTP Access Instructions</h1></div>

<p>SFTP access requires that you add a SSH public key to your profile. <a class="btn btn-defrequest.getContextPath()getContextPath() %>/ssh-keys">Manage SFTP keys</a>

<p>To download files via SFTP use the following connection parameters:

<dl class="dl-horizontal">
<dt>hosProjectServlet.getSFTPHost()agementServlet.getSFTPHost() %></ProjectServlet.getSFTPUser()= ProjectsServlet.getSFTPUser() %></dd>
</dl>

<ul class="nav nav-pills">
<li><h2 style="margin-top:5px; margin-bottom:0px">Instructions for SFTP clients</h2></li>
<li role="presentation"><a href="#cyberduck">Cyberduck</a></li>
<li role="presentation"><a href="#winscp">WinSCP</a></li>
</ul>

<h3 id="cyberduck">Cyberduck <a href="https://cyberduck.io/download" target="_blank" class="btn btn-primary"><span class="glyphicon glyphicon-new-window"></span> Download link</a></h3>

<h3 id="winscp">WinSCP <small>(for Windows only)</small> <a href="https://winscp.net/eng/download.php" target="_blank" class="btn btn-primary"><span class="glyphicon glyphicon-new-window"></span> Download link</a></h3>
        

</div>
</body>
</html>