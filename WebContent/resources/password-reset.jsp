<%@ taglib prefix="portal" tagdir="/WEB-INF/tags"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page import="edu.uic.rrc.portal.listener.AppConfigListener"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><%= AppConfigListener.getBrand() %></title>
<link rel="stylesheet" type="text/css"
	href="<%=request.getContextPath()%>/resources/css/base.css">
<style>
.login-page {
	width: 360px;
	padding: 8% 0 0;
	margin: auto;
}

.form {
	position: relative;
	z-index: 1;
	background: #FFFFFF;
	max-width: 360px;
	margin: 0 auto 100px;
	padding: 45px;
	text-align: center;
	box-shadow: 0 0 20px 0 rgba(0, 0, 0, 0.2), 0 5px 5px 0
		rgba(0, 0, 0, 0.24);
}

.form input {
	width: 90%;
}
</style>
</head>
<body style="background-color: #ddd">
	<portal:header />
	<div id="content">
		<div class="login-page">
			<h2 align='center'>Reset password</h2>
			<div class="form">
				<form class="login-form" method="POST" action='<%= request.getContextPath() %>/reset-password'>
				<% if ( request.getParameter("email") != null && request.getParameter("reset") != null ) { %>
					<input type="hidden" name="email" value="<%= request.getParameter("email") %>">
					<input type="hidden" name="reset" value="<%= request.getParameter("reset") %>">
					<% if ( request.getAttribute("error") != null ) { %>
					<div class="alert alert-danger">Error: <%= request.getAttribute("error") %></div>
					<% } %>
					<div class="form-group">
						<label for="new_pass">New password</label>
						<input type="password" name="new_pass" id="new_pass" class="form-control" placeholder="New password"/>
					</div>
					<div class="form-group">
						<label for="new_pass2">Re-enter password</label>
						<input type="password" name="new_pass2" id="new_pass2" class="form-control" placeholder="Re-enter password"/>
					</div>
					<button type='submit'>Set password</button>
				<% } else { %>
					<div class="form-group">
						<label for="email">Email</label>
						<input type="text" name="email" id="email" class="form-control" placeholder="Email"/>
					</div>					
					<button type='submit'>Reset password</button>
				<% } %>
				</form>
			</div>
		</div>
	</div>
</body>
</html> 