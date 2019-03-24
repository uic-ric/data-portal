<%@ page import="edu.uic.rrc.portal.ProjectsServlet,edu.uic.rrc.portal.model.entities.arvados.Group" %><!DOCTYPE html>
<html>
<head>
	<title>File Upload</title>
	<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/fileupload.css">
	<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/base.css">
	<link rel="stylesheet" type="text/css" href="<%=request.getContextPath()%>/resources/css/bootstrap.min.css">
	<script type="text/javascript" charset="utf8" src="<%=request.getContextPath()%>/resources/js/jquery-3.2.1.min.js"></script>
	<script src="https:<%=request.getContextPath()%>/resources/js/bootstrap.min.js"></script>
	<script type="text/javascript">
	+ function($) {
	    var dropZone = document.getElementById('drop-zone');
	    var uploadForm = document.getElementById('js-upload-form');
		var filequeue = [];
		var renamedFname="";
		var isRenamed=false;
		var currstatus=document.createElement('div');
		var fileindex = 0;

	    var startUpload = function(files) {
	        console.log(files)
	    }
	    
	    function checkAndTransferFile(file){
	    	var status;
	    	var url = "<%=request.getContextPath()%>/file-upload/<%=request.getParameter("projectid")%>/"+ file.name;
	    	console.log(url);
	    	var http = new XMLHttpRequest();
	    	http.open("HEAD", url , true);
			http.onreadystatechange = function() {
				if ( http.readyState == 4 && http.status==409 ) {
					renameOrOverrideFile(file);
				}
				else if(http.readyState == 4 && http.status==200){
					transferFileViaPut(file);
				}
			};
			http.send();
			return status;
	    }
	    function renameOrOverrideFile(file){
	    	$('#myModal').modal('show');
	    	if(isRenamed){
	    		file.name=renamedFname;
	    	}
	    	console.log(file);
	    	transferFileViaPut(file);
	    }
	    
	    function transferFileViaPut(file){
	    	
	    	//var file = filequeue.shift();
			currstatus = document.getElementById( "status_" + file.name);
			var http = new XMLHttpRequest();
			<% Group project = ProjectsServlet.getCurrentProject(request);%>
			var URL = "<%= request.getContextPath() %>/file-upload/<%=request.getParameter("projectid")%>/"+ file.name;
			console.log(URL);
				http.open("PUT", URL , true);
				http.onreadystatechange = function() {
					if ( http.readyState == 4 ) {
						if ( http.status == 409 ) {
							currstatus.innerHTML = "File exists";
						} else if ( http.status == 201) {
							currstatus.innerHTML = "Done"; 
						} else {
							currstatus.innerHTML = "Error"; 						
						}
					}
				};
				http.upload.addEventListener("progress", function(e) {
					if (e.lengthComputable) {
						var percentage = Math.round((e.loaded * 100) / e.total);
						currstatus.innerHTML = percentage + "%";
						 document.getElementById('dispprog').innerHTML = percentage + "% Complete";
						 document.getElementById('progress-bar').style = "width: "+percentage +"%;";
						$("#dispprog").innerHTML = percentage + "% Complete";
					}
				}, false);
				http.upload.addEventListener("load", function(e){ 
					currstatus.innerHTML = "Success";
				}, false);
				http.send(file);
	    }

	    uploadForm.addEventListener('submit', function(e) {
	        var files = document.getElementById('js-upload-files').files;
	        e.preventDefault()
	        var myDiv = document.getElementById('list-group');
	        for ( var i = 0; i < files.length; i++ ) {
				fileindex++;
				var file = files[i];
				checkAndTransferFile(file);
				//filequeue.push(file);
				var aTag = document.createElement('a');
				aTag.setAttribute('href',"#");
				aTag.setAttribute('class',"list-group-item list-group-item-success");
				aTag.innerHTML = '<div class="progress-bar" role="progressbar" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style="width: 0%;"></div><span id="status_'+file.name+'" class="badge alert-success pull-right"></span>' + file.name;
				
				myDiv.appendChild(aTag);
	        }
	        //transferFileViaPut();
	       // startUpload(e.dataTransfer.files)
	    })

	    dropZone.ondrop = function(e) {
	        e.preventDefault();
	        this.className = 'upload-drop-zone';
	        var files = e.dataTransfer.files;
	        var myDiv = document.getElementById('list-group');
	        for ( var i = 0; i < files.length; i++ ) {
				fileindex++;
				var file = files[i];
				checkAndTransferFile(file);
				var aTag = document.createElement('a');
				aTag.setAttribute('href',"#");
				aTag.setAttribute('class',"list-group-item list-group-item-success");
				aTag.innerHTML = '<span id="status_'+file.name+'" class="badge alert-success pull-right"></span>' + file.name;

				myDiv.appendChild(aTag);
	        }
	        startUpload(e.dataTransfer.files);
	    }

	    dropZone.ondragover = function() {
	        this.className = 'upload-drop-zone drop';
	        return false;
	    }

	    dropZone.ondragleave = function() {
	        this.className = 'upload-drop-zone';
	        return false;
	    }

	}(jQuery);
	</script>
