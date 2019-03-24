<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.HashMap,edu.uic.rrc.portal.model.entities.arvados.User,edu.uic.rrc.arvados.ArvadosAPI,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.UsersServlet,edu.uic.rrc.portal.listener.AppConfigListener,edu.uic.rrc.portal.model.entities.arvados.Collection,edu.uic.rrc.portal.AdministrationServlet" %>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Projects</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<!-- <link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/> -->
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/tables.js"></script></head>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/moment.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datetime.js"></script>
<script src="//cdnjs.cloudflare.com/ajax/libs/textile-js/2.0.4/textile.min.js"></script>
<body>
<portal:header/>
<portal:menu active="projects"/>
<div id="content">
<%
	ArvadosAPI arv = AdministrationServlet.getArv(request);	
	HashMap<String, User> userMap = new HashMap<String, User>();
	Group project = AdministrationServlet.getCurrentProject(request);
	User arvUser = arv.getCurrentUser();
	if ( project != null ) { %>
<div id="heading">
<h1><%= project.getName() %>
<a href="<%= request.getContextPath() %>/projects/<%= project.getUuid() %>" class="btn btn-info btn-sm" style="float:right">View as user</a>
</h1>
<%	String owner_uuid = project.getOwnerUUID();
	while ( ArvadosAPI.isGroupUUID(owner_uuid) ) {
		Group parent = arv.getGroup(owner_uuid);
		owner_uuid = parent.getOwnerUUID();
	}
	User owner = arv.getUser(owner_uuid); 
	
	boolean canManage = arv.canManage(project.getUuid());
%>
<h4><b>Project owner:</b> <%= owner.getFirstName() %> <%= owner.getLastName() %></h4>
</div>
<% 	String description = project.getDescription();
	if ( description != null && description.length() > 0) { %>
	<div class="panel panel-default">
		<div class="panel-heading">
		<h4 class="panel-title"><a role="button" data-toggle="collapse" href="#description" aria-expanded="true" aria-controls="description">Description</a></h4></div>
		<div class="panel-collapse collapse in" id="description">
		<div class="panel-body">
		<script>document.write(textile("<%= description.replace("\"", "\\\"").replace("\n", "\\n") %>"))</script>		
		</div>
		</div>
	</div>
<% } %>
<ul class="nav nav-tabs" role="tablist">
<li role="presentation" class="active"><a href="#collections" aria-controls="collections" role="tab" data-toggle="tab">Collections</a>
<li role="presentation"><a href="#users" aria-controls="users" role="tab" data-toggle="tab">Users</a>
<li role="presentation"><a href="#uploads" aria-controls="uploads" role="tab" data-toggle="tab">Uploaded Files</a>
<% if ( canManage ) { %>
<li role="presentation"><a href="#admins" aria-controls="admins" role="tab" data-toggle="tab">Project Admins</a>
<% } %>
</ul>

<div class="tab-content">

<%-- Tab for project collections --%>
<div role="tabpanel" class="tab-pane active" id="collections">
<div style="margin:10px">
<table class="table" id="collection-table">
<thead><tr><th>Collection</th><th>Created</th><th>Modified</th><th>Modified By</th></tr></thead><tbody>
<% for ( Collection collection : AdministrationServlet.getCollectionsForProject(request, project.getUuid()) ) { 
	if ( ! userMap.containsKey(project.getModifiedByUserUUID())) 
		userMap.put(project.getModifiedByUserUUID(), arv.getUser(project.getModifiedByUserUUID()));	
	
	User user = userMap.get(project.getModifiedByUserUUID());
	
	String name = collection.getName();
	if ( name == null || name.length() < 1 ) { name = collection.getUuid(); }
%><tr>
<td><a href="<%= request.getContextPath() %>/manage-projects/<%= project.getUuid() %>/<%= collection.getUuid() %>"><%= name %></a></td>
<td><%= collection.getCreatedAt().format(AdministrationServlet.DATE_FORMAT) %></td>
<td><%= collection.getModified().format(AdministrationServlet.DATE_FORMAT) %></td>
<td><%= user.getFirstName() %> <%= user.getLastName() %></td>
</tr>
<% } %></tbody></table>
<script>
$('#collection-table').DataTable({
    order: [],
    columnDefs: [ { targets: [1,2], render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ],
    pageLength: 25
});

</script>
</div>
</div>

<%-- Tab for project members (customers) --%>
<div role="tabpanel" class="tab-pane" id="users">
<div style="margin:10px">
<portal:usertable users="<%= ProjectsServlet.getCurrentMembers(request) %>" project="<%= project.getUuid() %>"/>
</div>
</div>

<%-- Tab for uploaded files --%>
<div role="tabpanel" class="tab-pane" id="uploads">
<div style="margin:10px">
<portal:uploadfiletable project="<%= project.getUuid() %>"/>
</div>
</div>

<%-- Tab for project administrators.  Only visible if the user can manage the project --%>
<% if ( canManage ) { %>
<div role="tabpanel" class="tab-pane" id="admins">
<div style="margin:10px">
<portal:project-admins-table project="<%= project %>"/>
</div>
</div>
<% } %>

</div>
<% } %>
</div>
</body>
</html>