package edu.uic.rrc.portal;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.listener.AppRequestListener;
import edu.uic.rrc.portal.model.entities.ProjectFiles;
import edu.uic.rrc.portal.model.entities.ProjectMembership;
import edu.uic.rrc.portal.model.entities.Release;
import edu.uic.rrc.portal.model.entities.arvados.Group;

/**
 * Servlet implementation class ZipServlet
 */
/**
 * @author George Chlipala
 *
 */
@WebServlet(value = "/zip/*", loadOnStartup = 1)
public class ZipServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static int BUFFER_SIZE = 4096;
	private static String KEEP_WEB ;
       
    public void init(ServletConfig config) throws ServletException{
    	super.init(config);
    	String size = config.getInitParameter("buffer-size");
		if ( size != null ) {
			BUFFER_SIZE = Integer.parseInt(size);
			this.log(String.format("Set transfer buffer size to: %d B", BUFFER_SIZE));
		}
		KEEP_WEB = config.getServletContext().getInitParameter("keep-web");
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		EntityManager em = AppRequestListener.getEntityManager(request);
		String projectID = null;
		Release release = null;
		
		// Get the project, to check for permission.
		if ( request.getParameter("release") != null ) {
			release = em.find(Release.class, new Long(request.getParameter("release")));
			projectID = release.getProjectID();
		} else {
			projectID = path.split("/")[1];
		}

		// If the user is not an admin, then check if the user is a member of the project.
		if ( ! request.isUserInRole("admin") ) {
			ProjectMembership member = UsersServlet.getMember(em, projectID, request.getRemoteUser());
			if ( member == null ) {
				response.sendError(HttpServletResponse.SC_FORBIDDEN);
				em.close();
				return;
			}
		}

		try {
			if ( release != null ) {
				// If there was a release, then generate a ZIP for the release
				this.zipRelease(request, response, release);
			} else {
				// Otherwise, get an array of the files to include in the ZIP and generate the ZIP file.
				String[] files = request.getParameterValues("zipList");
				this.zipFiles(request, response, files);
			}
		}
		catch (Exception e) {
			throw new ServletException(e);
		}
	}
	
	/**
	 * Send a ZIP file for a release.
	 * 
	 * @param request HTTP request
	 * @param response HTTP response object
	 * @param release Release to use
	 * @throws Exception
	 */
	private void zipRelease(HttpServletRequest request, HttpServletResponse response, Release release) throws Exception {
		ArvadosAPI arv = AppRequestListener.getArvadosApi(request);

		String zipName = release.getTitle() != null ? release.getTitle() : String.format("release_%d", release.getReleaseID());
		
		// Setup to send a ZIP file.
		response.setContentType("Content-type: text/zip");
		response.setHeader("Content-Disposition",
				"attachment; filename=" + zipName.replaceAll("\\s+", "_") + ".zip");
		ServletOutputStream out = response.getOutputStream();
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));
		
		for ( ProjectFiles file : release.getFiles() ) {
			String filename = file.getPath() + file.getFilename();
			filename = filename.substring(1);
			zos.putNextEntry(new ZipEntry(filename));
			InputStream inpStream = null;	
			String[] parts = file.getID().split("/", 2);
			CloseableHttpResponse keepResponse = arv.getCollectionResource(KEEP_WEB, parts[0], parts[1]);
			try {
				int status = keepResponse.getStatusLine().getStatusCode();

				if ( status == 200 ) {
					this.log("Successfully retrieved file " + file.getID());

					HttpEntity entity = keepResponse.getEntity();
					inpStream = entity.getContent();		
					BufferedInputStream bis = new BufferedInputStream(inpStream);
					
					byte[] buffer = new byte[BUFFER_SIZE];
					int count;
					while ( (count = bis.read(buffer)) > 0  ) {
						zos.write(buffer, 0, count);
					}
					bis.close();
					zos.closeEntry();
					this.log("Finished zipping file " + file.getID());
				}
			} catch (FileNotFoundException fnfe) {
					// If the file does not exists, write an error entry instead of
					// file
					// contents
					zos.write(("Error could not find file " + file.getID())
							.getBytes());
					zos.closeEntry();
					this.log("Could not find file "	+ file.getID());
					continue;
			} finally {
				keepResponse.close();
			}
		}
	
		zos.close();
		out.flush();		
	}
	
	/**
	 * Generate a ZIP file for a set of files
	 * 
	 * @param request HTTP request
	 * @param response HTTP response object
	 * @param files array of files to include in the ZIP file
	 * @throws Exception
	 */
	private void zipFiles(HttpServletRequest request, HttpServletResponse response, String[] files) throws Exception {
		String projectID = request.getPathInfo().split("/")[1];
		EntityManager em = AppRequestListener.getEntityManager(request);
		// Retrieve the project from Arvados.
		ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
		Group project = arv.getGroup(projectID);

		// If the ziplist was not defined, then return to the project view page.
		if ( files == null || files.length == -1 ) {
			request.setAttribute(ProjectsServlet.PROJECT_ID_ATTR, projectID);
			request.setAttribute(ProjectsServlet.PROJECT_ATTR, project);
			request.setAttribute(ProjectsServlet.PROJECT_VIEW_ATTR, ProjectsServlet.ZIP_VIEW);

			RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/project.jsp");
			reqDispatcher.forward(request, response);
			return;
		}

		// Setup to send a ZIP file.
		response.setContentType("Content-type: text/zip");
		response.setHeader("Content-Disposition",
				"attachment; filename="+ project.getName() +".zip");
		ServletOutputStream out = response.getOutputStream();
		ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(out));
		
		// This is a map of file names, so that we don't overwrite a file.
		Map<String,Integer> fMap = new HashMap<String,Integer>();
		
		for (String filePath : files) {
			String[] parts = filePath.split("/");
			String fileName = filePath.substring(parts[0].length() + parts[1].length() + 2);
			// Determine if the file has been released.  If not, skip the file
			ProjectFiles file = em.find(ProjectFiles.class, parts[1] + "/" + fileName);
			if ( file == null ) { continue; }
			fileName = parts[0] + "/" + fileName;
			// Check if the file was already added.  If so add a number to the end of the file.
			if (fMap.containsKey(fileName)) {
				fMap.put(fileName, fMap.get(fileName)+1);
				String[] fSplit = fileName.split(".");
				String ext = fSplit[fSplit.length-1];
				fileName = fileName.substring(0, fileName.length()-ext.length()-2)+"(" + fMap.get(fileName)+")"+((!fSplit[1].equals(""))?"."+fSplit[1]:"");
			} else {
				fMap.put(fileName, 1);
			}
			zos.putNextEntry(new ZipEntry(fileName));
			InputStream inpStream = null;	
			
			CloseableHttpResponse keepResponse = arv.getCollectionResource(KEEP_WEB, parts[1], fileName);
			try {
				int status = keepResponse.getStatusLine().getStatusCode();

				if ( status == 200 ) {
					this.log("Successfully retrieved file " + filePath);

					HttpEntity entity = keepResponse.getEntity();
					inpStream = entity.getContent();		
					BufferedInputStream bis = new BufferedInputStream(inpStream);
					
					byte[] buffer = new byte[BUFFER_SIZE];
					int count;
					while ( (count = bis.read(buffer)) > 0  ) {
						zos.write(buffer, 0, count);
					}
					bis.close();
					zos.closeEntry();
					this.log("Finished zipping file " + filePath);
				}
			} catch (FileNotFoundException fnfe) {
					// If the file does not exists, write an error entry instead of file contents
					zos.write(("Error could not find file " + filePath)
							.getBytes());
					zos.closeEntry();
					this.log("Could not find file "	+ filePath);
					continue;
			} finally {
				keepResponse.close();
			}
		}
		zos.close();
		out.flush();
	}
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// process the request as if it was a GET
		doGet(request, response);
	}
	
	
}
