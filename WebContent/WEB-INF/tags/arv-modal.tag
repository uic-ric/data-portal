<%@ tag language="java" pageEncoding="UTF-8"%>
<!-- Bootstrap modal to choose a project -->
<div id="arv-project-form" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg" role="document">
<div class="modal-content">
<div class="modal-header">
<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
<h4 class="modal-title">Select Arvados Project</h4>
</div>
<div class="modal-body">
<div id='arv-project-error' class="alert alert-error" role="alert">
<strong>Error!</strong> Select a project before continuing
</div>
<div class="input-group">
<span class="input-group-addon">
<label>
<input type="radio" name="arv-project" value="_NEW_" onclick="selectProject(this,'')">
<strong>New project</strong></label>
</span>
<input type="text" class="form-control" aria-label="New project name" id='arv-project-name' >
</div>
<table id="arv-project-table" class="table table-striped table-bordered" cellspacing="0" width="100%">
<thead>
<tr><th>Project</th><th>Created</th><th>Modified</th></tr>
</thead>
</table>
</div>
<div class="modal-footer">
<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
<button id="arv-sel-project" type="button" class="btn btn-primary" onclick="arvNext();">Next <span class="glyphicon glyphicon-chevron-right"></span></button>
</div>
</div>
</div>
</div>

<!-- Bootstrap modal to set a new collection (for a new project) -->
<div id="arv-new-collection-form" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg" role="document">
<div class="modal-content">
<div class="modal-header">
<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
<h4 class="modal-title">New Arvados Collection</h4>
</div>
<div class="modal-body">
<strong>Collection name</strong>
<input type="text" class="form-control" aria-label="New collection name" id='arv-new-collection'>
</div>
<div class="modal-footer">
<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
<button id="arv-new-back" type="button" class="btn btn-default"><span class="glyphicon glyphicon-chevron-left"></span> Back</button>
<button id="arv-create-collection" type="button" class="btn btn-primary">Select</button>
</div>
</div>
</div>
</div>

<!-- Bootstrap modal dialog to select a collection from a project -->
<div id="arv-collection-form" class="modal fade" tabindex="-1" role="dialog">
<div class="modal-dialog modal-lg" role="document">
<div class="modal-content">
<div class="modal-header">
<button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
<h4 class="modal-title">Select Arvados Collection</h4>
</div>
<div class="modal-body">
<div id='arv-collection-error' class="alert alert-error" role="alert">
<strong>Error!</strong> Select a collection or define a new one before continuting
</div>
<p><strong>Project: </strong><span id="arv-project-name"></span></p>
<div class="input-group">
<span class="input-group-addon">
<label>
<input type="radio" name="arv-collection" value="_NEW_" onclick="selectCollection(this,'')">
<strong>New collection</strong></label>
</span>
<input type="text" class="form-control" aria-label="New collection name" id='arv-collection-name' >
</div>
<p><strong>Existing collection(s)</strong></p>
<table id="arv-collection-table" class="table table-striped table-bordered" cellspacing="0" width="100%">
<thead>
<tr><th>Collection</th><th>Created</th><th>Modified</th></tr>
</thead>
</table>
</div>
<div class="modal-footer">
<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>
<button id="arv-back" type="button" class="btn btn-default"><span class="glyphicon glyphicon-chevron-left"></span> Back</button>
<button id="arv-select-collection" type="button" class="btn btn-primary">Select</button>
</div>
</div>
</div>
</div>


<script src="<%= request.getContextPath() %>/resources/js/arv-modals.js"></script>
<script>
$('#arv-project-table').DataTable({
	serverSide: true,
	deferLoading: 0,
	processing: true,
	ajax: '<%= request.getContextPath() %>/arv-tables/projects',
	columns: [ { data: 'name', render: projectFormat },
		{data: 'created_at'}, { data: 'modified_at'} ],
	rowId: 'uuid',
	infoCallback: function (settings, start, end, max, total, pre) { return "Showing " + start + " to " + end + " of " + total + " entries"; },
	columnDefs: [ { targets: [ 1,2 ],  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ]
});

$('#arv-collection-table').DataTable({
	serverSide: true,
	processing: true,
	deferLoading: 0,
	ajax: { url: '<%= request.getContextPath() %>/arv-tables/collections', data: function (d) { if ( selproject.id != undefined ) { d.project = selproject.id; } } },
	columns: [ { data: 'name', render: collectionFormat }, {data: 'created_at'}, { data: 'modified_at'} ],
	rowId: 'uuid',
	infoCallback: function (settings, start, end, max, total, pre) { return "Showing " + start + " to " + end + " of " + total + " entries"; },
	columnDefs: [ { targets: [ 1,2 ],  render: $.fn.dataTable.render.moment( moment.ISO_8601, 'MMM Do, YYYY h:mm A' ) } ]
});
</script>