<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ tag import="edu.uic.rrc.portal.model.entities.ProjectFiles,java.util.List,java.util.HashMap,java.util.Map,
edu.uic.rrc.arvados.ArvadosAPI,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.ProjectsServlet,
edu.uic.rrc.portal.listener.AppRequestListener,java.text.DateFormat, java.text.SimpleDateFormat" %>
<table class="table" id="project-table">
<thead>
<tr>
	<th>Project Name</th>
	<th>Created By</th>
	<th>Created At</th>
	<th>Description</th>
</tr>
</thead>
</table>

<script>
<% boolean isAdmin = request.isUserInRole("admin"); %>

function displayProject(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return '<a href="<%= request.getContextPath() %>/<%= isAdmin ? "manage-" : "" %>projects/' + full.uuid + '">' + data + '</a>';
}

function displayDescription(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return data == undefined ? data : ( data.length > 100 ? data.substr(0,100) + '...' : data);
}

function displayUser(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data.username; }
	return data.firstName + ' ' + data.lastName;
}

$('#project-table').DataTable({
	ajax: "<%= request.getContextPath() %>/projects/recent",
    order: [],
    columns: [
            { data: 'name', render: displayProject },
            { data: 'owner', render: displayUser },
            { data: 'created_at' },
            { data: 'description', width: '50%', render: displayDescription }],
    rowId: 'uuid',
    paging: false,
    searching: false,
    columnDefs: [ { targets: 2,  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ],
    pageLength: 25,
    "language": {
        "emptyTable": "No projects available"
      }
});
</script>