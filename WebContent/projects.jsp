<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.Set, java.util.ArrayList,java.util.List,java.util.HashMap,edu.uic.rrc.portal.model.entities.arvados.User,java.util.Map,java.util.Map.Entry,edu.uic.rrc.arvados.ArvadosAPI,edu.uic.rrc.portal.AdministrationServlet,edu.uic.rrc.portal.model.entities.arvados.Group,java.text.DateFormat,java.text.SimpleDateFormat" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Projects</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css">
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css">
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/tables.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datetime.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/moment.min.js"></script>
<script src="//cdnjs.cloudflare.com/ajax/libs/textile-js/2.0.4/textile.min.js"></script>
</head>
<body>
<portal:header/>
<portal:menu active="projects"/>
<div id="content">
<div class="page-header"><h1>Projects</h1></div>
<table class="table" id="project-table">
<thead>
<tr>
<th>Project Name</th>
<th class="no-sort">Analyst</th>
<th>Created At</th>
<th class="no-sort">Description</th>
</tr>
</thead>
<tbody>
</tbody></table>
<script>
function displayProject(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }	
	return '<a href="<%= request.getContextPath() %>/projects/' + full.uuid + '">' + data + '</a>';
}

function displayUser(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data.username; }
	if(typeof data !== 'undefined' )
		return data.firstName + ' ' + data.lastName + ' &lt;' + data.email + '&gt;';
	else
		return null;	
}

function displayDescription(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( data != undefined && data.length > 75 ) { return data.substring(0, 75) + "..."; } else { return data; } 
}

$('#project-table').DataTable({
	ajax: "<%= request.getContextPath() %>/projects",
	order: [ [0, 'asc'] ],
    columns: [
            { data: 'name', render: displayProject },
            { data: 'owner', render: displayUser },
            { data: 'created_at' },
            { data: 'description', width: '30%', render: displayDescription }],
    rowId: 'id',
    columnDefs: [ { targets: [2],  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM D, YYYY h:mm A' ) } ],
    pageLength: 25,
    "language": {
        "emptyTable": "No projects available"
      }
});

</script>
</div>
</body>
</html>