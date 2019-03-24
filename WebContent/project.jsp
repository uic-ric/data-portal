<%@page import="edu.uic.rrc.portal.model.entities.UploadedFile"%>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.arvados.Group,
	java.util.List,edu.uic.rrc.portal.model.entities.Release,java.text.DateFormat,
	java.util.Date,edu.uic.rrc.portal.model.entities.ProjectMembership,
	edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.UsersServlet,
	edu.uic.rrc.portal.listener.AppConfigListener,
	java.util.Set, java.util.HashSet, java.util.Arrays" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<% Group project = ProjectsServlet.getCurrentProject(request); 
	User user = UsersServlet.getSelf(request);
%>
<title><%= AppConfigListener.getBrand() %> - <%= project != null ? project.getName() : "Project List" %></title>
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
<script></script>
<script src="//cdnjs.cloudflare.com/ajax/libs/textile-js/2.0.4/textile.min.js"></script>
<script type="text/javascript">
function validateDownload() {
    if (!$('input[name="zipList"]').is(':checked')) {
        alert("Please check atleast one file to download");
        return false;
    }
}
</script>
<style>
	dl.activity > dt { padding-bottom: 10px; font-size: 12pt;}
	dl.activity > dd { margin-left: 10px; margin-bottom: 10px;}

		a .hover-img { 
  position:relative;
 }
a .hover-img span {
  position:absolute; left:-9999px; top:-9999px; z-index:9999;
 }
a:hover .hover-img span { 
  top: 20px; 
  left:0;
 }
	
	
</style>
</head>
<body>
	<portal:header/>
	<portal:menu active="projects"/>
		<div class="modal fade" id="myModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel">
  <div class="modal-dialog" role="document">
  <div class="modal-content">
        <div class="modal-header">
          <button type="button" class="close" data-dismiss="modal">&times;</button>
          <h4 class="modal-title">SFTP Download</h4>
        </div>
        <div class="modal-body">
         <p>To download use a SFTP client such as 
          <ul>
          <li ><a href="https://filezilla-project.org/download.php?" target="_blank"><div class="hover-img">
  FileZilla
   <span><img src="<%= request.getContextPath() %>/resources/images/fetch-sftp.PNG" align='left' alt="image" height="350" />      </span>
