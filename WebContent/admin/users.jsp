<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.List, java.util.HashMap,edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.arvados.ArvadosAPI,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.AdministrationServlet,edu.uic.rrc.portal.ProjectsServlet" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Users</title>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/datatables.min.css"/>
<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/jstree.min.css"/>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/moment.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/datetime.js"></script>
<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/jstree.min.js"></script>
<script>
function addAdmin(form) {
	var fullname = form.elements["fullname"].value;
	var affiliation = form.elements["affiliation"].value;
	var email = form.elements["email"].value;
	
    if( fullname === "" || affiliation == "" ) {
    	document.getElementById("admin-error").style.display="inline";
    } else {
    	$.ajax({
			url: "<%=request.getContextPath()%>/manage-admins",
			accepts: { json: "application/json" },
			type: "POST",
			dataType:"json",
			data: { 'userid': email, 'fullname' : fullname, 'affiliation' : affiliation }, 
			success: function (data, status, http) {
				if ( http.status == 200 ) {		
					$('#admin-table').DataTable().ajax.reload();
				} 
			}, 
			error: function (http, status, error) {
				alert(status);
			}
		});
		$('#add_admin').modal('hide');
		document.getElementById("admin-error").style.display="none";
    }
}
</script>
</head>
<body>
<portal:header/>
<portal:menu active="users"/>
<!-- Admin form Modal -->
<div id="add_admin" class="modal fade" role="dialog">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
	<form name="admin-form" action="<%=request.getContextPath()%>/manage-admins" method="post">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h4 class="modal-title">Add new administrator</h4>
      </div>
      <div class="modal-body">
      	<p id="admin-error" align='center' style='color: red; display: none'>Please enter all the necessary details!</p>
      		<div class="form-group"><label for="email">Email address</label><input type="text" name="email" id="email" placeholder="Email" class="form-control"/></div>		      
			<div class="form-group"><label for="fullname">Full Name</label><input type="text" name="fullname" id="fullname" placeholder="Full name" class="form-control"/></div>
			<div class="form-group"><label for="affiliation">Affiliation</label><input type="text" name="affiliation" id="affiliation" placeholder="Affiliation" class="form-control"/></div>
      </div>
      <div class="modal-footer">
		<button type='button' class="btn btn-primary" onclick="addAdmin(this.form);">Add</button>
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
	</form>
    </div>
  </div>
</div>

<div id="content">
<h1>Users</h1>

<ul class="nav nav-tabs" role="tablist">
<li role="presentation" class="active"><a href="#users" aria-controls="users" role="tab" data-toggle="tab">Project users</a></li>
<li role="presentation" ><a href="#admins" aria-controls="admins" role="tab" data-toggle="tab">Administrators</a></li>
</ul>

<%
	if ( request.getAttribute("message") != null ) {
%><div class="message"><%=(String) request.getAttribute("message")%></div>
<%
	}
%>


<div class="tab-content">

<div role="tabpanel" class="tab-pane active" id="users" style="padding:10px">
<table id="user-table" class="table">
<thead>
<tr>
<th>User Name</th>
<th>Email Address</th>
<th>Affiliation</th>
</tr>
</thead>
</table>
</div>

<div role="tabpanel" class="tab-pane" id="admins" style="padding:10px">
<div><button type="button" class="btn btn-link" onclick="$('#add_admin').modal('show')"><span class="glyphicon glyphicon-plus-sign"></span> Add Admin</button></div>
<table id="admin-table" class="table" style="width:100%">
<thead>
<tr>
<th>User Name</th>
<th>Email Address</th>
<th>Affiliation</th>
</tr>
</thead>
</table>
</div>

<script type="text/javascript">
<%List<User> userList= (List<User>)request.getAttribute("portal_users");%>
var users = <%=ProjectsServlet.usersToJson(userList)%>

$('#user-table').DataTable({
    data: users,
    order: [ [0, 'asc'] ],
    columns: [
            { data: 'name' },
            { data: 'id' },
            { data: 'affiliation'}
            ],
    rowId: 'id',
    pageLength: 25,
    "language": {
        "emptyTable": "No users available"
    },
});

$('#admin-table').DataTable({
    ajax: { url: '<%= request.getContextPath() %>/manage-users?admin', dataSrc: '' }, 
    order: [ [0, 'asc'] ],
    columns: [
            { data: 'name' },
            { data: 'id' },
            { data: 'affiliation'}
            ],
    pageLength: 10,
    "language": {
        "emptyTable": "No users available"
    },
});
</script>

</div></div>
</body>
</html>