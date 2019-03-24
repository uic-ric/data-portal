<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="release" required="true" type="edu.uic.rrc.portal.model.entities.Release" %>
<%@ attribute name="files" required="false" type="java.util.List" %>
<%@ tag import="java.util.List, edu.uic.rrc.portal.model.entities.ProjectFiles,java.text.DateFormat,java.text.SimpleDateFormat,
edu.uic.rrc.portal.FileServlet,edu.uic.rrc.portal.model.entities.Release,edu.uic.rrc.portal.model.entities.User" %>
<table border="1" style="margin: 0px 0px 0px 40px">
<tr>
	<th>File Name</th>
	<th>File Type</th>
	<th>Released By</th>
	<th>Release Date</th>
	<th>Description</th>
</tr>
<tr>
<% 
	DateFormat df = new SimpleDateFormat("EEE, MMM d, hh:mm a,yyyy.");
	User user = release.getReleasedBy();
	for ( Object elem: files ) {
		if ( elem instanceof ProjectFiles ) { 
			ProjectFiles afile = (ProjectFiles) elem;
			String[] filepath = afile.getID().split("/");
%>
	<tr><td><a href="<%=request.getContextPath()%>/file/<%= afile.getID() %>"><%=(filepath.length>1)?filepath[1]:filepath[0] %></a></td>
	<td><%= afile.getType() %></td>
	<td><%= user.getName() %> (<%= user.getUserid() %>)</td>
	<td><%= df.format(release.getRelease_date()) %></td>
	<td><%= afile.getDescription() %></td>
</tr>			
	<%		}
		}
	%>
</table>

