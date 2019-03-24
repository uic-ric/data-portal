<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.arvados.Group,edu.uic.rrc.portal.listener.AppRequestListener,
edu.uic.rrc.portal.model.entities.User,edu.uic.rrc.portal.model.entities.SSHKey,edu.uic.rrc.portal.listener.AppConfigListener,
	javax.persistence.EntityManager, javax.persistence.EntityTransaction, javax.persistence.RollbackException" %>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title><%= AppConfigListener.getBrand() %></title>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/base.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/bootstrap.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/datatables.min.css"/>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/jstree.min.css"/>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery-3.2.1.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jstree.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/bootstrap.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/moment.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/tables.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/jquery.dataTables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datatables.min.js"></script>
<script type="text/javascript" charset="utf8" src="<%= request.getContextPath() %>/resources/js/datetime.js"></script>
</head>
<body>
<portal:header/>
<portal:menu/>
<body>
<div id="content">
<div class="page-header"><h1>SFTP Access</h1></div>
<% User user = AppRequestListener.getSelf(request);	
EntityManager em = AppRequestListener.getEntityManager(request);
if ( request.getParameter("sshkey") != null ) {
	try {
		EntityTransaction txn = em.getTransaction();
		txn.begin();
		String key = request.getParameter("sshkey");
		user.addSSHKey(key); 		
		txn.commit(); %>
<div class="alert alert-success alert-dimissable"><strong>Success:</strong> added SSH key.</div>	
<% 
	} catch (RollbackException e) { 
		Throwable cause = e.getCause();
		String message;
		cause = ( cause.getCause() != null ? cause.getCause() : cause);
		if ( cause instanceof org.hibernate.exception.ConstraintViolationException ) {
			String name = ((org.hibernate.exception.ConstraintViolationException)cause).getConstraintName();
			if ( name.equalsIgnoreCase("ssh_sha256") || name.equalsIgnoreCase("ssh_md5") ) {
				message = "Key already exists!  Please enter a new, unique public key.";				
			} else {
				message = cause.getLocalizedMessage();				
			}
		} else if ( cause != null ) {
			message = e.getLocalizedMessage();
		} else {
			message = e.getLocalizedMessage();
		}
		em.refresh(user);
%>
<div class="alert alert-danger alert-dimissable"><strong>Error:</strong> <%= message %></div>		
<%	} 
} else if ( request.getParameter("del") != null ) {
		EntityTransaction txn = em.getTransaction();
		txn.begin();
		user.removeKey(request.getParameter("del"));
		txn.commit(); %>
<div class="alert alert-success alert-dimissable"><strong>Success:</strong> deleted SSH key.</div>	
<% } %>

<p>In order download files via SFTP, a SSH public key must be associated with your user account.  
If you do not already have an SSH keypair (public and private key), you will need to generate a keypair using an SSH key generation utility.  
The public key must be the SSH public keyfile format.  The following is an example of a SSH public in the proper format.
</p>

<div class="row">
<pre style="margin-left:40px; width:43em">
ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEAklOUpkDHrfHY17SbrmTIpNLTGK9Tjom/BWDSU
GPl+nafzlHDTYW7hdI4yZ5ew18JH4JW9jbhUFrviQzM7xlELEVf4h9lFX5QVkbPppSwg0cda3
Pbv7kOdJ/MTyBlWXFCR+HAo3FXRitBqxiX1nKhXpHAZsMciLq8V6RjsNAQwdsdMFvSlVK/7XA
t3FaoJoAsncM1Q9x5+3V0Ww68/eIFmb1zuUFljQJKprrX88XypNDvjYNby6vw/Pb0rwert/En
mZ+AW4OZPnTPI89ZPmVMLuayrD2cE86Z/il8b+gw3r3+1nKatmIkjn2so1d01QraTlMqVSsbx
NrRFi9wrf+M7Q== user@somwhere.com
</pre>
</div>

<p>Once you have a SSH key pair, copy <b><i>ONLY the public key</i></b> and paste into the <strong>New SSH Key</strong> field below and click <strong>Add key</strong>.</p>



<form method="post" class="form-horizontal">
<div class="form-group">
<div class="col-sm-8">
<label for="sshkey">New SSH Key</label>
<textarea rows="4" cols="40" name="sshkey" class="form-control"></textarea>
</div>
</div>
<div class="form-group">
<div class="col-sm-8">
<button type="submit" class="btn btn-primary">Add key</button>
</div>
</div>
</form>

<%	if ( user.getSSHKeys().size() > 0 ) { %>

<!-- 

<div class="row">

<div class="col-sm-4 col-sm-offset-1">
<div class="form-horizontal">
<div class="form-group">
<label>SFTP host</label><div>data.cri.uic.edu</div>
</div>
<div class="form-group">
<label>SFTP username</label><div>fetch</div>
</div>
</div>
</div>

</div>
-->
<div class="panel panel-default">

<div class="panel-heading" role="tab" id="keysHeading">
<h4 class="panel-title"><a href="#keysContent" role="button" data-toggle="collapse">Current SSH Keys</a></h4>
</div>

<div id="keysContent" role="tabpanel" class="panel-collapse collapse in">
<div class="panel-body">

<table class="table">
<thead>
<tr><th>Finger print</th>
<th>Date added</th>
<th>Description</th>
<th></th>
</tr>
</thead>
<tbody>
<% for ( SSHKey key : user.getSSHKeys() ) { %>
<tr><td><%= key.getMD5Hash() %></td>
<td><%= key.getDateAdded() %></td>
<td><%= key.getInfo().replace(">", "&gt;").replace("<", "&lt;") %></td>
<td><form method="post"><input type="hidden" name="del" value="<%= key.getMD5Hash() %>"><button class="btn btn-xs btn-danger" type="submit"><span class="glyphicon glyphicon-remove"></span> Delete</button></form></td>
</tr>
<% } %>
</tbody>
</table>

</div>
</div>
</div>

<% } %>

</div>
</body>
</html>