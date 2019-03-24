<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ attribute name="project" required="true" %>
<table class="table" id="upload-file-table" style="width:90%">
<thead><tr><th>File</th><th>Upload date</th><th>Size</th></tr></thead>
</table>

<script>
function displayFile(data, type, full, meta ) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
<% if ( request.isUserInRole("admin") ) { %>
	return '<input type="checkbox" value="' + data + '" name="file"> <a href="<%= request.getContextPath() %>/view-file/<%= project %>/' + data + '">' + data + '</a>';
<% } else {%>
	return data;
<% } %>
}

function humanSize(size) {
	if (size == 0) { return "0.0 B"; }
	var e = Math.floor(Math.log(size) / Math.log(1024));
	var units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB'];
	if ( e == 0 ) { return size + ' B'; }
	return (size / Math.pow(1024, e)).toFixed(1) + ' ' + units[e];
}

function displaySize(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	return humanSize(data);
}

$('#upload-file-table').DataTable({
	ajax: "<%= request.getContextPath() %>/projects/<%= project %>?upload",
    order: [],
    columns: [
            { data: 'name', render: displayFile },
            { data: 'date' },
            { data: 'size', render: displaySize }],
    rowId: 'name',
    paging: false,
    searching: false,
    columnDefs: [ { targets: 1, render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ],
    pageLength: 25,
    "language": {
        "emptyTable": "No files available"
      }
});
</script>
<% if ( request.isUserInRole("admin") ) { %>
<script>
function selectDest() { 
	$('#error-message').hide();
	selproject.id = "<%= project %>";
	var files = [];
	
	$.each($("input[name='file']:checked"), function() { files.push($(this).val());});
	
	if ( files.length == 0 ) {
		$('#error-message').show();
		return;
	}
	
	var now = new Date();
	setNewCollectionName('Uploaded files - ' + now.toDateString() + ' ' + now.toLocaleTimeString()); 
	openArvCollectionForm( function(project,collection) { addFile(project, collection, files); });
}

function addFile(project, collection, files) {
	var formdata = { 'files': files };
	
	if ( collection.id === '_NEW_' ) {
		formdata['collection_name'] = collection.name		
	} else {
		formdata['collection_id'] = collection.id
	}
	
	
    $.ajax({
		url: "<%= request.getContextPath() %>/add-files/<%= project %>",
		accepts: { json: "application/json" },
		type: "POST",
		data: formdata, 
		success: function (data, status, http) {
			if ( http.status == 200 ) {		
				var arr = [];
				 $("input[name='file']:checked").each(function () {
				    //arr.push($(this).val());
				    $(this).replaceWith("<span value="+files+" name="+files+" class='glyphicon released glyphicon-ok'></span>")
				}); 
				//alert(arr);
				//glyphicon released glyphicon-ok
				//$('#upload-file-table').DataTable().ajax.reload();
				
 			} 
		}, 
		error: function (http, status, error) {
			alert(status);
		}
	});
}

function removeFiles() {
	var files = [];
	$.each($("input[name='file']:checked"), function() { files.push($(this).val());});
	var formdata = { 'files': files };
    $.ajax({
		url: "<%= request.getContextPath() %>/del-files/<%= project %>",
		accepts: { json: "application/json" },
		type: "POST",
		data: formdata, 
		success: function (data, status, http) {
			if ( http.status == 200 ) {	
				$('#upload-file-table').DataTable().ajax.reload();
 			}			
		}, 
		error: function (http, status, error) {
			alert(status);
		}
		
	});
    $('#delete-files-modal').hide();
    
}
</script>

<portal:arv-modal/>

<!-- Bootstrap modal to confirm deleting files -->
<div id="delete-files-modal" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg" role="document">
<div class="modal-content">
<div class="modal-header">
<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
<h4 class="modal-title">Confirm file deletion</h4>
</div>
<div class="modal-body">

</div>
<div class="modal-footer">
<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
<button type="button" class="btn btn-danger" onclick="removeFiles()" data-backdrop="false" data-dismiss="modal" >Delete</button>
</div>
</div>
</div>
</div>

<div style="margin-top:10px">
<div id="error-message" class="alert alert-danger alert-dismissable" style="display:none; width: 50%">
  <a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a>
  <strong>Error:</strong> Please select files to add/delete. 
</div>
<p><button class='btn btn-primary' onclick='selectDest()'>Add files</button> <button  class="btn btn-danger" onclick="$('#delete-files-modal').modal('show')">Delete files</button></p>
</div>
<% } %>