/**
 * 
 */

var dirmap = [];
var root = [];
var currFiles;
var parentDir = [];

function reloadTable() { 
	var file_table = $('#file-table').DataTable();
	
	file_table.clear();
	file_table.rows.add(currFiles);
	file_table.draw();	
}

function dirname(path) {
	var i = path.lastIndexOf("/");
	if ( i == -1 ) {
		return undefined;
	} else {
		return path.substring(0,i);
	}
}

function basename(path) {
	var i = path.lastIndexOf("/");
	if ( i == -1 ) {
		return path;
	} else {
		return path.substr(i + 1);
	}
}

function humanSize(size) {
	if (size == 0) { return "0.0 B"; }
	var e = Math.floor(Math.log(size) / Math.log(1024));
	var units = ['B', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB'];
	if ( e == 0 ) { return size + ' B'; }
	return (size / Math.pow(1024, e)).toFixed(1) + ' ' + units[e];
}

function buildFileTree(files) {
	root = [];
	
	for ( var f in files ) {
		var file = files[f];
		file.type = 'file';
		var filename = file.id.substr(file.id.indexOf("/") + 1);
		var parent = dirname(filename);
		if ( parent != undefined ) {
			var dir = dirmap[parent];
			if ( dir == undefined ) {
				// Create a new directory object
				dir = { 'id': parent, 'type':'dir', 'files': [], 'name': basename(parent), 
						'release' : { 'title' :'', 'date': undefined, 'user':undefined}, 'description': '', parent:undefined};
				dirmap[parent] = dir;
				// check if parent directories need to be added.
				var dirparent = dirname(parent);
				var leafdir = dir;
				if ( dirparent != undefined ) {
					CHECK_PARENT : while ( dirparent != undefined) { 
						var parentobj = dirmap[dirparent];
						leafdir.parent = dirparent;
						if ( parentobj == undefined ) {
							parentobj = { 'id': dirparent, 'type':'dir', 'files':[ leafdir ], 'name': basename(dirparent), 
									'release' : { 'title' :'', 'date': undefined, 'user':undefined}, 'description': '', parent:undefined };
							dirmap[dirparent] = parentobj;
							dirparent = dirname(dirparent);
							if ( dirparent == undefined ) {
								root.push(parentobj);
								break CHECK_PARENT;
							}
							leafdir = parentobj;
						} else {
							parentobj.files.push(leafdir);
							break CHECK_PARENT;
						}
					}
				} else {
					root.push(dir);
				}
			} 
			dir.files.push(file);
		} else {
			root.push(file);
		}
	}
	// Set the current directory to the root listing
	currFiles = root;
	return root;
}

function buildCollectionFileTree(files) {
	root = [];
	
	for ( var f in files ) {
		var file = files[f];
		file.type = 'file';
		var parent = dirname(file.name);
		file.name = basename(file.name);
		file.selected = false;
		file.hide = false;
		if ( parent != undefined ) {
			var dir = dirmap[parent];
			if ( dir == undefined ) {
				// Create a new directory object
				dir = { 'id': parent, 'type':'dir', 'files': [], 'name': basename(parent), 'hidden': false,
						'released' : false, 'description': '', parent:undefined, 'selected': -1, 'hide': -1, 'filetype': ''};
				dirmap[parent] = dir;
				// check if parent directories need to be added.
				var dirparent = dirname(parent);
				var leafdir = dir;
				if ( dirparent != undefined ) {
					CHECK_PARENT : while ( dirparent != undefined ) { 
						var parentobj = dirmap[dirparent];
						leafdir.parent = dirparent;
						if ( parentobj == undefined ) {
							parentobj = { 'id': dirparent, 'type':'dir', 'files':[ leafdir ], 'name': basename(dirparent),  'hidden': false,
									'released' : false, 'description': '', parent:undefined, 'selected': -1, 'hide': -1, 'filetype': '' };
							dirmap[dirparent] = parentobj;
							dirparent = dirname(dirparent);
							if ( dirparent == undefined ) {
								root.push(parentobj);
								break CHECK_PARENT;
							}
							leafdir = parentobj;
						} else {
							parentobj.files.push(leafdir);
							break CHECK_PARENT;
						}
					}
				} else {
					root.push(dir);
				}
			} 
			dir.files.push(file);
		} else {
			root.push(file);
		}
	}
	// Set the current directory to the root listing
	currFiles = root;
	return root;
}

function sortTable(column, sorttype) {
	var table = null;
	var parent = column.parentNode;
	while ( table == null && parent != null ) {
		if ( parent.nodeName === "TABLE" ) {
			table = parent;
		} else {
			parent = parent.parentNode;
		}
	}
	
	if ( table != null ) {
		var colindex = column.cellIndex;
		var headers = column.parentNode.cells;
		for ( var i = 0; i < headers.length; i++ ) {
			if ( i != colindex ) {
				var classname = headers[i].className;
				if ( classname === "sortdesc" || classname === "sortasc" )
					headers[i].className = "sortable";
			}
		}
		var body = table.tBodies[0];
		var rows = Array.prototype.slice.call(body.rows);
		var direction = ( column.className === "sortasc" ? -1 : 1);
		column.className = ( direction === -1 ? "sortdesc" : "sortasc" );
		if ( sorttype === "number" ) {
			rows.sort(function (a,b) { 
				var avalue = getCellValue(a.cells[colindex]);
				var bvalue = getCellValue(b.cells[colindex]);
				var anumber = parseFloat(avalue);
				var bnumber = parseFloat(bvalue);
				if ( isNaN(anumber) ) anumber = 0;
				if ( isNaN(bnumber) ) bnumber = 0;
				var diff = anumber - bnumber;
				return direction * ( diff === 0 ? avalue.localeCompare(bvalue) : diff ); });
		} else if ( sorttype === "date" ) {
			rows.sort(function (a,b) { 
				var avalue = new Date(getCellValue(a.cells[colindex]));
				var bvalue = new Date(getCellValue(b.cells[colindex]));
				return 	direction * ( avalue.getTime() - bvalue.getTime()) ; });					
		} else {
			rows.sort(function (a,b) { 
				var avalue = getCellValue(a.cells[colindex]);
				var bvalue = getCellValue(b.cells[colindex]);
				return direction * ( avalue.localeCompare(bvalue)) ; });					
		}
		body.innerHTML = "";
		for ( var i = 0; i < rows.length; i++ ) {
			body.appendChild(rows[i]);
		}
	}
}

function getCellValue(cell) {
	var child = cell.childNodes[0];
	if ( child === undefined ) {
		return null;
	}
	if ( child.nodeName === "A" ) {
		return child.innerHTML;
	} else {
		return cell.innerHTML;
	}
}
