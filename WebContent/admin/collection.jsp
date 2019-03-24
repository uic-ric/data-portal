<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.util.List, java.util.Map,edu.uic.rrc.portal.model.entities.arvados.Collection,edu.uic.rrc.portal.model.entities.arvados.Collection.CollectionFile,edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.UsersServlet,edu.uic.rrc.portal.model.entities.ProjectFiles,edu.uic.rrc.portal.AdministrationServlet, com.google.gson.JsonObject, com.google.gson.Gson" %>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Projects</title>
<style>
	table.table tr td input { margin-right: 10px; }
	a#parent_dir { font-size: 12pt; font-weight: bold; }
	span.icon-hidden { color: orange; }
	span.released { color: green; }
	span.visible-icon { color: SkyBlue; }
	span.hidden-eye { color: DarkGray; }
</style>
<% User user = null;
if ( request.getRemoteUser() != null ) { user = UsersServlet.getSelf(request);} %>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/tables.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/collection-table.js"></script>
<script src="//cdnjs.cloudflare.com/ajax/libs/textile-js/2.0.4/textile.min.js"></script>
<%	Collection collection = AdministrationServlet.getCurrentCollection(request); %>
<script type="text/javascript">
var allFiles = <%= AdministrationServlet.fileJson(request, collection).toString() %>;
buildCollectionFileTree(allFiles);
checkDirs();
</script>
</head>

