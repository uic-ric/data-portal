<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ tag import="edu.uic.rrc.portal.model.entities.ProjectMembership,edu.uic.rrc.portal.model.entities.User,java.text.DateFormat,edu.uic.rrc.portal.UsersServlet,edu.uic.rrc.portal.ProjectsServlet" %>
<%@ attribute name="users" required="true" type="java.util.List" %>
<%@ attribute name="project" required="true" %>
<%
	boolean isAdmin = request.isUserInRole("admin");
	boolean isOwner = false;
	if ( ! isAdmin ) {
		ProjectMembership currentUser = UsersServlet.getMyMembership(request, project);
        isOwner = currentUser != null ? currentUser.getOwner() == 1 : false;
	}
    if ( isAdmin || isOwner ) {
%>
<script>
function modifyUsersAction(e) {
	var action = e.form.action + "/"  + e.name;
   	$.ajax({
		url: action,
		accepts: { json: "application/json" },
		type: "POST",
		dataType: "json",
		data: { 'userid': e.value }, 
		success: function (data, status, http) {
			if ( http.status == 200 ) {		
				var datatables = $('#users-table').DataTable();
				datatables.clear();
				datatables.rows.add(data);
				datatables.draw();
			} 
		}, 
		error: function (http, status, error) {
			alert(status);
		}
	});
}

// Regex to match email addresses.
var emailre = /[A-Z0-9\._%\+\-]+@[A-Z0-9\.\-]+\.[A-Z]{2,}/i;

function addUser(form) {
	var email = form.elements["email"].value;
	
    if( email == "" ) {
		var error = document.getElementById("admin-error");
		error.innerHTML = "Please enter an email address";
		error.style.display="inline";
    } else {
    	var check = emailre.exec(email);
    	if ( check == undefined ) {
    		var error = document.getElementById("admin-error");
    		error.innerHTML = "Invalid email address! Please enter a valid email address";
    		error.style.display="inline";
    		return;
    	}
		// Use the Regex matched value.  This will clear out any non-printable characters.
    	var formdata = { 'userid' : check[0]};
		if ( form.elements["owner"].checked ) {
			formdata.owner = 1;
		}
    	$.ajax({
			url: "<%= request.getContextPath() %>/users/<%= project %>/add-user",
			accepts: { json: "application/json" },
			type: "POST",
			dataType:"json",
			data: formdata, 
			success: function (data, status, http) {
				if ( http.status == 200 ) {		
					var datatables = $('#users-table').DataTable();
					datatables.clear();
					datatables.rows.add(data);
					datatables.draw();
 				} 
			}, 
			error: function (http, status, error) {
				alert(status);
			}
		});
		$('#add_user').modal('hide');
		document.getElementById("admin-error").style.display="none";
    }
}

function displayButtons(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	var output;
	if ( data ) {
		output = '<button name="revoke-owner" value="' + full.email + '" type="button" class="btn btn-info btn-sm" onclick="modifyUsersAction(this)"><span class="glyphicon glyphicon-user"></span> Revoke ownership</button>';
	} else { 
		output = '<button name="make-owner" value="' + full.email + '" type="button" class="btn btn-warning btn-sm" onclick="modifyUsersAction(this)"><span class="glyphicon glyphicon-king"></span> Make an owner</button>';
	}
	return output + ' <button name="remove-user" value="' + full.email + '" type="button" class="btn btn-danger btn-sm" onclick="modifyUsersAction(this)"><span class="glyphicon glyphicon-remove"></span> Remove from project</button></td>';
}

</script>
<!-- add user form Modal -->
<div id="add_user" class="modal fade" role="dialog">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
	<form name="admin-form" action="<%= request.getContextPath() %>/projects/<%= project %>/users" method="post">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal">&times;</button>
        <h4 class="modal-title">Add user to project</h4>
      </div>
      <div class="modal-body">
      	<p id="admin-error" align='center' style='color: red; display: none'>Please enter all the necessary details!</p>
      		<div class="form-group"><label for="email">Email address</label><input type="text" name="email" id="email" placeholder="Email" class="form-control" autocomplete="off"/></div>		      
			<div class="form-group"><input type="checkbox" name="owner" id="owner" /> <label for="owner">Owner</label></div>
      </div>
      <div class="modal-footer">
		<button type='button' class="btn btn-primary" onclick="addUser(this.form);">Add</button>
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
	</form>
    </div>
  </div>
</div>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/typeahead.bundle.min.js"></script>
<style>
.twitter-typeahead{
width:100%;
}

.tt-menu {
  min-width: 160px;
  background-color: #fff;
  border: 1px solid #ccc;
  border: 1px solid rgba(0,0,0,.2);
  width:100%;        
}

.tt-suggestion {
  display: block;
  padding: 3px 20px;
}

.twitter-typeahead .tt-query,
.twitter-typeahead .tt-hint {
  margin-bottom: 0;
}

.tt-suggestion.tt-cursor {
  color: #fff;
  background-color: #0081c2;
}

.tt-suggestion.tt-cursor a {
  color: #fff;
}

.tt-suggestion p {
  margin: 0;
}

.tt-suggestion:hover {
	background-color: #ff0;
}

</style>
<script>

function ttUser(user) {
	if ( user.name != undefined ) {
		return user.name + ' <' + user.id + '>';
	} else {
		return user.id;
	}
	
}

$('#email').typeahead({
	classNames: {
	    input: 'form-control'
	  }
}, {
	async: true,
	source: function (query, processSync, processAsync) {
		// processSync(['This suggestion appears immediately', 'This one too']);
		return $.ajax({
			url: "<%= request.getContextPath() %>/user", 
			type: 'GET',
			data: { search: query },
			dataType: 'json',
			success: function (json) {
				return processAsync(json);
			}
		});
	},
	display: ttUser
});
</script>
<% } %>

<form id="userForm" action="<%= request.getContextPath() %>/users/<%= project %>" method="post">
	<div id="project_users">
	<table id="users-table" class="table" style="width:90%">
		<thead>
		<tr><th>Users</th><th>Affiliation</th><th></th><% if ( isAdmin || isOwner ) { %><th></th><% } %></tr>
		</thead>
	</table>
	</div>
</form>
<script>
// Bind the click-event on all input with type=submit

function displayName(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	var output;
	if ( full.name != undefined ) {
		output = full.name + ' (' + data + ')';
	} else {
		output = data;
	}
	return '<span class="glyphicon ' + ( full.owner ? 'glyphicon-king owner' : 'glyphicon-user' ) + '"></span> ' + output;
}

function displayOwner(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return data ? '<span class="label label-warning">Owner</span>' : '';
}

$('#users-table').DataTable({
    data: <%= ProjectsServlet.membersToJson(users) %>, 
    order: [ [2, 'desc'], [0, 'asc'] ],
    columns: [
            { data: 'email', render: displayName },
            { data: 'affiliation'},
            { data: 'owner', render: displayOwner } 
<% if ( isAdmin || isOwner ) { %>,  { data: 'owner', render: displayButtons, orderable: false } <% } %>
            ],
    paging: false,
    "language": {
        "emptyTable": "No users available"
    },
    rowid: 'email',
    dom: 'rt'
});
</script>
<% if ( isAdmin || isOwner ) { %>
<div id="addUser"></div>
<button id="addUserButton" class="btn btn-success btn-sm" onclick="$('#email').val(''); $('#add_user').modal('show')"><span class="glyphicon glyphicon-plus"></span> Add a user</button>
<% } %>