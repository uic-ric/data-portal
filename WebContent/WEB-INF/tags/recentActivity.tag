<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<table class="table" id="release-table">
<thead>
<tr>
	<th>Release title</th>
	<th>Release date</th>
	<th>Description</th>
</tr>
</thead>
</table>

<% boolean isAdmin = request.isUserInRole("admin"); %>
<script>
function displayRelease(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return '<a href="<%= request.getContextPath() %>/<%= isAdmin ? "show-" : "" %>release/' + full.id + '">' + data + '</a>';
}

$('#release-table').DataTable({
	ajax: "<%= request.getContextPath() %>/release",
    order: [],
    columns: [
            { data: 'title', render: displayRelease },
            { data: 'date' },
            { data: 'description', width: '50%', render: displayDescription }],
    rowId: 'uuid',
    paging: false,
    searching: false,
    columnDefs: [ { targets: 1,  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ],
    pageLength: 25,
    "language": {
        "emptyTable": "No projects available"
      }
});
</script>