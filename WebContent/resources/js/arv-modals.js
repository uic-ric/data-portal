var selproject = { id: undefined, name: undefined };
var selcollection = { id: undefined, name: undefined };

function getSelArvProject() {
	return $("#arv-project-table").DataTable().row('.selected').id();
}

function getSelArvCollection() {
	return $("#arv-collection-table").DataTable().row('.selected').id();
}

function selectProject(elem) { 
	if (elem.checked) { 
		selproject.id = elem.value; 
		if ( selproject.id !== "_NEW_" ) { 
			table = $('#arv-project-table').DataTable();
			projectData = table.row('#' + selproject.id).data();	
			selproject.name = projectData.name;
		}
	} else { 
		selproject.id = undefined; 
		selproject.name = undefined; 
	}
}

function selectCollection(elem) { 
	if (elem.checked) { 
		selcollection.id = elem.value; 
		if ( selcollection.id !== '_NEW_' ) { 
			table = $('#arv-collection-table').DataTable();
			collData = table.row('#' + selcollection.id).data();	
			selcollection.name = collData.name;
		}
	} else { 
		selcollection.id = undefined; 
		selcollection.name = undefined; 
	}
}

function projectFormat(data,type,full,meta) { 
	var checked = (full.uuid === selproject.id );
	return'<label><input type="radio" name="arv-project" onclick="selectProject(this)" value="' + 
		full.uuid + ( checked ? '" checked' : '"') + '> ' + data + "</label>";
}

function collectionFormat(data,type,full,meta) { 
	var checked = (full.uuid === selcollection.id );
	return'<label><input type="radio" name="arv-collection" onclick="selectCollection(this)" value="' + 
		full.uuid + ( checked ? '" checked' : '"') + '> ' + data + "</label>";
}

function arvNext () { 
	selproject.id = $('input[name=arv-project]:checked').val();
	if ( selproject.id == undefined ) {
		$('#arv-project-error').show();
	} else {
		$('#arv-project-error').hide();
		$('#arv-project-form').modal('hide');
		if ( selproject.id === "_NEW_" ) { 
			selproject.name = $('#arv-project-name').val();
			selcollection.id = "_NEW_";
			$('#arv-new-collection-form').modal('show'); 
		} else {
			table = $('#arv-project-table').DataTable();
			projectData = table.row('#' + selproject.id).data();	
			selproject.name = projectData.name;

			$('#arv-collection-error').hide();
			$('#arv-collection-table').DataTable().ajax.reload(); 
			$('#arv-collection-form').modal('show'); 
		}
	}
}

$('#arv-new-back').click( function () { 
	$('#arv-new-collection-form').modal('hide'); 
	$('#arv-project-form').modal('show');
} );

$('#arv-back').click( function () { 
	$('#arv-collection-form').modal('hide'); 
	$('#arv-collection-error').hide();
	$('#arv-project-form').modal('show');
} );

$('#arv-select-collection').click( function () { 
	selcollection.id = $('input[name=arv-collection]:checked').val();
	if ( selcollection.id == undefined ) {
		$('#arv-collection-error').show();
	} else {
		if ( selcollection.id === '_NEW_' ) { 
			selcollection.name = $('#arv-collection-name').val();
		} else {
			table = $('#arv-collection-table').DataTable();
			collData = table.row('#' + selcollection.id).data();	
			selcollection.name = collData.name;
		}
		if ( arv_callback != undefined ) { 
			arv_callback(selproject, selcollection);
		}
		$('#arv-collection-form').modal('hide'); 
	}
} );

$('#arv-create-collection').click( function () { 
	selcollection.name = $('#arv-new-collection').val();
	selcollection.id = "_NEW_";
	if ( arv_callback != undefined ) { 
		arv_callback(selproject, selcollection);
	}
	$('#arv-new-collection-form').modal('hide'); 
} );

var arv_callback = undefined;

// Function to display the project selection modal 
function openArvForm(callback) { 
	$('#arv-project-error').hide();
	$('#arv-project-form').modal('show');
	$('#arv-project-table').DataTable().ajax.reload(); 
	arv_callback = callback;
}

// Function to show just the collection selection modal
function openArvCollectionForm(callback) { 
	$('#arv-collection-error').hide();
	$('#arv-collection-table').DataTable().ajax.reload(); 
	$('#arv-back').hide();
	$('#arv-collection-form').modal('show'); 
	arv_callback = callback;
}

function setNewProjectName(name) { 
	$('#arv-project-name').val(name);
}

function setNewCollectionName(name) { 
	$('#arv-collection-name').val(name);
	$('#arv-new-collection').val(name);
}

function setSelection(project, collection) {
	selproject = project;
	selproject = collection;
}