</div></a></li>
          <li><a href="https://winscp.net/eng/download.php" target="_blank">WinSCP (For Windows only)</a> , etc.</li>
          </ul>
          Use the following connection parameter:
          <ul>
          <li>hostname : <%= AppConfigListener.getSFTPHost() != null ? AppConfigListener.getSFTPHost() : request.getServerName() %></li>
          <li>username : <%= AppConfigListener.getSFTPUser() %></li>
          </ul>
          Set the SSH keys if not already set
          <a class="btn btn-info btn-sm" href="/ssh-keys">Manage SFTP keys</a>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        </div>
      </div>   
      </div>
    </div>
		<div id="content">
			<% 
				if ( project != null ) {
					
					
				String view = ( request.getParameter("files") != null ? "files" : 
						request.getParameter("activity") != null ? "activity" :
							request.getParameter("users") != null ? "users" :
								request.getParameter("zip") != null ? "zip" :
									request.getParameter("upload") != null ? "upload" :
									"files"); 
				String description = project.getDescription(); %>
			<h1><%=project.getName()%>
			<% if ( request.isUserInRole("admin") ) { %>
			<a href="<%= request.getContextPath() %>/manage-projects/<%= project.getUuid() %>" class="btn btn-danger btn-sm" style="float:right"><span class="glyphicon glyphicon-pencil"></span> Manage project</a> 
			<% } %>
			</h1>
			<% 	if ( description != null && description.length() > 0) { %>
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
				<li role="presentation"<% if ( view.equals("files") ) { %> class="active"<% } %>><a href="#files" aria-controls="files" role="tab" data-toggle="tab">Files</a></li>
				<li role="presentation"<% if ( view.equals("activity") ) { %> class="active"<% } %>><a href="#activity" aria-controls="activity" role="tab" data-toggle="tab">Activity</a></li>
				<li role="presentation"<% if ( view.equals("users") ) { %> class="active"<% } %>><a href="#users" aria-controls="users" role="tab" data-toggle="tab">Users</a></li>
				<li role="presentation"<% if ( view.equals("upload") ) { %> class="active"<% } %>><a href="#upload" aria-controls="upload" role="tab" data-toggle="tab">Uploaded Files</a></li>
			</ul>
			
			 <div class="tab-content">
			 	<div role="tabpanel" class="tab-pane<% if ( view.equals("files") ) { %> active<% } %>" id="files">
			 		<div class="row">
			 			<div class="col-sm-12">
						<a href="<%= request.getContextPath() %>/projects/<%= project.getUuid() %>?zip" class="btn btn-link"><span class="glyphicon glyphicon-import"></span> Download ZIP archive</a>			 			
						<% String sftpURL = ( user.getSSHKeys().size() == 0 ? request.getContextPath() + "/ssh-keys" : ProjectsServlet.getSFTPURL(request.getServerName(), project.getUuid()) ); %>				
						<a data-toggle="modal" data-target="#myModal" class="btn btn-link"><span class="glyphicon glyphicon-cloud-download"></span> SFTP Download</a>		 			
			 			</div>
					</div>
					
					<div>
					<div style="margin:10px 0">
					<% Set<String> types = new HashSet<String>(); 
						if ( request.getParameter("type") != null ) {
							types.addAll(Arrays.asList(request.getParameterValues("type")));
						} else {
							types.add("report"); types.add("result"); types.add("raw");	
						}
					%>
						Display data type(s): 						
						<div class="btn-group" role="group" aria-label="..." style="margin-right:10px" data-toggle="buttons">
						<label class="btn btn-default<% if (types.contains("raw")) { %> active<%}%>" role="button">
							<input type="checkbox" name="type" value="raw" autocomplete="off" onchange="updateDataType();"<% if (types.contains("raw")) { %> checked<%}%>>Raw data
						</label>
						<label class="btn btn-default<% if (types.contains("result")) { %> active<%}%>">
							<input type="checkbox" name="type" value="result" autocomplete="off" onchange="updateDataType()"<% if (types.contains("result")) { %> checked<%}%>>Results
						</label>
						<label class="btn btn-default<% if (types.contains("report")) { %> active<%}%>">
							<input type="checkbox" name="type" value="report" autocomplete="off" onchange="updateDataType()"<% if (types.contains("report")) { %> checked<%}%>>Reports
						</label>
						</div>
						
						Group by: <div class="btn-group" role="group" aria-label="..." style="margin-right:10px" data-toggle="buttons">
						<label class="btn btn-default active" role="button">
							<input type="radio" name="group" value="" autocomplete="off" onchange="$('#file-table').DataTable().rowGroup().disable().draw();">None
						</label>
						<label class="btn btn-info" role="button">
							<input type="radio" name="group" value="release.title" autocomplete="off" onchange="toggleGroup(this)">Release
						</label>
						<label class="btn btn-info" role="button">
							<input type="radio" name="group" value="datatype" autocomplete="off" onchange="toggleGroup(this)">Data Type
						</label>
						</div>
					</div>
					<ol class="breadcrumb">
<% String path = (String)request.getAttribute("path"); 
	if ( path == null ) { %>
					<li><i>Root folder</i></li>
<% } else { 
	String[] paths = path.split("/+"); %>
					<li><a href="<%= request.getContextPath() %>/projects/<%= project.getUuid() %>"><i>Root folder</i></a></li>
					
<% 
	StringBuffer fullPath = new StringBuffer("/");
	for ( int i = 0 ; i < (paths.length - 1) ; i++ ) { 
		fullPath.append(paths[i]);
		fullPath.append("/");
	%>
					<li><a href="<%= request.getContextPath() %>/projects/<%= project.getUuid() %><%= fullPath.toString() %>"><i><%= paths[i] %></i></a></li>	
<% 	} %>
					<li><%= paths[paths.length - 1] %></li>
<% 
} %>				</ol>
					</div>
					
					<table id="file-table" class="table" >
					<thead>
					<tr>
					<th></th>
					<th>Filename</th>
					<th>Data type</th>
					<th>Release</th>
					<th>Released date</th>
					<th>Released by</th>
					<th>Description</th></tr>
					</thead>
					</table>
				<script>
				
				function updateDataType() {
					$('#file-table').DataTable().ajax.reload();
					
					// removeClass('focus');
				}

				function toggleGroup(input) {
				    var table = $('#file-table').DataTable();
				    if ( input.checked ) {
					    table.rowGroup().dataSrc(input.value);
			            table.rowGroup().enable().draw();
				    } else {
			            table.rowGroup().disable().draw();
				    }
				}
								
				function displayRelease(data, type, full, meta) {
					if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
					return '<a href="<%= request.getContextPath() %>/release/' + full.release.id + '">' + data + '</a>';
				}

				function displayFile(data, type, full, meta) {
					if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
					if ( full.type === "dir" ) {
						return '<a href="<%= request.getContextPath() %>/projects/<%= project.getUuid() %>' + full.id + '">' + data + '</a>';                		                		
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
				        ajax:  {
				        	url: '<%= request.getContextPath() %>/projects/<%= project.getUuid() %>/<%= path != null ? path : "" %>',
				        	data: function(d) {
				        		d.type = [];
								$('input[name="type"]:checked').each(function () {d.type.push($(this).val())});				        		
				        	},
				        	traditional: true
				        },
				        order: [ [0, 'asc'] ],
				        columns: [
				                { data: 'type', render: displayFileType }, 
				                { data: 'name', render: displayFile },
				                { data: 'datatype', render: displayDataType },
				                { data: 'release.title', render: displayRelease },
				                { data: 'release.date' },
				                { data: 'release.user', render: displayUser },
								{ data: 'description' } ],
				        rowId: 'id',
				        columnDefs: [ { targets: 4,  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM D, YYYY h:mm A' ) },
				                      {targets: 0, 'width': '5px'}],
				        pageLength: 25,
				        "language": { "emptyTable": "No files available" },
						rowGroup: { dataSrc : 'release.title', enable: false }
				});
				</script>
			 	</div>
<!-- Panel for activity view -->
			 	<div role="tabpanel" class="tab-pane<% if ( view.equals("activity") ) { %> active<% } %>" id="activity">
			 		<% List<Release> releases = ProjectsServlet.getReleasesForProject(project, request); %>
			 		<div class="row" style="margin: 10px">
			 		<div class="col-sm-12">
			 			<dl class="activity">		 			
			 			<% 
			 			DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT);
			 			for ( Release release : releases ) { 
			 				Date releaseDate = release.getRelease_date(); %>
			 				<dt><a href="<%= request.getContextPath() %>/release/<%= release.getReleaseID() %>"><%= release.getTitle() %></a> - <%= format.format(releaseDate) %></dt>
    						<dd><%= release.getDescription() %></dd>
    					<% } %>
    					</dl>
					</div>
			 		</div>
			 	</div>
<!-- Panel for users view -->
			 	<div role="tabpanel" class="tab-pane<% if ( view.equals("users") ) { %> active<% } %>" id="users">
			 		<portal:usertable users="<%= ProjectsServlet.getMembers(project, request) %>" project="<%= project.getUuid() %>"/>
			 	</div>
			 	<div role="tabpanel" class="tab-pane<% if ( view.equals("upload") ) { %> active<% } %>" id="upload">
				<div class="row">
					<portal:uploadform project="<%= project.getUuid() %>" files="<%= ProjectsServlet.getProjectUploadFiles(request) %>"/>
				</div>
				</div>
				<% if ( view.equals("zip") ) { %>
				<div role="tabpanel" class="tab-pane active" id="zip">
				<div class="row">
			 			<div class="col-sm-12">
							<portal:filetree project="<%= project.getUuid() %>"/>
			 			</div>
					</div>
				</div>
				<% } %>
			 </div>
		</div>
		<div id="upload-files"></div>
		<% } %>
	</body>
</html>