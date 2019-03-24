<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ tag import="edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.UsersServlet, edu.uic.rrc.portal.listener.AppConfigListener" %>
<%@ attribute name="check_user" required="false" type="java.lang.Boolean" %>
<style>

<!-- a.brand:hover { color: white; text-decoration:none; } -->
<!-- a.brand { font-family: Georgia, "Times New Roman", Times, serif } -->

</style>
<header><h1><a href="<%= request.getContextPath() %>/" class="brand"><%= AppConfigListener.getBrand()  %></a>
<% User user = null;
if ( check_user == null ) { check_user = true; }
if ( request.getRemoteUser() != null ) { 
	user = UsersServlet.getSelf(request); %>
<span style="float:right; font-size:12pt; height:100%; padding-bottom:0px; padding-top:auto;">Logged in as 
<span id="user_info" ><% if ( user.getName() != null ) { %>
<%= user.getName() %> (<%= user.getUserid() %>)
<% } else { out.print(user.getUserid()) ; } %></span>
<button type="button" class="btn btn-link" data-toggle="modal" data-target="#user_update"><span class="glyphicon glyphicon-pencil" style="color:white"></span></button>
</span><% } %></h1></header>
<% if ( user != null ) { 
	boolean mustSetToken = ( user.getToken() == null && user.getRole().equals("admin")  ); %>
<script type="text/javascript">
function updateUser(form) {
	var fullname = form.elements["fullname"].value;
	var affiliation = form.elements["affiliation"].value;
	
    if( fullname === "" || affiliation == "" ) {
    	document.getElementById("error").style.display="inline";
    } else {
    	$.ajax({
			url: "<%= request.getContextPath() %>/update-profile",
			accepts: { json: "application/json" },
			type: "POST",
			dataType:"json",
			data: { 'fullname' : fullname, 'affiliation' : affiliation }, 
			success: function (data, status, http) {
				if ( http.status == 200 ) {				
					$('#user_info').html(data.name + ' ('  + data.userid + ')');
				}
			}, 
			error: function (http, status, error) {
				alert(status);
			}
		});
		$('#user_update').modal('hide');
		document.getElementById("error").style.display="none";
    }
}

function changeUserPassword(form) {
	var current_pass = form.elements["current_pass"].value;
	var new_pass = form.elements["new_pass"].value;
	var reenter_pass = form.elements["new_pass2"].value;
	
	if ( current_pass === '' ) {
    	document.getElementById("password_error").style.display="inline";
    	document.getElementById("password_error").innerHTML="Enter your current password.";
		return;		
	} 
	if ( new_pass === '' ) {
    	document.getElementById("password_error").style.display="inline";
    	document.getElementById("password_error").innerHTML="Enter a new password.";
		return;		
	} 
    if ( new_pass !== reenter_pass ) {
    	document.getElementById("password_error").style.display="inline";
    	document.getElementById("password_error").innerHTML="New passwords do not match.";
    	return;
    } else {
    	$.ajax({
			url: "<%= request.getContextPath() %>/update-password",
			accepts: { json: "application/json" },
			type: "POST",
			dataType:"json",
			data: { 'current_pass' : current_pass, 'new_pass' : new_pass, 'new_pass2': reenter_pass }, 
			success: function (data, status, http) {
				$('#change_password').modal('hide');
			},
			error: function(http, status, error) {
			   	document.getElementById("password_error").style.display="inline";
				if ( http.status == 403 ) {
					data = http.responseJSON;
			    	document.getElementById("password_error").innerHTML=data.error;					
				} else {
			    	document.getElementById("password_error").innerHTML=error;					
				}
				
			}
		});
		document.getElementById("password_error").style.display="none";
    }
}

function showChangePassword() { 
	$('#current_pass').val('');
	$('#new_pass').val('');
	$('#new_pass2').val('');
	$('#user_update').modal('hide');
	$('#change_password').modal('show');
}
</script>
<!-- Modal -->
<div id="user_update" class="modal fade" role="dialog" data-backdrop="static" data-keyboard="false">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
	<form name="updateProfile" action="<%= request.getContextPath() %>/update-profile" method="post">
      <div class="modal-header">
      	<% if ( ! mustSetToken ) { %><button type="button" class="close" data-dismiss="modal">&times;</button><% } %>
        <h4 class="modal-title">Update User Information <% if ( user.getToken() != null ) { %><span class="badge" style="background-color: #5cb85c;"><span class="glyphicon glyphicon-ok"></span> Arvados API token set</span><% } %></h4>
      </div>
      <div class="modal-body">
		<div><span id="error" align='center' style='color: red; display: none'>Please enter all the necessary details!</span></div>
			<% if ( mustSetToken  ) { %>
			<div class="alert alert-danger">Please set the Arvados API Token to proceed further! 
				<a class="btn btn-warning btn-sm" href="https://<%= AppConfigListener.getArvadosAPIServer() %>/login?return_to=https://<%= request.getServerName() %><%= request.getContextPath() %>/admin/arv-token.jsp">Set Arvados API token</a></div>
			<%} %>
			<div class="form-group">
				<label for="fullname">Full Name</label>
				<input type="text" name="fullname" id="fullname" class="form-control" <% if (user.getName()==null) { %>placeholder="Full Name" <%}else{ %>  value="<%=user.getName()%>"<%} %>/>
			</div>
			<div class="form-group">
				<label for="affiliation">Affiliation </label>
				<input type="text" name="affiliation" id="affiliation" class="form-control" <% if(user.getAffiliation()==null||user.getAffiliation().equals("")){ %>placeholder="Affiliation" <%}else{ %>  value="<%=user.getAffiliation()%>"<%} %>/>
			</div>
      		<div align="left">
 			<button type="button" class="btn btn-default" onclick="showChangePassword()">Change password</button>
			</div>
      		<div align="left">
 			<a class="btn btn-info" href="<%= request.getContextPath() %>/ssh-keys">Manage SFTP keys <span class="badge"><%= user.getSSHKeys().size() %> key(s) set</span></a>
			</div>
      </div>
      <div class="modal-footer">
		<button id = "updateButton" type='button' class="btn btn-primary" onclick="updateUser(this.form);">Update</button>
        <button  id ="closeButton" type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
	</form>
    </div>
  </div>
</div>

<!-- Password change modal -->
<div id="change_password" class="modal fade" role="dialog" data-backdrop="static" data-keyboard="false">
  <div class="modal-dialog">
    <!-- Modal content-->
    <div class="modal-content">
	<form name="changePassword" action="<%= request.getContextPath() %>/update-password" method="post">
      <div class="modal-header">
        <h4 class="modal-title">Change password</h4>
      </div>
      <div class="modal-body">
		<div><span id="password_error" align='center' style='color: red; display: none'></span></div>
			<div class="form-group">
				<label for="current_pass">Current password</label>
				<input type="password" name="current_pass" id="current_pass" class="form-control" placeholder="Current password"/>
			</div>
			<div class="form-group">
				<label for="new_pass">New password</label>
				<input type="password" name="new_pass" id="new_pass" class="form-control" placeholder="New password"/>
			</div>
			<div class="form-group">
				<label for="new_pass2">Re-enter password</label>
				<input type="password" name="new_pass2" id="new_pass2" class="form-control" placeholder="Re-enter password"/>
			</div>
      </div>
      <div class="modal-footer">
		<button id = "updateButton" type='button' class="btn btn-primary" onclick="changeUserPassword(this.form);">Update</button>
        <button  id ="closeButton" type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
	</form>
    </div>
  </div>
</div>

<% if ( check_user ) { %>
<% if ( mustSetToken ) { %>
<script type="text/javascript">
	$('#user_update').modal('show');
	$('#closeButton').hide();
	$('#updateButton').attr('disabled','true');
</script>
<% } else if ( user.getAffiliation() == null || user.getName() == null ) { %>
<script type="text/javascript">
    $(window).on('load',function(){
        $('#user_update').modal('show');
    });
</script>
<% } } } %>