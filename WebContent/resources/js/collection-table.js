var dataTypeMap = {
	'raw' : 'Raw data',
	'result' : 'Results',
	'report' : 'Reports'		
};

var typeOrder = [ 'raw', 'result', 'report'];

function checkDirs() {
	var keys = Object.keys(dirmap);
	keys.sort(function(a,b) { b.length - a.length});
	for ( var i = 0; i < keys.length ; i++ ) {
		var thisdir = dirmap[keys[i]];
		thisdir.released = thisdir.files.reduce(function(a,b) { return a || b.released; }, false);
	}	
}

function validateRevoke(form) {
    if (!$('input[name="revoke"]').is(':checked')) {
        alert("Please check atleast one file to revoke");
        return false;
    }
	var formdata = [];
	for ( var f in allFiles ) {
		if ( allFiles[f].type !== 'dir' && allFiles[f].revoke ) {
			var elem = document.createElement('INPUT');
			elem.type='hidden';
			elem.name='revokeList';
			elem.value = allFiles[f].id;
			form.appendChild(elem);    
		}
	}
}

// Validate the release and send the data via AJAX and reload the table.
function validateRelease(form) {
    if (!$('input[name="fileList"]').is(':checked')) {
        alert("Please check at least one file to release");
        return false;
    }
	var formdata = [];
	var datatype = form.elements['data_type'].value;
	for ( var f in allFiles ) {
		if ( allFiles[f].selected ) {
			if ( allFiles[f].filetype == undefined || allFiles[f].filetype === '' ) 
				allFiles[f].filetype = datatype;
			formdata.push(allFiles[f]);
		}
	}
	var elem = document.createElement('INPUT');
	elem.type='hidden';
	elem.name='files';
	elem.value = JSON.stringify(formdata);
	form.appendChild(elem);
}
	
