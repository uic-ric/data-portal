<%@ tag language="java" pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="portal" tagdir="/WEB-INF/tags" %>
<%@ attribute name="files" required="true" type="java.util.List" %>
<%@ attribute name="project" required="true" %>
<%@ tag import="java.util.List,java.io.File,java.text.DateFormat,java.text.SimpleDateFormat, java.util.Date, edu.uic.rrc.portal.FileServlet" %>
<div id="file-list">
<portal:uploadfiletable project="<%= project %>"/>
<button id="file-upld-btn" class="btn btn-link"><span class="glyphicon glyphicon-cloud-upload"></span>Upload new files</button>
<script>
$("#file-upld-btn").click(function(){
	$("#file-upload").show();
	$("#file-list").hide();
});

var filequeue = [];
var renamedFname="";
var isRenamed=false;
var currstatus=document.createElement('div');
var fileindex = 0;
var currFile;

function checkAndTransferFile(file, name=undefined){
	var status;
	
	if ( file.size > <%= String.format("%d", FileServlet.maxFileSize) %>) {
		currstatus = document.getElementById("status_" + file.name);
		currstatus.innerHTML = "<span class='glyphicon glyphicon-exclamation-sign'></span><strong>File is too large</strong> ";
		currstatus.classList.remove('badge');
		currstatus.parentElement.classList.add("list-group-item-danger");
		return;
	}
	
	var url = "<%= request.getContextPath() %>/file-upload/<%= project %>/" + ( name !== undefined ? name : file.name );
	console.log(url);
	var http = new XMLHttpRequest();
	http.open("HEAD", url , true);
	http.onreadystatechange = function() {
		if ( http.readyState == 4 && http.status==409 ) {
			currstatus = document.getElementById("status_" + file.name);
			currstatus.innerHTML = "<span class='glyphicon glyphicon-exclamation-sign'></span> <strong>File exists</strong> ";
			currstatus.classList.remove('badge');
//			currstatus.classList.add('alert-warning');
			currstatus.parentElement.classList.add("list-group-item-warning");
			
			// Create button group for possible actions.
			var div = document.createElement('div');
			div.classList.add('btn-group');
			div.setAttribute('data-toggle', 'buttons');
			
			// Create button to overwrite file
			var button = document.createElement('button');
			button.classList.add('btn', 'btn-warning', 'btn-sm');
			button.onclick = function (e) {
				overwrite(file);
			}
			button.innerHTML = "Overwrite";
			div.appendChild(button);
			
			// Create button to rename file
			button = document.createElement('button');
			button.classList.add('btn', 'btn-warning', 'btn-sm');
			button.onclick = function(e) {
				renameFile(file);
			}
			button.innerHTML = "Rename";
			div.appendChild(button);

			// Create button to skip file
			button = document.createElement('button');
			button.classList.add('btn', 'btn-warning', 'btn-sm');
			button.onclick = function(e) {
				skipFile(file);
			}
			button.innerHTML = "Skip";
			div.appendChild(button);
			
			currstatus.appendChild(div);

			// currstatus.onclick = function(e) {
			//	$('#file-modal').modal('show');
			//	$('#file-name').html(file.name);
			//	currFile = file;
			//}
		} else if (http.readyState == 4 && http.status==200){
			transferFileViaPut(file, false, name);
		}
	};
	http.send();
	return status;
}

function overwriteFile(file) {
	  currstatus = document.getElementById("status_" + file.name);
	  currstatus.classList.remove('btn', 'btn-warning');
	  currstatus.parentElement.classList.remove("list-group-item-warning");
	  currstatus.innerHTML = "Ready";
	  transferFileViaPut(file, true);
}

function renameFile(file) {
	currFile = file;
	$('#inputFname').val(file.name);
	$('#renameModal').modal('show');
}

function skipFile(file) {
  currstatus = document.getElementById("status_" + file.name);
  currstatus.innerHTML = "Skipped";
  currstatus.classList.remove('btn','btn-warning');
  currstatus.parentElement.classList.remove("list-group-item-warning");
    
  currstatus.classList.add("alert-info", "badge");
  currstatus.parentElement.classList.add("list-group-item-info");
  currstatus.onclick = undefined;
}

