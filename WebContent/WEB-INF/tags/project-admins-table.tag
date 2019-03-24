<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ tag import="edu.uic.rrc.portal.model.entities.arvados.User, edu.uic.rrc.arvados.ArvadosAPI, 
edu.uic.rrc.portal.model.entities.arvados.Group, edu.uic.rrc.portal.UsersServlet,
edu.uic.rrc.portal.listener.AppConfigListener, edu.uic.rrc.portal.AdministrationServlet" %>
<%@ attribute name="project" required="true" type="edu.uic.rrc.portal.model.entities.arvados.Group" %>
<%@ attribute name="can_edit" required="false" type="java.lang.Boolean" %>

<p><button id="add_admin_button" class="btn btn-default" onclick="showAdminModal();">Add administrator</button>

<div class="table-responsive">
<table class="table" id="admin-table" style="width:100%">
<thead><tr><th>Admin</th><th>Permission</th></tr></thead>
<tbody></tbody>
</table>
</div>

<script type="text/javascript">
var ARVADOS_API_HOST = "<%= AppConfigListener.getArvadosAPIServer() %>";
var ARVADOS_API_TOKEN = "<%= UsersServlet.getSelf(request).getToken() %>";
var CURRENT_PROJECT = "<%= project.getUuid()  %>";

var itemcount = 0;

// Function to process the results of a link.
// Will get the user object, if the link tail is a user
// Will get the group object, if the link tail is a group
function processAdmins(data, inherited) {
	itemcount = data.items.length;
	
	for ( var i = 0; i < data.items.length; i++ ) {
		var item = data.items[i];
		var parts = item.tail_uuid.split('-');
		item.inherited = inherited;
		if ( parts[1] == "tpzed" ) {
			// User
			getArvadosUser(item, item.tail_uuid, 'tail');
		} else {
			getArvadosGroup(item, item.tail_uuid, 'tail');
		}
	}
}

// Function to get an arvados user
// Will add the arvados user object to the item using the field specified
function getArvadosUser(item, uuid, field) {
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/users/" + uuid,
		accepts: "application/json",
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN }
	}).done(function(data) {
		item[field] = data;
		adminTable.row.add(item);
		itemcount--;
		if ( itemcount < 1 ) {
			adminTable.draw();
		}
	}).fail(function(request, status, error) {
		console.log(status);
	});	
}

//Function to get an arvados group
//Will add the arvados group object to the item using the field specified
function getArvadosGroup(item, uuid, field) {
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/groups/" + uuid,
		accepts: "application/json",
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN }
	}).done(function(data, status, req) {
		console.log(uuid + " done");
		item[field] = data;
	}).fail(function(request, status, error) {
		item[field] = { 'name' : uuid, 'uuid': uuid };
	}).always(function() {
		console.log(uuid + " always");
		adminTable.row.add(item);
		itemcount--;
		if ( itemcount < 1 ) {
			adminTable.draw();
		}		
	});	
}

// Function will load the permission links for a project.
function loadAdmins(uuid, parent=false) {
	// If this is not a parent group, Then clear the table.
	if ( ! parent ) { 
		itemcount = 0;
		adminTable.clear();
	}
	// Get the permissions for the project
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/permissions/" + uuid,
		accepts: "application/json",
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		traditional: true,
	}).done( 
		function(data) { processAdmins(data, parent); } 
	).fail( function (request, status, error) {
		// If the response is 403 (forbidden) and this is the original project, then hide the Add admin button
		if ( request.status === 403 && (! parent) ) { $('#add_admin_button').hide(); }
	});
	
	// Get the project owner and check if it is a group.  If so, add admins for the project.
	$.ajax({ 
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/groups/" + uuid,
		accepts: "application/json",
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		traditional: true		
	}).done( checkParent );
}

function checkParent(project) {
	var parts = project.owner_uuid.split('-');
	if ( parts[1] == "j7d0g" ) { loadAdmins(project.owner_uuid, true); }
}

