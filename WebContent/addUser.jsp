<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Add User to Project</title>
</head>
<body>
<!-- projectId got from usertable.tag -->
<form action="<%=request.getContextPath() %>/users/<%= request.getParameter("projectId") %>/add-user" method="post">
	User ID: <input id="userid" name="userid">
	Owner: <input type="checkbox" name="owner" value="1">
	<button type="button">Add User</button>
<script>
$("#addUser button").click(addUsersAction);
</script>
</form>
</body>
</html>