function transferFileViaPut(file, overwrite = false, name=undefined) {
	
	//var file = filequeue.shift();
	currstatus = document.getElementById("status_" + file.name);
	
	if ( file.size > <%= String.format("%d", FileServlet.maxFileSize) %>) {
		currstatus.innerHTML = "<span class='glyphicon glyphicon-exclamation-sign'></span> <strong>File is too large</strong> ";
		currstatus.classList.remove('badge');
		currstatus.parentElement.classList.add("list-group-item-danger");
		return;
	}
	
	var http = new XMLHttpRequest();
	var URL = "<%= request.getContextPath() %>/file-upload/<%= project %>/" + ( name !== undefined ? name : file.name );
	if ( overwrite ) {
		URL = URL + "?overwrite";
	}
	http.open("PUT", URL , true);
	http.onreadystatechange = function() {
		if ( http.readyState == 4 ) {
			if ( http.status == 409 ) {
				currstatus.innerHTML = "<strong>File exists</strong> ";
				currstatus.classList.add("alert-warning");
				currstatus.parentElement.classList.add("list-group-item-warning");
			} else if ( http.status == 201) {
				currstatus.innerHTML = "Done";
				currstatus.classList.add("alert-success");
				currstatus.parentElement.classList.add("list-group-item-success");
			}else if(http.status == 406) {
				currstatus.innerHTML = "Error: File already present in the staging area"; 						
				currstatus.classList.add("alert-danger");
				currstatus.parentElement.classList.add("list-group-item-error");
			} 
			else {
				currstatus.innerHTML = "Error"; 						
				currstatus.classList.add("alert-danger");
				currstatus.parentElement.classList.add("list-group-item-error");
			}
		}
	};
	http.upload.addEventListener("progress", function(e) {
		if (e.lengthComputable) {
			var percentage = Math.round((e.loaded * 100) / e.total);
			currstatus.innerHTML = percentage + "%";
			var progressbar = currstatus.parentElement.getElementsByClassName("progress");
			progressbar.style = "width: " + percentage + "%;";
		}
	}, false);
	http.send(file);
}

function closeForm() { 
	$('#file-upload').hide(); 
	$('#file-table').show();
	$('#list-group').html('');
}

</script>
</div>
<link rel="stylesheet" type="text/css" href="<%= request.getContextPath() %>/resources/css/fileupload.css">
<div id="file-upload" class="container" style="display:none; margin-top:10px;">
      <div class="panel panel-default">
        <div class="panel-heading"><strong>Upload Files</strong><span style="align:right"><button type="button" class="close" aria-label="Close" onclick="closeForm();">&times;</button></span></div>
        <div class="panel-body">
		  <div class="alert alert-warning" role="alert"><strong>Maximum allowed file size is <script>document.write(humanSize(<%= String.format("%d", FileServlet.maxFileSize) %>))</script>.</strong> If you need to send a file larger than that size, please contact RRC to make arrangements for transfer the file(s).</div>

          <!-- Standard Form -->
          <h4>Select files from your computer</h4>
          <form id="js-upload-form">
            <div class="form-inline">
              <div class="form-group">
                <input type="file" id="js-upload-files" onchange="document.getElementById('js-upload-submit').disabled = false" name="files[]" multiple />
              </div>
              <button type="submit" class="btn btn-sm btn-primary" id="js-upload-submit" disabled>Upload files</button>
            </div>
          </form>

          <!-- Drop Zone -->
          <h4>Or drag and drop files below</h4>
          <div class="upload-drop-zone" id="drop-zone">
            Just drag and drop files here
          </div>

          <!-- Progress Bar -->
 <!--          <div class="progress">
            <div id="progress-bar" class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;">
              <span id="dispprog"></span>
            </div>
          </div>
