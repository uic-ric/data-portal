<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.List, java.util.HashMap, edu.uic.rrc.portal.model.entities.Release, edu.uic.rrc.portal.model.entities.ProjectFiles,edu.uic.rrc.arvados.ArvadosAPI, java.text.DateFormat, java.text.SimpleDateFormat, javax.json.Json, javax.json.stream.JsonGenerator,
edu.uic.rrc.portal.listener.ProjectSyncScheduleListener,edu.uic.rrc.portal.model.entities.arvados.Group, edu.uic.rrc.portal.model.entities.User" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Projects</title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/jstree.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jstree.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/moment.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datetime.js"></script>
</head>
<body>
<portal:header/>
<portal:menu active="releases"/>
<div id="content">
<div class="page-header"><h1>Releases</h1></div>
<% boolean isAdmin = request.isUserInRole("admin");
if ( isAdmin ) { %><form method="post"><% } %>
<table class="table" id="release-table">
<thead>
<tr>
	<th>Release</th>
	<th>Project name</th>
	<th>Released by</th>
	<th>Release Date</th>
	<th>Description</th>
</tr>
</thead>
</table>
<% if ( isAdmin ) { %><br>
<button type="submit" name="action" value="revoke-release">Revoke Release</button>
</form>
<% } %>
<script>

var tabledata = 
<% 
List<Release> releaseList= (List<Release>)request.getAttribute("releases");
DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
JsonGenerator generator = Json.createGenerator(out);
generator.writeStartArray();

for ( Release release : releaseList ) {
	List<ProjectFiles> files = release.getFiles();
	Group proj = ProjectSyncScheduleListener.getProject(files.get(0).getProjectID());
	String name = release.getTitle();
	if ( name == null || name.equals("") ) { name = df.format(release.getRelease_date()); }
	generator.writeStartObject();	
	generator.write("id", release.getReleaseID());
	generator.write("name", name);
	generator.write("release_date", df.format(release.getRelease_date()));
	generator.write("description", release.getDescription());
	
	User user = release.getReleasedBy();
	generator.write("release_by", user.toJSON());
	
	generator.writeStartObject("project");
	generator.write("uuid", proj.getUuid());
	generator.write("name", proj.getName());
	generator.writeEnd();
	generator.writeEnd();
} 
generator.writeEnd();
generator.flush();
%>;


function displayProject(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data.name; }
	return '<a href="<%= request.getContextPath() %>/<%= isAdmin ? "manage-" : "" %>projects/' + data.uuid + '">' + data.name + '</a>';
}

function displayDescription(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return data == undefined ? data : ( data.length > 100 ? data.substr(0,100) + '...' : data);
}

function displayRelease(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return '<% if ( isAdmin ) { %><input type="checkbox" name="revokeList" value="' + full.id + '"> <% } %><a href="<%= request.getContextPath() %>/release/' + full.id + '">' + data + '</a>';
}

function displayUser(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data.id; }
	return data.name != undefined ? (data.name + ' &lt;' + data.id + '&gt;' ) : data.id;
}

$('#release-table').DataTable({
//	ajax: "<%= request.getContextPath() %>/projects/recent",
	data: tabledata, 
	order: [],
    columns: [
            { data: 'name', render: displayRelease },
            { data: 'project', render: displayProject },
            { data: 'release_by', render: displayUser },
            { data: 'release_date' },
            { data: 'description', width: '50%', render: displayDescription }],
    rowId: 'uuid',
    paging: false,
    searching: false,
    columnDefs: [ { targets: 3,  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ],
    pageLength: 25,
    order : [ [3,'desc']],
    "language": {
        "emptyTable": "No releases available"
      }
});
</script>

</div>
</body>
</html>