function releaseFiles(form) {
    $.ajax({
		url: form.action,
		accepts: { json: "application/json" },
		type: "POST",
		dataType:"json",
		data: JSON.stringify( {release: formdata} ), 
		success: function (data, status, http) {
			if ( http.status == 200 ) {		
				var datatables = $('#file-table').DataTable();
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

function revokeFiles(form) {
	var formdata = [];
	for ( var f in allFiles ) {
		if ( allFiles[f].revoke ) {
			formdata.push(allFiles[f]);
		}
	}
    $.ajax({
		url: form.action,
		accepts: { json: "application/json" },
		type: "POST",
		dataType:"json",
		data: JSON.stringify( {revoke: formdata}), 
		success: function (data, status, http) {
			if ( http.status == 200 ) {		
				var datatables = $('#file-table').DataTable();
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


function ranges(a,b) {
	if ( b.released ) { return a; }
	var thisval = (b.type === "dir" ? b.selected : (b.selected ? 1 : -1));
	a.selected[0] = Math.min(a.selected[0], thisval);
	a.selected[1] = Math.max(a.selected[1], thisval);
	thisval = (b.type === "dir" ? b.hide : (b.hide ? 1 : -1));
	a.hide[0] = Math.min(a.hide[0], thisval);
	a.hide[1] = Math.max(a.hide[1], thisval);
	return a;
}

function back() {
	var thisdir = parentDir.pop();
	var rangeDetail = thisdir.files.reduce(ranges, { selected: [1, -1], hide: [1, -1] });
	if ( rangeDetail.selected[0] != rangeDetail.selected[1] ) {
		thisdir.selected = 0;
	} else {
		thisdir.selected = rangeDetail.selected[0];
	}
	
	if ( rangeDetail.hide[0] != rangeDetail.hide[1] ) {
		thisdir.hide = 0;
	} else {
		thisdir.hide = rangeDetail.hide[0];
	}
	
	if ( parentDir.length > 0 ) {
		currFiles = parentDir[parentDir.length - 1].files;
	} else {
		currFiles = root;
		$('#back-button').hide();
	}
	reloadTable();
}
function reloadTable() { 
	var file_table = $('#file-table').DataTable();
	file_table.clear();
	file_table.rows.add(currFiles);
	file_table.draw();			
	$('input.indeterminate').each(function() { $(this).prop("indeterminate", true); });
}

function changeDir(id) {
	var thisdir = dirmap[id];
	parentDir.push(thisdir);
	currFiles = thisdir.files;
	$('#back-button').show();
	reloadTable();
}

function selectDir(thisdir, select) { 
	thisdir.selected = ( select ? 1 : -1);
	if ( ! select ) { thisdir.hide = false; }
	for ( var f in thisdir.files ) {
		var file = thisdir.files[f];
		if ( ! file.released ) { 
			if ( file.type === 'dir' ) {
				selectDir(file, select);
			} else {
				file.selected = select;
				if ( ! select ) { file.hide = false; }				
			}
		}
	}
	if ( ! select ) { reloadTable(); }
}

function hideDir(thisdir, hide) {
	thisdir.hide = hide;
	if ( hide ) { thisdir.selected = 1; }
	for ( var f in thisdir.files ) {
		var file = thisdir.files[f];
		if ( ! file.released ) { 
			if ( file.type === 'dir' ) {
				hideDir(file, hide);
				if ( hide && file.selected == -1 ) { 
					selectDir(file, true); 
				} 
			} else {
				file.hide = hide;
				if ( hide ) { file.selected = true; }
			}
		}
	}
	if ( hide ) { reloadTable(); }
}
 
function revokeDir(thisdir, revoke) {
	thisdir.revoke = revoke;
	for ( var f in thisdir.files ) {
		var file = thisdir.files[f];
		if ( file.released ) { 
			if ( file.type === 'dir' ) {
				revokeDir(file, revoke);
			} else if (file.released ) {
				file.revoke = revoke;
			}
		}
	}
}
 
function updateFile(id, select) { 
	var data = $('#file-table').DataTable().row('#' + id).data();
	data.selected = select;
	if ( ! select ) { data.hide = false; }
	reloadTable(); 
}

function hideFile(id, hide) { 
	var data = $('#file-table').DataTable().row('#' + id).data();
	data.hide = hide;
	if ( hide ) { data.selected = true; }
	reloadTable(); 
}

function selectAllFiles(select) {
	$('input[name="fileList"]').each(function() { $(this).prop('checked', select);});
	for ( var f in root ) {
		var file = root[f];
		if ( file.type === 'dir' ) {
			selectDir(file, select);
		} else {
			file.selected = select;
			if ( ! select ) { file.hide = false; }
		}	
	}
	reloadTable();
}

function hideAllFiles(select) {
	$('input[name="hide"]').each(function() { $(this).prop('checked', select);});
	for ( var f in root ) {
		var file = root[f];
		if ( file.type === 'dir' ) {
			hideDir(file, select);
		} else {
			file.hide = select;
			if ( select ) { file.selected = true; }
		}	
	}
	reloadTable();
}


function revokeAllFiles(select) {
	$('input[name="revokeList"]').each(function() { $(this).prop('checked', select);});
	for ( var f in root ) {
		var file = root[f];
		if ( file.type === 'dir' ) {
			revokeDir(file, select);
		} else if ( file.released ){
			file.revoke = select;
		}	
	}
	reloadTable();
}

function releaseIcon(file) {
	return '<span class="glyphicon ' + (file.hidden ? 'icon-hidden glyphicon-eye-close' : 'released glyphicon-ok') + '"></span> ';
}

function displayRevoke(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return full.released; }
	if ( full.released ) {
		return '<input type="checkbox" name="revoke" value="' + full.id + '" onclick="revokeFile(\'' + full.id + '\',this.checked)"' + (full.revoke ? " checked" : '') + '>';                		                		
	}
	return '';                		                		
}

function revokeFile(id, status) {
	var data = $('#file-table').DataTable().row('#' + id).data();
	if ( data.type === "dir" ) {
		revokeDir(data, status);
	} else {
		data.revoke = status;		
	}
	reloadTable(); 
}

function displaySelection(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( full.type === "dir" ) {
		return ( full.released ? '<span class="glyphicon glyphicon-ok"></span> ' : 
				'<input id="sel_' + full.id + '" type="checkbox" name="fileList" value="' + full.id + 
				'" onclick="selectDir(dirmap[\'' + full.id + '\'],this.checked)"' + 
				(full.selected == 1 ? " checked" : '') + (full.selected == 0 ? ' class="indeterminate"' : '') + '>') + 
				'<a onclick="changeDir(\'' + full.id + '\')">' + data + '</a>';                		                		
	}  else {	
		if(full.released){
			return ( full.released ? releaseIcon(full) : '<input id="sel_' + full.id + '" type="checkbox" name="fileList" value="' + 
					full.id + '" onclick="updateFile(\'' + full.id + '\',this.checked)"' + (full.selected ? " checked" : '') + '>') + 
					'<a id="'+data+'" href="' + contextPath + '/file/' + full.id + '">' + data +'</a>';                	
		}
		else{
		return ( full.released ? releaseIcon(full) : '<input id="sel_' + full.id + '" type="checkbox" name="fileList" value="' + 
				full.id + '" onclick="updateFile(\'' + full.id + '\',this.checked)"' + (full.selected ? " checked" : '') + '>') + 
				'<a id="'+data+'" href="' + contextPath + '/file/' + full.id + '">' + data +'</a><button id="rename-btn-'+data+'" type="button" onclick="setRenameButtonId(this,\'' + full.id + '\')" class="btn btn-link" data-toggle="modal" data-target="#renameModal"><span class="glyphicon glyphicon-pencil" style="color:black"></span></button>';                		
		}}
}

function setRenameButtonId(obj,tt){		
	var id;
	if(obj.id == "rename-btn-"+tt){
		id=obj.id;
	}
	else{
		id="rename-btn-"+tt;
	}
    var res = id.substring("rename-btn-".length,id.length);
    if($('#myfieldname').length)
    	{
    	var newres = res.split('/');
        var res1 = newres[newres.length-1];
    		var newFieldName = $('a[id="'+res1+'"]').text();
    		$('#myfieldname').val(res);
    	}
    else{
    	$('#renameModal').find('#renameForm').append('<input id="myfieldname" type="hidden" name="myfieldname" value="'+res+'" />');
    } 
    var oldFileNamearr = res.split('/');
    var oldFileTextField = oldFileNamearr[oldFileNamearr.length-1];
    $('#renameModal').find('#newFilename').prop('value',oldFileTextField);
       reloadTable();
    
}
  

function setFileDescription(id, text) {
	var data = $('#file-table').DataTable().row('#' + id).data();
	data.description = text;
	reloadTable(); 
}

function displayDescription(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( full.released ) { return full.hidden ? '' : data; }
	if ( full.type === "dir" ) {
		return '';                		                		
	} else {
		return '<input type="text" class="form-control" style="width: 100%" onchange="setFileDescription(\'' + full.id + '\', this.value)" value="' + (data != undefined ? data : '') + '"' + (full.hide ? ' disabled' : '') + '>';                		
	}
}

function setFileType(id, value) {
	var data = $('#file-table').DataTable().row('#' + id).data();
	data.filetype = value;
	reloadTable();
}

function changeDirType(id, value) {
	var data = $('#file-table').DataTable().row('#' + id).data();
	setDirType(data, value);
	reloadTable();
}	
	
function setDirType(thisdir, value) {
	thisdir.filetype = value;
	for ( var f in thisdir.files ) {
		var file = thisdir.files[f];
		if ( ! file.released ) { 
			if ( file.type === 'dir' ) {
				setDirType(file, value);
			} else {
				file.filetype = value;
			}
		}
	}
}

function displayDataType(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return data; }
	if ( full.released ) { return data; }
	if ( full.type === "dir" ) {
		return '<select onchange="changeDirType(\'' + full.id + '\', this.value)">' + genTypeOptions(data) + '</select>';                		
	} else {
		return '<select onchange="setFileType(\'' + full.id + '\', this.value)">' + genTypeOptions(data) + '</select>';                		
	}
}

function genTypeOptions(type) {
	var options = '<option value="">- Default -</option>';
	for ( var t in typeOrder ) {
		options += '<option value="' + typeOrder[t] + '"' + ( typeOrder[t] === type ? ' selected' : '') + '>' + dataTypeMap[typeOrder[t]] + '</option>';		
	}
	return options;
}


function displayHidden(data, type, full, meta) {
	if ( type === "sort" || type === "filter" || type === "type" ) { return full.released ? full.hidden : data; }
	if ( full.type === "dir" ) {
		if ( full.released ) { return ''; }
		return '<input type="checkbox" name="hide" value="' + full.id + '" onclick="hideDir(dirmap[\'' + full.id + '\'], this.checked)"' + (full.hide == 1 ? " checked" : '') + (full.hide == 0 ? ' class="indeterminate"' : '') + '>'
	} else {
//    	if ( full.released ) { return '<span class="glyphicon ' + (full.hidden ? 'glyphicon-eye-close hidden-eye' : 'glyphicon-eye-open visible-icon') + '"></span>'; }
    	if ( full.released ) { return '<span class="glyphicon ' + (full.hidden ? 'glyphicon-eye-close hidden-eye' : 'visible-icon') + '"></span>'; }
		return '<input type="checkbox" name="hide" value="' + full.id + '" onclick="hideFile(\'' + full.id + '\',this.checked)"' + (data ? " checked" : '') + '>';                		
	}
}