-->
          <!-- Upload Finished -->
          <div class="js-upload-finished">
            <h3>Processed files</h3>
            <div class="list-group" id="list-group"></div>
          </div>
        </div>
      </div>
      <!-- modal -->
      <div class="modal fade" id="file-modal" role="dialog">
	   <div class="modal-dialog modal-lg">
	      <div class="modal-content">
	        <div class="modal-header">
	          <button type="button" class="close" data-dismiss="modal">&times;</button>
	          <h4 class="modal-title">Duplicate File</h4>
	        </div>
	        <div class="modal-body">
	          <p><span id="file-name"></span> already exists.</p>
	          <p>Do you want to rename the uploaded file or overwrite the existing one?</p>
	          <div class="checkbox">
                        <label>
                            <input type="checkbox"/> Do the same for all
                        </label>
                      </div>
	        </div>
	        <div class="modal-footer">
	        	<button id="rename-btn" type="button" class="btn btn-default">Rename</button>
	          	<button id="overwrite-btn" type="button" class="btn btn-default">Overwrite</button>
	          	<button id="skip-btn" type="button" class="btn btn-primary" data-dismiss="modal">Skip</button>
	        </div>
	      </div>
	    </div>
	  </div><!-- /modal -->
	        <!-- modal -->
      <div class="modal fade" id="renameModal" role="dialog">
	   <div class="modal-dialog modal-lg">
	      <div class="modal-content">
	        <div class="modal-header">
	          <button type="button" class="close" data-dismiss="modal">&times;</button>
	          <h4 class="modal-title">Rename File</h4>
	        </div>
	        <div class="modal-body">
	        <form class="form-horizontal" role="form">
	        	<div class="form-group">
                    <label  class="col-sm-2 control-label"
                              for="inputFname3">File Name</label>
                    <div class="col-sm-10">
                        <input type="text" class="form-control" 
                        id="inputFname" placeholder="Enter your new file name here"/>
                    </div>
	         
	        	</div>
	        	<div class="form-group">
                    <div class="col-sm-offset-2 col-sm-10">
                      <button id="rename-upload-btn" class="btn btn-default" type="button">Upload</button>
                    </div>
                </div>
             </form>
	      </div>
	    </div>
	  </div><!-- /modal -->
	  <script> 
	  $("#rename-btn").click(function(){
		  $('#file-modal').modal('hide');
		$('#renameModal').modal('show');
		});
	  $("#rename-upload-btn").click(function(){
			renamedFname = $("#inputFname").val();
		    	console.log('renamed file name is '+renamedFname);
				  currstatus = document.getElementById("status_" + currFile.name);
	    		currFile.name = renamedFname;
//	    		currstatus.id = "status_" + currFile.name;
	  		  currstatus.classList.remove('btn', 'btn-warning');
			  currstatus.parentElement.classList.remove("list-group-item-warning");
			  currstatus.innerHTML = "Ready";
		    	$('#renameModal').modal('hide');
		    	checkAndTransferFile(currFile, renamedFname);
		    });
	  $("#overwrite-btn").click(function(){		  
		  currstatus = document.getElementById("status_" + currFile.name);
		  currstatus.classList.remove('btn', 'btn-warning');
		  currstatus.parentElement.classList.remove("list-group-item-warning");
		  currstatus.innerHTML = "Ready";
		  transferFileViaPut(currFile, true);
		  $('#file-modal').modal('hide');
      });
	  
	  $("#skip-btn").click(function() {
		  currstatus = document.getElementById("status_" + currFile.name);
		  currstatus.innerHTML = "Skipped";
		  currstatus.classList.remove('btn','btn-warning');
		  currstatus.parentElement.classList.remove("list-group-item-warning");
		  
		  
		  currstatus.classList.add("alert-info", "badge");
		  currstatus.parentElement.classList.add("list-group-item-info");
		  currstatus.onclick = undefined;
		  $('#file-modal').modal('hide');
	  });
		    
		  $('#js-upload-submit').click(function(e) {
		        var files = document.getElementById('js-upload-files').files;
		        e.preventDefault()
		        var myDiv = document.getElementById('list-group');
		        for ( var i = 0; i < files.length; i++ ) {
		    		fileindex++;
		    		var file = files[i];		    		
		    		//filequeue.push(file);
		    		var aTag = document.createElement('li');
		    		aTag.setAttribute('class',"list-group-item");
		    		aTag.style="display: inline-block; width:100%;";
		    		aTag.innerHTML = '<div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div>' + file.name;
		    		var statusSpan = document.createElement("span");
		    		statusSpan.id = "status_" + file.name;		    		
		    		statusSpan.className="badge pull-right";
		    		statusSpan.innerHTML = "Ready";
		    		aTag.appendChild(statusSpan);
		    		statusSpan.thisfile = file;
		    		myDiv.appendChild(aTag);
		    		checkAndTransferFile(file);
		        }		        
		        $('input[type=file]').val('');
		        //document.getElementById("js-upload-form").submit();
		        //checkAndTransftransferFileViaPut();
		       //startUpload(e.dataTransfer.files)
		  });
		    
		    var dropZone = $("#drop-zone");
		    dropZone.on('drop', function(e) {
		        e.preventDefault();
		        this.className = 'upload-drop-zone';
		        var files = e.originalEvent.dataTransfer.files;
		        var myDiv = document.getElementById('list-group');
		        for ( var i = 0; i < files.length; i++ ) {
		    		fileindex++;
		    		var file = files[i];		    		
		    		var aTag = document.createElement('li');
		    		aTag.setAttribute('class',"list-group-item");
		    		aTag.style="display: inline-block; width:100%;";
		    		aTag.innerHTML = '<div class="progress-bar" role="progressbar" aria-valuenow="0" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div>' + file.name;
		    		var statusSpan = document.createElement("span");
		    		statusSpan.id = "status_" + file.name;
		    		statusSpan.className="badge pull-right";
		    		statusSpan.innerHTML = "Ready";
		    		aTag.appendChild(statusSpan);
		    		statusSpan.thisfile = file;
		    		myDiv.appendChild(aTag);
		    		checkAndTransferFile(file); //edit by amogh 
		        }
		        $('input[type=file]').val('');
		    });
		
		    dropZone.on('dragover', function() {
		        this.className = 'upload-drop-zone drop';
		        return false;
		    });

		    dropZone.on('dragleave', function() {
		        this.className = 'upload-drop-zone';
		        return false;
		    });
	  </script>
	  </div>
    </div>


