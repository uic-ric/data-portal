<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="files" required="true" type="java.util.List" %>
<%@ attribute name="project" required="true" type="java.lang.String" %>
<%@ attribute name="view" required="true" type="java.lang.String" %>
<%@ attribute name="group" required="true" type="java.lang.String" %>
<%@ tag import="edu.uic.rrc.portal.model.entities.ProjectFiles,java.text.DateFormat,java.text.SimpleDateFormat,
edu.uic.rrc.portal.model.entities.Release,edu.uic.rrc.portal.ProjectsServlet,java.util.List" %>
<script src="<%= request.getContextPath() %>/resources/js/tables.js"></script>
<script>
var allFiles = <%= ProjectsServlet.filesToJson((List<ProjectFiles>) files).toString() %>;
var contextPath = "<%= request.getContextPath() %>";

buildFileTree(allFiles);

function back() {
	parentDir.pop();
	if ( parentDir.length > 0 ) {
		currFiles = parentDir[parentDir.length - 1].files;
	} else {
		currFiles = root;
		$('#back_botton').hide();
	}
	reloadTable();
}

function changeDir(id) {
	var thisdir = dirmap[id];
	parentDir.push(thisdir);
	currFiles = thisdir.files;
	$('#back_button').show();
	reloadTable();
}

function changeGroup(select) {
	var group = select.options[select.selectedIndex].value;
	var path = "<%= request.getContextPath() %>/projects/<%= project %>/<%= view %>";
	if ( group !== "" ) {
		path = path + "/" + group;
	}
	window.location.href = path;
}

function toggleDatatype(btn, datatype) {
    var btn = $(btn);
    if ( btn.hasClass("active") ) {
            btn.removeClass("active");
    } else {
            btn.addClass("active");
    }
}

function toggleReleaseGroup(btn) {
    var btn = $(btn);
    if ( btn.hasClass("active") ) {
            $('#file-table').DataTable().rowGroup().disable().draw();
            btn.removeClass("active");
    } else {
            $('#file-table').DataTable().rowGroup().enable().draw();
            btn.addClass("active");
    }
}
</script>
<div>
<div>
<div style="margin:10px 0">
<!-- 
Display data type(s): <div class="btn-group" role="group" aria-label="..." style="margin-right:10px">
<button type="button" class="btn btn-default" onclick="toggleDatatype(this,'raw_data')">Raw data</button>
<button type="button" class="btn btn-default active" onclick="toggleDatatype(this, 'results')">Results</button>
<button type="button" class="btn btn-default active" onclick="toggleDatatype(this, 'reports')">Reports</button>
</div>
-->
<% List<String> groups = ProjectsServlet.getGroupsForProject(request, project); 
	if ( groups.size() > 0 ) {
%>File group: <select onchange="changeGroup(this)">
<option value="">NONE</option>
<% for (String group : groups) { %>
<option><%= group %></option>
<% } %>
</select>
<% } %>
<button type="button" id="groupBtn" class="btn btn-info" onclick="toggleReleaseGroup(this)">Group by release</button>


</div>
<a onclick='back();' class="btn btn-link" id="back_button"><span class="glyphicon glyphicon-circle-arrow-left"></span> Back</a>
</div>
<table id="file-table" class="table">
<thead>
<tr>
<th></th>
<th>Filename</th>
<!--  <th>Data type</th> -->
<th>Release</th>
<th>Released date</th>
<th>Released by</th>
<th>Description</th></tr>
</thead>
</table>
</div>
<script>
// Code for using Data tables.

function displayRelease(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return '<a href="<%= request.getContextPath() %>/release/' + full.release.id + '">' + data + '</a>';
}

function displayFile(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( full.type === "dir" ) {
		return '<a onclick="changeDir(\'' + full.id + '\')">' + data + '</a>';                		                		
	} else {
		return '<a href="<%= request.getContextPath() %>/file/' + full.id + '">' + data + '</a>';                		
	}
}

function displayFileType(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( data === "dir" ) { return '<span class="glyphicon glyphicon-folder-open"></span>';} else { return '<span class="glyphicon glyphicon-file"></span>'; }
}

function displayUser(data, type, full, meta ) {
	if ( data == undefined ) { return ''; }
	if ( type === "sort" || type === "filter" || type === "type" ) { return data.id; }
	if ( data.name == undefined ) {
		return data.id;
	} else {
		return data.name + " &lt;" + data.id + "&gt;";
	}
}

function displayDataType(data, type, full, meta) {
	if ( data === "dir" ) {
		if ( type === "filter" ) return "raw_data,report,results";
		return "";
	}
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	switch(data) {
		case "raw": return "Raw data";
		case "result": return "Result";
		case "report": return "Report";
		default: return data;
	}
	
}

$('#file-table').DataTable({
        data: currFiles,
        order: [ [0, 'asc'] ],
        columns: [
                { data: 'type', render: displayFileType }, 
                { data: 'name', render: displayFile },
//                { data: 'datatype', render: displayDataType },
                { data: 'release.title', render: displayRelease },
                { data: 'release.date', render: function (data, type, full, meta) {
                	if ( data == undefined ) { return ''; }
					return moment(data).format('MMM Do, YYYY h:mm A');
                } },
                { data: 'release.user', render: displayUser },
				{ data: 'description' } ],
        rowId: 'id',
        columnDefs: [ { targets: 3,  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) },
                      {targets: 0, 'width': '5px'}],
        pageLength: 25,
        "language": { "emptyTable": "No files available" },
		rowGroup: { dataSrc : 'release.title', enable: false }
});

$('#back_button').hide();
</script>
