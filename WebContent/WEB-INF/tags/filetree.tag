<%@ tag language="java" pageEncoding="ISO-8859-1"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ attribute name="project" required="true"%>
<%@ tag import="edu.uic.rrc.portal.ProjectsServlet" %>
<script type="text/javascript">

function generateDownload(form) {
    var sellist = $("#filetree").jstree(true).get_selected();
	var zipcount = 0;    
    
    for ( var i in sellist ) {
            var id = sellist[i];
    		var item = files.find(function(x) { return x.id === id; } );
            if ( item.file ) {
            		zipcount++;
            		var input = document.createElement("input");
            		input.type = "hidden";
            		input.name = "zipList";
            		input.value = item.id;
					form.appendChild(input);
            }
    }
    if ( zipcount === 0 ) {
            alert("Please check at least one file to download");
            return false;
	} 
    $("#filetree").html("<h3 style='align:center'>Preparing download...</h3><p>Creating ZIP archive.  Download should begin shortly.</p>");
	$('#zip-header').hide();
	form.submit();
	form.style.display = "none";
}

var files = [  { id: "raw_data", text: "Raw data", state : { opened: false }, parent: "#", icon: false, a_attr: { class: 'section'} },
				{ id: "results", text: "Results", state : { opened: true }, parent: "#", icon: false, a_attr: { class: 'section'} },
				{ id: "reports", text: "Reports", state : { opened: true }, parent: "#", icon: false, a_attr: { class: 'section'}  } ];

function loadFiles(projectid, filetype) {

	var http = new XMLHttpRequest();
	http.open("GET", "<%= request.getContextPath() %>/projects/" + projectid + "?tree&type=" + filetype, true);
	http.setRequestHeader('Accept', 'application/json');
	http.onreadystatechange = function() {
		if( http.readyState == 4 && http.status==200 ){
			files = files.concat(JSON.parse(http.responseText));
			done = done - 1;
			if ( done === 0 ) {
				rebuildTree();
			}
		}
	};
	http.send();
}

function rebuildTree() { 
	$('#filetree').jstree({ 'core' : {
		'data' : files,
		'themes' : { 'variant' : 'large' } },
	"plugins" : [ "checkbox" ] });
}

</script>
<div id="zip-header" class="row">
<h3>Download ZIP archive</h3>
<p>Select the files and folders to include in the ZIP file then click "Create ZIP File"</p>
</div>
<div id="filetree"><h2>Building file tree...</h2></div>
<div class="row" style="margin-top:10px">
<form id="fileForm" method="post" action="<%= request.getContextPath() %>/zip/<%= project %>">
<button type="button" class="btn btn-primary" onclick="generateDownload(this.form)" >Create ZIP File</button>
<button type="button" class="btn btn-default" onclick="rebuildTree()">Clear selections</button>
</form>
</div>
<script>
var done = 3;
loadFiles("<%= project %>", "raw_data");
loadFiles("<%= project %>", "results");
loadFiles("<%= project %>", "reports");
</script>