<body>
<portal:header/>
<portal:menu active="projects"/>
<div id="content">
<% 	if ( request.getAttribute("message") != null ) { %>
<div style="height: 35px; width: 50%; font-size :20px;" class="message"><%= (String) request.getAttribute("message") %></div>
<%	}
	if ( collection != null ) { 
		String name = collection.getName();
		if ( name == null || name.length() < 1 ) { name = "Collection: " + collection.getUuid(); }
	%>
<h2><%= name %></h2>
<a href="<%= request.getContextPath() %>/manage-projects/<%= collection.getOwnerUUID() %>" class="btn btn-link"><span class="glyphicon glyphicon-circle-arrow-left"></span> Return to project view</a>	
<% String description = collection.getDescription();
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
<h3>Files</h3>

<div class="modal fade" id="renameModal" role="dialog">
	<div class="modal-dialog modal-lg">
		<div class="modal-content">
			<div class="modal-header">
				<button type="button" class="close" data-dismiss="modal">&times;</button>
				<h4 class="modal-title">Rename File</h4>
			</div>
			<div class="modal-body">
				<div id="renameForm" class="form-group">
					<label class="col-sm-2 control-label" for="inputFname3">FileName:
					</label><input id="newFilename" type="text" name="newFilename" /> <br />
					<input id="confirm-btn" class="btn btn-primary" type="button"
						value="Confirm" name="ok" style="float: right" /> <br />
				</div>
			</div>
		</div>
	</div>
</div>
<!-- Ajax call to rename the file in collection -->
<script type="text/javascript">          
     $(document).ready(function() {
   	    $('#confirm-btn').click(function() {
   	    	var oldfilename = $('#myfieldname').val(); 
   			var newFilename = $('#newFilename').val(); 
     		$.ajax({
				url : '<%= request.getContextPath() %>/manage-projects/<%= collection.getOwnerUUID() %>/<%= collection.getUuid() %>',
				type: "POST",
				accepts: { json: "application/json" },
				dataType:"json",
				data :{ 
					myfieldname : $('#myfieldname').val(),
					ok : $('#confirm-btn').val(),
					newFilename : $('#newFilename').val()   				
				}, 
				success : function(data) {  	
					var newid = '<%= collection.getUuid() %>';
					newid = newid+"/"+newFilename;
					for (var i = 0; i < allFiles.length; i++) {
						if (allFiles[i].id === oldfilename) {  
							allFiles[i].name = newFilename;
							allFiles[i].id = newid;
				    		break;
				  		}
					}
					reloadTable();
					$('#renameModal').modal('hide');  			
				},
				error : function(jqXHR, textStatus, errorThrown) {
					if (jqXHR.status == 500) {
		                alert('Internal error: ' + jqXHR.responseText);
		            } else if(jqXHR.status == 422){
		            	alert("Error! "+newFilename+" already exists!");
		                $('#renameModal').modal('hide');                    
		            }
		    	}
			
			});
   	 	});
    });
</script>

<form id="collectionForm"  method="post" >

<div class="row">
<!--  The file selection panel -->
<div class="col-md-10 col-lg-8 col-sm-12">

<div class="row" style="margin:10px">
<div class="col-md-6">
<div class="btn-group">
<label class="btn btn-default"><input type="checkbox" onclick="selectAllFiles(this.checked)"> Select all files</label>
<label class="btn btn-default"><input type="checkbox" onclick="hideAllFiles(this.checked)"> Hide all files</label>
<label class="btn btn-default"><input type="checkbox" onclick="revokeAllFiles(this.checked)"> Revoke all files</label>
</div>
</div>

<div class="form-horizontal">
<div class="form-group">
<div class="col-md-2" style="text-align:right">
<label for="root_path" class="control-label">Parent file path</label>
</div>
<div class="col-md-4">
<input type="text" class="form-control" name="parent_path" placeholder="/" data-toggle="tooltip" data-placement="top" title="Provide a parent path for all files.  This path will be prepended to the files selected.">
</div>
</div>
</div>
</div>

<div class="row">
<div class="col-md-8">
<p><a id="back-button" onclick='back();' class="btn btn-link btn-lg" style="display:none"><span class="glyphicon glyphicon-level-up"></span> Previous directory</a></p>
</div>
</div>

<table id="file-table" class="table" style="width:100%">
<thead>
<tr>
<th></th>
<th>Filename</th>
<th>Size</th>
<th>Description</th>
<th>Data type</th>
<th>Hidden</th>
<th>Revoke</th>
</tr>
</thead>
</table>

<script>
var contextPath = "<%= request.getContextPath() %>";
var portableHash = "<%= collection.getPortableDataHash() %>"
$(function () { $('[data-toggle="tooltip"]').tooltip() });
// Code for using Data tables.
$('#file-table').DataTable({
        data: currFiles,
        order: [ [0, 'asc'] ],
        columns: [
                { data: 'type', render: function (data, type, full, meta ) {
                	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
                	if ( data === "dir" ) { return '<span class="glyphicon glyphicon-folder-open"></span>';} else { return '<span class="glyphicon glyphicon-file"></span>'; }
                }}, 
                { data: 'name', render: displaySelection },
                { data: 'size', render: function (data, type, full, meta) {
                	if ( full.type === "dir" ) {
						return '';                		                		
                	} else {
                    	if ( type === "sort" || type === "filter" || type === "type" ) { return data; 
                    	} else { return humanSize(data); }
                	}}},
				{ data: 'description', render: displayDescription },
                { data: 'filetype', render: displayDataType}, 
				{ data: 'hide', render: displayHidden },
				{ render: displayRevoke }],
        rowId: 'id',
        columnDefs: [ {targets: 0, 'width': '5px'},
        	{targets: [2,4], width: '20px' },
        	{targets: [5,6], 'width': '15px'} ],
        pageLength: 25,
        "language": {
            "emptyTable": "No files available"
        },
        buttons: [ { text: 'Revoke', action: function(e, dt, node, config) { console.log("nothing"); } }]
});

</script>
</div>
<div class="col-md-2 col-lg-4 col-sm-6">
<div class="panel panel-default">
<div class="panel-heading">
Release details
</div>
<div class="panel-body">
<div class="form-group">
<label for="release_title">Release title</label> <input class="form-control" id="release_title" placeholder="Release title" type="text" name="release_title">
</div>
<div class="form-group">
<label for="data_type">Default data type: </label>
<select name="data_type" class="form-control">
<option value="raw">Raw data</option>
<option value="report">Reports</option>
<option value="result">Results</option>
</select>
</div>
<div class="form-group"><label for="release_notes">Release notes</label>
<textarea rows="5" cols="60" name="release_notes" class="form-control" placeholder="Notes"></textarea>
</div>
</div>
</div>
<div class="pull-right">
<button class="btn btn-primary" type="submit" name="action" value="release-files" onclick="return validateRelease(this.form)">Release Files</button>
<button class="btn btn-default" type="submit" name="action" value="revoke-files" onclick="return validateRevoke(this.form)" >Revoke Files</button>
</div>
</div>
</div>  
</form>

<% } %>
</div>
</body>
</html>