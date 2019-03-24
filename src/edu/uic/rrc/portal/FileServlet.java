package edu.uic.rrc.portal;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.log4j.Logger;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.jobs.ArvPutJob;
import edu.uic.rrc.portal.jobs.JobManager;
import edu.uic.rrc.portal.listener.AppRequestListener;
import edu.uic.rrc.portal.listener.AppSessionListener;
import edu.uic.rrc.portal.model.entities.ProjectMembership;
import edu.uic.rrc.portal.model.entities.Release;
import edu.uic.rrc.portal.model.entities.UploadedFile;
import edu.uic.rrc.portal.model.entities.User;

/**
 * Servlet implementation class FileServlet handles request to serve the files from the arvados keep-web
 * @author SaiSravith
 *
 */
@WebServlet(value = {"/file/*","/file-upload/*","/view-stg-files","/view-file/*","/add-files/*", "/del-files/*"}, loadOnStartup = 1)
public class FileServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	// Constants 
	// Max file size 2 MB
	public static  long maxFileSize;
	static final DateFormat JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm a z");

	public static int BUFFER_SIZE = 4096; // Default size is 4 kB
	private static String KEEP_WEB;
	private static String FILE_PATH;
	private static File file ;
	private static Logger logger = Logger.getLogger(FileServlet.class);
	
	
	private static String ARVADOS_API_TOKEN = null;
	private static String ARVADOS_API_HOST = null;
	private static String ARVADOS_API_HOST_INSECURE = null;
	private static String MAX_UPLOAD_FILE_SIZE = null;
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		
		String size = context.getInitParameter("buffer-size");
		if ( size != null ) {
			BUFFER_SIZE = Integer.parseInt(size);
			this.log(String.format("Set transfer buffer size to: %d B", BUFFER_SIZE));
		}	
		if(MAX_UPLOAD_FILE_SIZE == null) {
			MAX_UPLOAD_FILE_SIZE = context.getInitParameter("MAX_UPLOAD_FILE_SIZE");			
		}
		if(MAX_UPLOAD_FILE_SIZE != null) {
			maxFileSize = Long.parseLong(MAX_UPLOAD_FILE_SIZE);
		}
		else {
			maxFileSize = 2 *1024 * 1024;
		}
			
		
	    
		KEEP_WEB = context.getInitParameter("KEEP_WEB");
		FILE_PATH = context.getInitParameter("FILE_PATH");
    	if ( ARVADOS_API_TOKEN == null )
    		ARVADOS_API_TOKEN = context.getInitParameter("ARVADOS_API_TOKEN");
    	if ( ARVADOS_API_HOST == null ) 
    		ARVADOS_API_HOST = context.getInitParameter("ARVADOS_API_HOST");
    	if ( ARVADOS_API_HOST_INSECURE == null ) 
    		ARVADOS_API_HOST_INSECURE = context.getInitParameter("ARVADOS_API_HOST_INSECURE");
	}
	
	protected void doHead(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		logger.info("FileServlet:: making a head request..");
		String path = request.getPathInfo();

		if(request.getServletPath().equals("/file-upload")){
			String[] parts = path.split("/",3);
			if(parts.length>=2){
				File projectDir = new File(FILE_PATH, parts[1]);
				file = new File( projectDir, parts[2]) ;
				// If the file already exists, return a 409 (Conflict)				
				if ( file.exists() ) {
					response.setStatus(HttpServletResponse.SC_CONFLICT);
					return;
				}
				else{
					response.setStatus(HttpServletResponse.SC_OK);
					return;
				}
			}
		}
		
		logger.info("FileServlet:: done head request..");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if(request.getServletPath().equals("/file")){
			String path = request.getPathInfo();		
			if ( path == null ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			
			path = path.substring(1);
			// If the user is neither an admin or member of the project send 403 (FORBIDDEN)
			if ( ! request.isUserInRole("admin") ) {
				if ( ! ProjectsServlet.canReadFile(request, path) ) {
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;				
				}
			}
			String[] parts = path.split("/", 2);
			if ( parts.length < 2 ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			try {
				ArvadosAPI arv = AppRequestListener.getArvadosApi(request);		
				CloseableHttpResponse keepResponse = arv.getCollectionResource(KEEP_WEB, parts[0], parts[1]);
				try {
					sendResponse(keepResponse, response);
				} finally {
					keepResponse.close();
				}	
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}
		else if(request.getServletPath().equals("/view-file")){
			if ( ! request.isUserInRole("admin") ) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				return;				
			}
			String path = request.getPathInfo();		
			if ( path == null ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			String[] parts = path.split("/", 3);
			byte[] buff = new byte[BUFFER_SIZE];
			File projectDir = new File(FILE_PATH, parts[1]);
			if ( ! projectDir.exists() ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			}
			file = new File( projectDir, parts[2]) ;		
			if ( file.exists() ) {
				InputStream ip = new FileInputStream(file);
				OutputStream out = response.getOutputStream();
				try{
					while((ip.read(buff))>0){
						out.write(buff);
					}
				}
				catch(Exception e){
					logger.error(e.getMessage());
				}
				finally{
					ip.close();
					out.close();
				}
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			}
			else{
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}
	}
	
	
	// Handle all POST requests as a GET
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String servletPath = request.getServletPath();
		
		
		
		if ( servletPath.equals("/add-files") ) {
			// Code to add uploaded files to the project
			String path = request.getPathInfo();
			PrintWriter out = response.getWriter();
			// If the user is not an admin.  Send 403 status (Forbidden)
			if ( ! request.isUserInRole("admin") ) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				out.print("{ error: 'Access denied'}");
				return;
			}
			
			// If the path was not properly specified, send 404 (Not found)
			if ( path == null ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				out.print("{ error: 'Path not found'}");
				return;				
			}
			
			String[] parts = path.split("/",2);	
			
			// If the path was not properly specified, send 404 (Not found)
			if ( parts.length != 2 ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				out.print("{ error: 'Path not found'}");
				return;								
			}
						
			// Get the current user and setup the ArvPutJob.  Use the token associated with the user or default token for the application.
			User user = AppRequestListener.getSelf(request);
			String token = ( user != null ? user.getToken() : null );				
			ArvPutJob job = new ArvPutJob(ARVADOS_API_HOST, ( token != null ? token : ARVADOS_API_TOKEN), new File(FILE_PATH, parts[1]));
			
			if ( request.getParameter("collection_id") == null )
				job.setNewCollection(parts[1], request.getParameter("collection_name"));
			else
				job.setCollection(request.getParameter("collection_id"));
			
			for ( String file : request.getParameterValues("files[]") ) {
				job.addFile(file);
			}
			
			JobManager mgr = AppSessionListener.getJobManager(request);
			mgr.addJob(job);
			for(int i=1;i<=5;i++) {
				try {
					if(mgr.isRunning())
						Thread.sleep(i*1000);
					else
						break;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
			File projectDir = new File(FILE_PATH, parts[1]);
			for ( String file : request.getParameterValues("files[]") ) {
				File realFile = new File(projectDir, file.toString());
				 realFile.delete();
			}
			
		} else if ( servletPath.equals("/del-files") ) {
			PrintWriter out = response.getWriter();
			String path = request.getPathInfo();
			// If the user is not an admin.  Send 403 status (Forbidden)
			if ( ! request.isUserInRole("admin") ) {
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				out.print("{ error: 'Access denied'}");
				return;
			}
			
			// If the path was not properly specified, send 404 (Not found)
			if ( path == null ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				out.print("{ error: 'Path not found'}");
				return;				
			}
			
			String[] parts = path.split("/",2);	
			
			// If the path was not properly specified, send 404 (Not found)
			if ( parts.length != 2 ) {
				response.setStatus(HttpServletResponse.SC_NOT_FOUND);
				out.print("{ error: 'Path not found'}");
				return;								
			}
			// Code to delete uploaded files.
			File projectDir = new File(FILE_PATH, parts[1]);
			for ( String file : request.getParameterValues("files[]") ) {
				File realFile = new File(projectDir, file.toString());
				 realFile.delete();
			}
			// Code to delete the uploaded files from database
			EntityManager em = AppRequestListener.getEntityManager(request);
			String filePath = request.getPathInfo();
			String[] eachFile = filePath.split("/");
			String [] fileIDs = new String[request.getParameterValues("files[]").length];
			int i=0;
			for ( String file : request.getParameterValues("files[]") ) {
				fileIDs[i] = eachFile[1] +"/"+file;
				i++;
			}	
			UploadedFile.removeFiles(em,fileIDs);
			em.close();
			
		} else {
			this.doGet(request, response);			
		}
		
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doPut(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * 
	 * The PUT method for the FileServlet is use to for file uploads.  All file upload paths should contain a project UUID and the name of the file.
	 * If the file already exists, the servlet will return a 409 (Conflict).
	 * If the file is created it will return a 201 (Created)
	 * If the file is too large it will return 413 (Request entity too large)
	 * If the user is not allowed, it will return 403 (Forbidden)
	 */
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		logger.info("FileServlet:: uploading files using PUT method!!");
		String path = request.getPathInfo();
		byte[] buff = new byte[BUFFER_SIZE];
		if(request.getServletPath().equals("/file-upload")) {
			String[] parts = path.split("/");
			if( parts.length > 2 ){
				
				if ( ! request.isUserInRole("admin") ) {
					EntityManager em = AppRequestListener.getEntityManager(request);
					ProjectMembership member = UsersServlet.getMember(em, parts[1], request.getRemoteUser());
					if ( member == null ) {
						em.close();
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					}					
				}

				if ( request.getContentLength() > maxFileSize ) {
					response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
					return;
				}
				
				InputStream ip = request.getInputStream();
				// First check if the project directory exists.  If not create it.
				File projectDir = new File(FILE_PATH, parts[1]);
				if ( ! projectDir.exists() ) {
					projectDir.mkdirs();
				}
				file = new File( projectDir, parts[2]) ;
				
				// If the file already exists, return a 409 (Conflict)				
				if ( file.exists() && (! "overwrite".equalsIgnoreCase(request.getQueryString()) ) ) {
					response.setStatus(HttpServletResponse.SC_CONFLICT);
					return;
				}
				FileOutputStream fo = new FileOutputStream(file);
				try{
					int bytes = 0;
					while( (bytes=ip.read(buff)) != -1){
						fo.write(buff, 0, bytes);
					}
					fo.flush();
				}
				catch(Exception e){
					file.delete();
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					logger.error(e.getMessage());
				}
				finally{
					EntityManager em = AppRequestListener.getEntityManager(request);
					em.getTransaction().begin();
					UploadedFile file = new UploadedFile(parts[1] + "/" + parts[2], AppRequestListener.getSelf(request), parts[1]);
					System.out.println(file.getFileID()+"->"+file.getProjectID()+"->"+file.getUploadDate()+"->"+file.getUploadedBy());
					try {
					em.persist(file);
					em.getTransaction().commit();
					}catch (RollbackException mysql) {
						response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
							File realFile = new File(projectDir, file.toString());
							 realFile.delete();						
						return;
					}
					fo.close();
				}
				response.setStatus(HttpServletResponse.SC_CREATED);
			}
			
		}
		logger.info("FileServlet:: uploaded files successfully using put..");
	}
	
	/**
	 * Get a list of files for a project
	 * @param projectID ID of the project
	 * @return
	 */
	static List<File> getStagedFiles(String projectID) {
		File rootDir = new File(FILE_PATH);
		if ( rootDir != null ) {
			File projDir = new File(rootDir, projectID);
			if ( projDir.exists() ) {
				return Arrays.asList(projDir.listFiles(new FileFilter() { public boolean accept(File f) { return f.isFile(); } }));
			}
		}
		return new ArrayList<File>();
	}
	
	public static Release getRelease(HttpServletRequest request, String releaseID) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		return em.find(Release.class, releaseID);
	}
	
	static JsonValue getStagedFilesJson(String projectID) {
		JsonObjectBuilder builder = Json.createObjectBuilder();
	
		JsonArrayBuilder items = Json.createArrayBuilder();
		
		File rootDir = new File(FILE_PATH);
		if ( rootDir != null ) {
			File projDir = new File(rootDir, projectID);
			if ( projDir.exists() ) {
				for ( File file : projDir.listFiles(new FileFilter() { public boolean accept(File f) { return f.isFile(); } })) {
					JsonObjectBuilder fileElem = Json.createObjectBuilder();
					fileElem.add("name", file.getName());
					fileElem.add("date", LocalDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), TimeZone
		                    .getDefault().toZoneId()).format(AdministrationServlet.DATE_FORMAT));
					fileElem.add("size", file.length());
					items.add(fileElem.build());
				}
			}
		}
		builder.add("data", items.build());
		return builder.build();
	}
	
	static void sendResponse(CloseableHttpResponse fileResponse, HttpServletResponse response) throws IOException {
		int status = fileResponse.getStatusLine().getStatusCode();
		response.setStatus(status);
		
		if ( status == 200 ) {
			HttpEntity entity = fileResponse.getEntity();

			OutputStream out = response.getOutputStream();
			InputStream fileIn = entity.getContent();			
			
			long length = entity.getContentLength();
			
			if (length <= Integer.MAX_VALUE) {
			  response.setContentLength((int)length);
			} else {
			  response.addHeader("Content-Length", Long.toString(length));
			}
			
			if ( entity.getContentType() != null ) {
				response.setContentType(entity.getContentType().getValue());
			}
			
			if ( entity.getContentEncoding() != null )
				response.setCharacterEncoding(entity.getContentEncoding().getValue());		

			byte[] buffer = new byte[BUFFER_SIZE];
			int count;
			while ( (count = fileIn.read(buffer)) > 0  ) {
				out.write(buffer, 0, count);
			}
			out.close();
		}
	}
}