</head>
<body>
<div class="container">
      <div class="panel panel-default">
        <div class="panel-heading"><strong>Upload Files</strong></div>
        <div class="panel-body">

          <!-- Standar Form -->
          <h4>Select files from your computer</h4>
          <form id="js-upload-form">
            <div class="form-inline">
              <div class="form-group">
                <input type="file" name="files[]" id="js-upload-files" multiple>
              </div>
              <button type="submit" class="btn btn-sm btn-primary" id="js-upload-submit">Upload files</button>
            </div>
          </form>

          <!-- Drop Zone -->
          <h4>Or drag and drop files below</h4>
          <div class="upload-drop-zone" id="drop-zone">
            Just drag and drop files here
          </div>

          <!-- Progress Bar -->
          <div class="progress">
            <div id="progress-bar" class="progress-bar" role="progressbar" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100" style="width: 1%;">
              <span id="dispprog"></span>
            </div>
          </div>

          <!-- Upload Finished -->
          <div class="js-upload-finished">
            <h3>Processed files</h3>
            <div class="list-group" id="list-group"></div>
          </div>
        </div>
      </div>
      <!-- modal -->
      <div class="modal fade" id="myModal" role="dialog">
	   <div class="modal-dialog modal-lg">
	      <div class="modal-content">
	        <div class="modal-header">
	          <button type="button" class="close" data-dismiss="modal">&times;</button>
	          <h4 class="modal-title">Duplicate File</h4>
	        </div>
	        <div class="modal-body">
	          <p>A file already exists with the given name. Do you want to rename the uploaded file or overwrite the existing one?</p>
	          <div class="checkbox">
                        <label>
                            <input type="checkbox"/> Do the same for all
                        </label>
                      </div>
	        </div>
	        <div class="modal-footer">
	        	<button id="rename-btn" type="button" class="btn btn-default">Rename</button>
	          	<button id="overwrite-btn" type="button" class="btn btn-default" data-dismiss="modal">Overwrite</button>
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
                      <button id="rename-upload-btn" class="btn btn-default">Upload</button>
                    </div>
                </div>
             </form>
	      </div>
	    </div>
	  </div><!-- /modal -->
	  <script>
	  $(function(){
		    $("#rename-btn").click(function(){
		    	$('#myModal').modal('hide');
		    	$('#renameModal').modal('show');
		    });
		    $("#rename-upload-btn").click(function(){
		    	renamedFname = document.getElementById("inputFname").value;
		    	console.log('renamed file name is '+renamedFname);
		    	$('#renameModal').modal('hide');
		    });
		    $(function(){
		        $("#overwrite-btn").click(function(){
			    	$('#myModal').modal('show');
		        });
		    });
		});
	  </script>
	  </div>
    </div> <!-- /container -->
</body>
</html>