// Function to display the tail object of a permission link.
// If a user, return the full name
// If a group, return the name of the group
function displayTail(data, type, row, meta) {
	var parts = data.uuid.split('-');
	var label;
	if ( parts[1] == "tpzed" ) {
		label = data.first_name + " " + data.last_name + " (user)";
	} else {
		label = data.name + " (group)";
	}
	return label;
}

function displayRole(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( full.updating ) {
		return "<h3><div class='label label-info label-lg'>Updating</div></h3>";
	} else if ( full.removing ) {
		return "<h3><div class='label label-danger label-lg'>Removing</div></h3>";
	}
	if ( full.inherited ) {
		var data = data.charAt(4).toUpperCase() + data.substring(5);
		return data + " (Inherited from <a href='<%= request.getContextPath() %>/manage-projects/" + full.head_uuid + "'>parent</a></span>)";		
	}
	return "<div class='form-group'>" + selectorForRole(full.uuid, data) + " <button class='btn btn-danger' onclick='removeUser(\"" + full.uuid + "\");'>Remove admin</button><div>";
}

function selectorForRole(uuid, role) {
	var selector = "<select onchange='updateRole(\"" + uuid + "\", this.value)' id='" + uuid + "-role'>";
	selector = selector + "<option value='can_read' " + ( role === "can_read" ? "selected" : "") + ">Read</option>";
	selector = selector + "<option value='can_write' " + ( role === "can_write" ? "selected" : "") + ">Write</option>";
	selector = selector + "<option value='can_manage' " + ( role === "can_manage" ? "selected" : "") + ">Manage</option>";
	return selector + "</select>";
}

function updateRole(uuid, role) {
	$('#' + uuid + "-role").attr('disabled', true);
	var row = adminTable.row('#' + uuid);
	row.data().updating = true;
	row.invalidate();
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/links/" + uuid,
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		accepts: { json: "application/json" },
		type: "PUT",
		dataType:"json",
		data: { 'link': { 'name': role } }, 
		success: function (data, status, http) { adminTable.row('#' + uuid).data().updating = false; loadAdmins(CURRENT_PROJECT, false); }, 
		error: function (http, status, error) { alert(error); }
	});
}

function removeUser(uuid) {
	var row = adminTable.row('#' + uuid);
	row.data().removing = true;
	row.invalidate();
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/links/" + uuid,
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		accepts: { json: "application/json" },
		type: "DELETE",
		success: function (data, status, http) { 
			loadAdmins(CURRENT_PROJECT, false); 
			$('#' + uuid + "-role").attr('disabled', false);
		}, 
		error: function (http, status, error) { alert(error); }
	});
}

var adminTable = $('#admin-table').DataTable({	
	data: [],
	columns: [
		{ data: 'tail', render: displayTail },
		{ data: 'name', render: displayRole },
	],
	rowId: 'uuid',
    order: [ [0, "asc"] ]
});

loadAdmins(CURRENT_PROJECT, false);

</script>

<div id="add_admin" class="modal fade" role="dialog" data-backdrop="static" data-keyboard="false">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
      <form>
      <div class="modal-header">
      	<button type="button" class="close" data-dismiss="modal">&times;</button>
        <h4 class="modal-title">Add Project Administrators</h4>
      </div>
      <div class="modal-body">
      
      <div style="margin-bottom: 10px;">
      	<label for="admin-role">Role:</label>
 	  	<select name="admin-role" id="admin-role">
      		<option value="can_read">Read</option>
      		<option value="can_write">Write</option>
      		<option value="can_manage">Manage</option>
      	</select>
      </div>
 
      <div>
      <ul class="nav nav-tabs" role="tablist">
		<li role="presentation" class="active"><a href="#admin_users" aria-controls="admin_users" role="tab" data-toggle="tab">Users</a>
		<li role="presentation"><a href="#admin_groups" aria-controls="admin_groups" role="tab" data-toggle="tab">Groups</a>
	  </ul>
	  </div>
	  
	  <div class="tab-content">
	  <div role="tabpanel" class="tab-pane active" id="admin_users">
		<div class="table-responsive" style="margin: 5px 0;">
			<table class="table" id="admin-users-table" style="width:100%">
	 		<thead><tr><th>Name</th><th>Email</th></tr></thead>
			<tbody></tbody>
			</table>
		</div>
	  </div>
	  <div role="tabpanel" class="tab-pane" id="admin_groups">
		<div class="table-responsive"  style="margin: 5px 0;">
			<table class="table" id="admin-groups-table" style="width:100%">
		    <thead><tr><th>Group name</th></tr></thead>
		  	<tbody></tbody>
			</table>
		</div>
	  </div>
	  </div>
	  </div>
	  <div class="modal-footer">
	  	<button id="addAdminButton" type='button' class="btn btn-primary" onclick="addAdmin(this.form);">Add</button>
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
	  </div>
	  </form>
    </div>
  </div>
