<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.UsersServlet" %>
<% String projectid = UsersServlet.getProjectID(request); %>
<% if ( projectid != null ) { %>
<portal:usertable users="<%= ProjectsServlet.getCurrentMembers(request) %>" project="<%= projectid %>"/>
<% } %>