</div>

<script type="text/javascript">

//Function will load available users
function loadAdminUsers(offset=0) {
	if ( offset == 0 ) { arvUsersTable.clear(); }
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/users",
		accepts: "application/json",
		data: {
			offset: offset,
			select: '["uuid", "first_name", "last_name", "email"]',
			limit: 50,
		},
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		traditional: true,
	}).done(processAdminUsers);
}

//Function to process the users
function processAdminUsers(data) {
	arvUsersTable.rows.add(data.items);
	if ( data.items_available > (data.offset + data.items.length) ) {
		loadAdminUsers(data.offset + data.items.length);
	} else {
		arvUsersTable.draw();
	}
} 

//Function will load available users
function loadAdminGroups(offset=0) {
	if ( offset == 0 ) { arvGroupsTable.clear(); }
	$.ajax({
		url: "https://" + ARVADOS_API_HOST + "/arvados/v1/groups",
		accepts: "application/json",
		data: {
			offset: offset,
			filters: '[["group_class", "=", "role"]]',
			select: '["uuid", "name" ]',
			limit: 50,
		},
		headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
		traditional: true,
	}).done(processAdminGroups);
}

//Function to process the users
function processAdminGroups(data) {
	arvGroupsTable.rows.add(data.items);
	if ( data.items_available > (data.offset + data.items.length) ) {
		loadAdminGroups(data.offset + data.items.length);
	} else {
		arvGroupsTable.draw();
	}
} 
function showAdminModal() { 
	$('#add_admin').modal('show');
	loadAdminUsers();
	loadAdminGroups();
}

var arvUsersTable = $('#admin-users-table').DataTable({	
	data: [],
	columns: [ 
		{ data: 'uuid', render: displayAdminUser },
		{ data: 'email' }
	],
    order: [ [0, "asc"] ]
});

function displayAdminUser(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return full.first_name + ", " + full.last_name; }
	return "<input type='radio' name='target' value='" + data + "'> " + full.first_name + " " + full.last_name;
}

var arvGroupsTable = $('#admin-groups-table').DataTable({	
	data: [],
	columns: [ 
		{ data: 'uuid', render: displayAdminGroup }
	],
    order: [ [0, "asc"] ]
});

function displayAdminGroup(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return full.name; }
	return "<input type='radio' name='target' value='" + data + "'> " + full.name;
}

function addAdmin(form) {
	var role = form.elements["admin-role"].value;
	var uuid = form.elements['target'].value;
	
	if ( uuid != undefined ) {
		$.ajax({
			url: "https://" + ARVADOS_API_HOST + "/arvados/v1/links",
			headers: { "Authorization": "OAuth2 " + ARVADOS_API_TOKEN },
			accepts: { json: "application/json" },
			type: "POST",
			dataType:"json",
			data: { 'link' : { 'head_uuid' : "<%= project.getUuid() %>",
				'link_class' : "permission",
				'name': role,
				'tail_uuid': uuid } }, 
			success: function (data, status, http) {
				var parts = uuid.split('-');
				itemcount++;
				if ( parts[1] == "tpzed" ) {
					// User
					getArvadosUser(data, uuid, 'tail');
				} else {
					getArvadosGroup(data, uuid, 'tail');
				}
			}, 
			error: function (http, status, error) {
				alert(error);
			}
		});
		$('#add_admin').modal('hide');		
	}
	
}
</script>
