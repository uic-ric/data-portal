package edu.uic.rrc.portal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonGenerator;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.listener.AppConfigListener;
import edu.uic.rrc.portal.listener.AppRequestListener;
import edu.uic.rrc.portal.model.entities.ProjectFiles;
import edu.uic.rrc.portal.model.entities.ProjectMembership;
import edu.uic.rrc.portal.model.entities.Release;
import edu.uic.rrc.portal.model.entities.User;
import edu.uic.rrc.portal.model.entities.arvados.Group;

/**
 * This servlet acts as a controller for all the requests pertaining to portal customer activities like displaying the projects the customer is a part of
 * Project details, adding user to a project,  recent activity , releases, update profile etc.
 * @author SaiSravith
 *
 */
@WebServlet(urlPatterns = {"/projects/*", "/projects", "/add-user","/activity", "/release/*"}, loadOnStartup = 1)
public class ProjectsServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public static final String PROJECT_ID_ATTR = "projectid";
	public static final String PROJECT_ATTR = "current_project";
	public static final String PROJECT_FILES_ATTR = "project_files";
	public static final String PROJECT_RELEASE_FILES_ATTR = "project_rel_files";
	public static final String PROJECT_USERS_ATTR = "project_users";
	public static final String PROJECT_VIEW_ATTR = "project_view";
	public static final String PROJECT_RELEASE_ATTR = "release";
	public static final String GROUP_ATTR = "file_group";
	public static final String USERS_VIEW = "users";
	public static final String USERS_ADD = "add_user";
	public static final String RAWDATA_VIEW = "rawdata";
	public static final String RESULTS_VIEW = "results";
	public static final String REPORTS_VIEW = "reports";
	public static final String UPLOADS_VIEW = "uploads";
	public static final String ZIP_VIEW = "zip";

	public static final String RELEASE_FILES_ATTR = "release_files";
	public static final String RELEASE_VIEW_ATTR = "release_view";
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String path = request.getPathInfo();
		EntityManager em = AppRequestListener.getEntityManager(request);

		// If there is more to the path, then there is a request for a project.
		if ( request.getServletPath().equals("/projects") && path != null && path.length() > 1 ) {
			String[] pathparts = path.split("/+", 3);
			String projectid = pathparts[1];    		
			

			// If Accept is application/json, then generate a JSON for the project
			if ( request.getHeader("Accept").contains("application/json") ) {
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();

				if ( pathparts.length > 1 && pathparts[1].equalsIgnoreCase("recent") ) {
					out.print(recentProjectsJson(request));
					return;
				}
				
				// If the user is not an admin, check if the user is a member of the project.
				if ( ! request.isUserInRole("admin") ) {
					if ( ! UsersServlet.isMember(em, projectid, request.getRemoteUser()) ) {
						em.close();
						response.sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
				}
				
				if ( request.getParameter("tree") != null ) {
                    String type = request.getParameter("type");
                    if ( type != null ) {
                    	switch ( type ) {
                    	case "raw_data":
                    		out.print(genTreeRoot(ProjectFiles.getFiles(em, projectid, "raw"), "raw_data")); break;
                    	case "results":
                    		out.print(genTreeRoot(ProjectFiles.getFiles(em, projectid, "result"), "results")); break;
                    	case "reports":
                    	default:
                    		out.print(genTreeRoot(ProjectFiles.getFiles(em, projectid, "report"), "reports")); break;
                    	}
                    } else if ( request.getParameter("release") != null ) { 
                    	Release release = em.find(Release.class, new Long(request.getParameter("release")));
                    	String title = release.getTitle() != null ? String.format("%d.%s", release.getReleaseID(), release.getTitle()) : String.format("%d. Release %d", release.getReleaseID(), release.getReleaseID());
                    	out.print(genTreeRoot(release.getFiles(), title));
                    } else {
                    	out.print("[]");
                    }
				} else if ( request.getParameter("users") != null ) {
					List<ProjectMembership> members = ProjectMembership.getProjectMembers(em, projectid);
					out.println(membersToJson(members));
				} else if ( request.getParameter("upload") != null ) {
					JsonValue items = FileServlet.getStagedFilesJson(projectid);	
					out.print(items.toString());	
				} else {
					String subpath = pathparts.length == 3 ? ( "/" + pathparts[2] ) : "/";
					if ( ! subpath.endsWith("/") ) subpath = subpath + "/";

					String[] types;
					if ( request.getParameter("type") != null ) {
						types = request.getParameterValues("type");
					} else {
						types = new String[2];
						types[0] = "report";
						types[1] = "result";
					}					

					JsonGenerator generator = Json.createGenerator(out);
					generator.writeStartObject();
					generator.writeStartArray("data");
					
					// Find any subfolders for this path and add those to the list of items.
					for (String folder : ProjectFiles.getSubPaths(em, projectid, subpath) ) {
						generator.writeStartObject();
						File folderFile = new File(folder);
						generator.write("id", folder);
						generator.write("path", subpath);
						
						generator.write("name", folderFile.getName());
						generator.write("type", "dir");		
						generator.writeStartObject("release");
						generator.write("title", "");
						generator.writeEnd();
						generator.writeNull("description");
						generator.write("datatype","");
						
						generator.writeEnd();
					}
					
					// List all the files in this path
					for (ProjectFiles file : ProjectFiles.getFilesForPath(em, projectid, subpath, types)) {
						generator.write(file.toJSON(true));
					}
					generator.writeEnd();
					generator.writeEnd();
					generator.flush();
				}
				return;
			}
			
			// If the user is not an admin, check if the user is a member of the project.
			if ( ! request.isUserInRole("admin") ) {
				if ( ! UsersServlet.isMember(em, projectid, request.getRemoteUser()) ) {
					em.close();
					response.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}
			request.setAttribute(PROJECT_ID_ATTR, projectid);
			try {
				ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
				request.setAttribute(PROJECT_ATTR, arv.getGroup(projectid));
				request.setAttribute("path", pathparts.length == 3 ? pathparts[2] : null);
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/project.jsp");
				reqDispatcher.forward(request, response);
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else if( request.getServletPath().equals("/projects") ){
			try {
				if ( request.getHeader("Accept").startsWith("application/json") ) {
					response.setContentType("application/json");
					PrintWriter out = response.getWriter();
					JsonGenerator generator = Json.createGenerator(out);
					try {
						generator.writeStartObject();
						generator.writeStartArray("data");
						projectsJSON(request, getMyProjects(request), generator);
						generator.writeEnd();
					} catch (Exception e) {
						generator.writeEnd();
						generator.write("error", e.getMessage());
					}
					generator.writeEnd();
					generator.flush();
					return;
				}
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/projects.jsp");
				reqDispatcher.forward(request, response);		
			} catch (Exception e) {
				throw new ServletException(e);
			}
		}else if(request.getServletPath().equals("/activity")){
			try {
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/activity.jsp");
				reqDispatcher.forward(request, response);		
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else if ( request.getServletPath().equals("/release") ) {
			
			if ( request.getHeader("Accept").startsWith("application/json") ) {
				response.setContentType("application/json");
				PrintWriter out = response.getWriter();
				out.print(releaseJson(request));
				return;
			}
			

			if ( path != null && path.length() > 1 ) {
				String[] parts = path.split("/+", 3);

				try {
					String view = "/release.jsp";

					Release release = em.find(Release.class, new Long(parts[1]));
					String projid = release.getProjectID();
					
					// Check if the user is an admin or a member of the project.
					if ( ! request.isUserInRole("admin") ) {
						if ( ! UsersServlet.isMember(em, projid, request.getRemoteUser()) ) {
							em.close();
							response.sendError(HttpServletResponse.SC_FORBIDDEN);
							return;
						}
					}
					ProjectsServlet.setRelease(request, em.find(Release.class, new Long(parts[1])));
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(request, response);	
				} catch (Exception e) {
					throw new ServletException(e);
				}
			} else {
				if ( request.isUserInRole("admin") ) {
					if ( "revoke-release".equalsIgnoreCase(request.getParameter("action")) ) {
						String[] releaseIDs = request.getParameterValues("revokeList");
						Release.removeReleases(em, releaseIDs);
						
						request.setAttribute("message", "Removed releases from project");
						//TODO: send email to notify the release has been removed?
					}
					request.setAttribute(AdministrationServlet.RELEASES, Release.getAllReleases(em));
				} else {
					request.setAttribute(AdministrationServlet.RELEASES, ProjectsServlet.getMyActivity(request));					
				}
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/releases.jsp");
				reqDispatcher.forward(request, response);						
			}
		} 
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		EntityManager em = AppRequestListener.getEntityManager(req);
		
		String accept = req.getHeader("Accept");
		if ( accept == null ) { accept = "text/html"; }
		
		if ( path != null && path.length() > 1 ) {

			String[] pathparts = path.split("/+", 3);
			String projectid = pathparts[1];   
			if ( ! req.isUserInRole("admin") ) {
				ProjectMembership member = UsersServlet.getMember(em, projectid, req.getRemoteUser());
				boolean isowner = member == null ? false : member.getOwner() == 1;
				if ( ! isowner ) {
					em.close();
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}

			String subview = pathparts[2];
			if ( subview.equals(USERS_ADD) || subview.equals(PROJECT_USERS_ATTR) ) {
				//add user to the project
				User user = UsersServlet.getUser(em, req.getParameter("userid"));
				if ( user == null ) {
					user = new User(req.getParameter("userid"));
				}
				addProjectMembership(em, new ProjectMembership(projectid, user, Integer.parseInt(req.getParameter("owner"))));
				List<ProjectMembership> members = ProjectMembership.getProjectMembers(em, projectid);
				if ( accept.startsWith("application/json") ) {
					resp.setContentType("application/json");
					PrintWriter out = resp.getWriter();
					out.println(membersToJson(members));
				} else {
					req.setAttribute(PROJECT_USERS_ATTR, ProjectMembership.getProjectMembers(em, projectid));
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/projects/"+ projectid+"/"+ USERS_VIEW );
					reqDispatcher.forward(req, resp);					
				}
				
			}
		}
	}

	/**
	 * 
	 * @param request
	 * @return List of the ProjectFiles
	 */
	@SuppressWarnings("unchecked")
	public static List<ProjectFiles> getProjectFiles(HttpServletRequest request) {
		return (List<ProjectFiles>) request.getAttribute(PROJECT_FILES_ATTR);
	}
	
	@SuppressWarnings("unchecked")
	public static List<File> getProjectUploadFiles(HttpServletRequest request) {
		return (List<File>) request.getAttribute(PROJECT_FILES_ATTR);
	}
	
	@SuppressWarnings("unchecked")
	public static List<ProjectFiles> getProjectReleaseFiles(HttpServletRequest request) {
		return (List<ProjectFiles>) request.getAttribute(PROJECT_RELEASE_FILES_ATTR);
	}

	/**
	 * 
	 * @param request
	 * @return Group
	 */
	public static Group getCurrentProject(HttpServletRequest request) {
		return (Group) request.getAttribute(PROJECT_ATTR);
	}

	/**
	 * 
	 * @param request
	 * @return Release
	 */
	public static Release getRelease(HttpServletRequest request) {
		return (Release) request.getAttribute(PROJECT_RELEASE_ATTR);
	}

	static void setRelease(HttpServletRequest request, Release release) {
		request.setAttribute(PROJECT_RELEASE_ATTR, release);
	}
	
	public static String getGroup(HttpServletRequest request) {
		Object group = request.getAttribute(GROUP_ATTR);
		if ( group instanceof String) 
			return (String)group;
		return null;
	}
	
	/**
	 * 
	 * @param request
	 * @return ProjectMembershiplist
	 */
	@SuppressWarnings("unchecked")
	public static List<ProjectMembership> getCurrentMembers(HttpServletRequest request) {
		return (List<ProjectMembership>) request.getAttribute(PROJECT_USERS_ATTR);
	}

	/**
	 * 
	 * @param request
	 * @return current project_view
	 */
	public static String getCurrentView(HttpServletRequest request) {
		return (String) request.getAttribute(PROJECT_VIEW_ATTR);
	}
	
	/**
	 * 
	 * @param request
	 * @return currentReleaseView
	 */
	public static String getCurrentReleaseView(HttpServletRequest request) {
		return (String) request.getAttribute(RELEASE_VIEW_ATTR);
	}

	public static JsonArray usersToJson(List<User> users) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for ( User user : users ) {
			builder.add(user.toJSON());
		}
		return builder.build();
	}
	
	public static JsonArray membersToJson(List<ProjectMembership> members) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for ( ProjectMembership member : members ) {
			builder.add(member.toJSON());
		}
		return builder.build();
	}
	
	/**
	 * Get members of the project
	 * @param projectid Arvados UUID of the project
	 * @return ProjectMembers of the given project
	 */
	/*
	@SuppressWarnings("unchecked")
	public static List<ProjectMembership> getProjectMembers(EntityManager em, String projectid) {
		TypedQuery<ProjectMembership> query = em.createQuery("FROM ProjectMembership WHERE projectid = :projectid", ProjectMembership.class);
		query.setParameter("projectid", projectid);
		return query.getResultList();
	}
	*/
	/**
	 * Get released files for a project and a given type ("raw","report","result")
	 * @param projectId Arvados UUID of the project
	 * @param type file type, i.e. "raw", "report", or "result"
	 * @return
	 */
	/*
	@Deprecated
	public static com.google.gson.JsonObject genJson(List<ProjectFiles> pfList) {
		DateFormat df = new SimpleDateFormat("EEE, MMM d, hh:mm a,yyyy.");
		com.google.gson.JsonObject root = new com.google.gson.JsonObject();
		for(ProjectFiles elem : pfList){
			Release release = elem.getRelease();
			buildJson(root, elem.getID().split("/", 2)[1], elem.getID(), release.getRelease_by(), df.format(release.getRelease_date()), release.getRelease_by(), elem.getDescription(),elem.getType());
		}
		return root;
	}
	*/
	/**
	 * Generate JSON array of ProjectFiles
	 * @param pfList List of ProjectFiles
	 * @return JsonArray
	 */
	public static JsonArray filesToJson(List<ProjectFiles> pfList) {
		JsonArrayBuilder builder = Json.createArrayBuilder();
		for ( ProjectFiles file : pfList ) {
			builder.add(file.toJSON());
		}
		return builder.build();
	}

	/*
	@Deprecated
	public static com.google.gson.JsonObject buildJson(com.google.gson.JsonObject root ,String path, String fileId, String releasedby, String modifiedat, String modifiedby, String description, String fileType){
		
		String[] parts = path.split("/");
		String name = parts[0];
		if(parts.length>=2){
			com.google.gson.JsonArray subDirs = root.getAsJsonArray("dirs");
			if(subDirs==null){
				com.google.gson.JsonObject newDir = new com.google.gson.JsonObject();
        		newDir.add("name", new com.google.gson.JsonPrimitive(name));
        		newDir.add("releasedby", new com.google.gson.JsonPrimitive(releasedby));
        		newDir.add("modifiedat", new com.google.gson.JsonPrimitive(modifiedat));
        		newDir.add("modifiedby", new com.google.gson.JsonPrimitive(modifiedby));
        		newDir.add("description", new com.google.gson.JsonPrimitive(description));
        		 com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        		 array.add(newDir);
        		 root.add("dirs",array);
        		 buildJson((com.google.gson.JsonObject)newDir,path.substring(parts[0].length()+1), fileId, releasedby, modifiedat, modifiedby, description, fileType);
        		 return root;
        	}
			boolean contains=false;
			for(com.google.gson.JsonElement elem : subDirs){
				if(((com.google.gson.JsonObject)elem).get("name").getAsString().equals(name)){
					contains=true;
					buildJson((com.google.gson.JsonObject)elem,path.substring(parts[0].length()+1), fileId, releasedby, modifiedat, modifiedby, description, fileType);
	    			return root;
				}
			}
			if(!contains){
				com.google.gson.JsonObject newDir = new com.google.gson.JsonObject();
        		newDir.add("name", new com.google.gson.JsonPrimitive(name));
        		newDir.add("releasedby", new com.google.gson.JsonPrimitive(releasedby));
        		newDir.add("modifiedat", new com.google.gson.JsonPrimitive(modifiedat));
        		newDir.add("modifiedby", new com.google.gson.JsonPrimitive(modifiedby));
        		newDir.add("description", new com.google.gson.JsonPrimitive(description));
        		subDirs.getAsJsonArray().add(newDir);
        		buildJson((com.google.gson.JsonObject)newDir,path.substring(parts[0].length()+1), fileId, releasedby, modifiedat, modifiedby, description, fileType);
        		return root;
			}
		}
		else{
			com.google.gson.JsonArray files = root.getAsJsonArray("files");
			if(files!=null){
				com.google.gson.JsonObject newFile = new com.google.gson.JsonObject();
        		newFile.add("name", new com.google.gson.JsonPrimitive(name));
        		newFile.add("filetype", new com.google.gson.JsonPrimitive(fileType));
        		newFile.add("fileid", new com.google.gson.JsonPrimitive(fileId));
        		newFile.add("releasedby", new com.google.gson.JsonPrimitive(releasedby));
        		newFile.add("modifiedat", new com.google.gson.JsonPrimitive(modifiedat));
        		newFile.add("modifiedby", new com.google.gson.JsonPrimitive(modifiedby));
        		newFile.add("description", new com.google.gson.JsonPrimitive(description));
        		files.getAsJsonArray().add(newFile);
			}
			else{
				com.google.gson.JsonObject newFile = new com.google.gson.JsonObject();
        		newFile.add("name", new com.google.gson.JsonPrimitive(name));
        		newFile.add("filetype", new com.google.gson.JsonPrimitive(fileType));
        		newFile.add("fileid", new com.google.gson.JsonPrimitive(fileId));
        		newFile.add("releasedby", new com.google.gson.JsonPrimitive(releasedby));
        		newFile.add("modifiedat", new com.google.gson.JsonPrimitive(modifiedat));
        		newFile.add("modifiedby", new com.google.gson.JsonPrimitive(modifiedby));
        		newFile.add("description", new com.google.gson.JsonPrimitive(description));
        		com.google.gson.JsonArray array = new com.google.gson.JsonArray();
        		 array.add(newFile);
        		root.add("files",array);
			}
		}
		return root;
	}
	*/
	
	/**
	 * 
	 * @param releaseid
	 * @param type
	 * @return list of files for the given release and type of file
	 */
	public static List<ProjectFiles> getFilesForRelease(EntityManager em, Long releaseid, String type) {
		TypedQuery<ProjectFiles> query = em.createQuery("FROM ProjectFiles WHERE releaseid = :releaseid AND type = :type", ProjectFiles.class);
		query.setParameter("releaseid", releaseid);
		query.setParameter("type", type);
		return query.getResultList();
	}
	
	public static List<String> getGroupsForProject(HttpServletRequest request, String projectID) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		return ProjectFiles.getGroups(em, projectID);
	}

	/**
	 * 
	 * @param projectid
	 * @return ProjectFilesList
	 */
//	public static JsonObject getRawData(String projectid) {
//		return getFiles(projectid, "raw");
//	}
	
	/**
	 * 
	 * @param projectid
	 * @return ProjectFilesList
	 */
//	public static JsonObject getResults(String projectid) {
//		return getFiles(projectid, "result");
//	}

	/**
	 * 
	 * @param projectid
	 * @return ProjectFilesList
	 */
//	public static JsonObject getReports(String projectid) {
//		return getFiles(projectid, "report");
//	}

	/**
	 * Get project for current user.
	 * @param request HttpServletRequest object
	 * @return list of current projects.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static List<Group> getMyProjects(HttpServletRequest request) throws Exception {
		EntityManager em = AppRequestListener.getEntityManager(request);
		Query query = em.createQuery("SELECT id.projectid FROM ProjectMembership WHERE userid = :userid");
		query.setParameter("userid", request.getRemoteUser());
		ArvadosAPI arv = AdministrationServlet.getArv(request);
		List<String> projectList = (List<String>)query.getResultList();
		List<Group> myProjects = new ArrayList<Group>(projectList.size());
		for(String str: projectList){
			myProjects.add(arv.getGroup(str));
		}
		return myProjects;
	}	
	
	/**
	 * Add a project membership object. 
	 * @param projectMembership object to add.
	 * @return Project ID of project.
	 */
	// TODO Should move to UsersServlet
	public static String addProjectMembership(EntityManager em, ProjectMembership projectMembership){
		EntityTransaction tx = em.getTransaction(); 
		try {
			tx.begin();
			em.persist(projectMembership);
			tx.commit();
		} catch ( Exception e ) {
			tx.rollback();
			throw e; 
		}
		return projectMembership.getId().getProjectid();
	}
	/**
	 * If the user is a customer, checks to see if he is a part of the project and if the file is released to him.
	 * @param  request
	 * @param  path
	 * @return allowed Tells if the user is allowed to view the file or not
	 */
	public static boolean canReadFile(HttpServletRequest request, String path) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		boolean allowed=false;
		String sql = "SELECT pf.fileid FROM project_membership pm INNER JOIN project_files pf ON(pm.projectid=pf.projectid) WHERE userid = :userid AND fileid = :fileid";
		Query query = em.createNativeQuery(sql);
		query.setParameter("userid", request.getRemoteUser());
		query.setParameter("fileid", path);
		allowed = query.getResultList().size() > 0;
		return allowed;
	}
	
	/*
	 * {
  id          : "string" // required
  parent      : "string" // required
  text        : "string" // node text
  icon        : "string" // string for custom
  state       : {
    opened    : boolean  // is the node open
    disabled  : boolean  // is the node disabled
    selected  : boolean  // is the node selected
  },
  li_attr     : {}  // attributes for the generated LI node
  a_attr      : {}  // attributes for the generated A node
}
	 */

	
	/**
	 * Method to create a jstree compatible JSON array for all of the files for a project for a give file type.
	 * @param projectId ID of the project
	 * @param fileType File type, e.g. raw, report, or result
	 */
	public static JsonArray genTreeRoot(List<ProjectFiles> pfList, String prefix) {

		JsonArrayBuilder items = Json.createArrayBuilder();
		
		Set<String> dirList = new HashSet<String>();
						
	    for (ProjectFiles file : pfList) {
	    	String[] parts = file.getID().split("/");
	    	JsonObjectBuilder fileElem = Json.createObjectBuilder();
	    	fileElem.add("id", prefix + "/" + file.getID());
            fileElem.add("text", parts[parts.length - 1]);
            fileElem.add("icon", "glyphicon glyphicon-file");
            fileElem.add("file", true);
            if ( parts.length > 2 ) {
                    StringBuffer fileparent = new StringBuffer(prefix + "/" + file.getProjectID());
                    int lastdir = parts.length - 1;
                    for ( int i = 1; i < lastdir ; i++ ) {
                            fileparent.append("/");
                            fileparent.append(parts[i]);

                            if ( ! dirList.contains(fileparent.toString()) ) {
                            	JsonObjectBuilder dir = Json.createObjectBuilder();
                            	dir.add("id", fileparent.toString());
                            	dir.add("parent", ( i > 2 ? fileparent.toString() : prefix));
                            	dir.add("text", parts[i]);
                            	dir.add("icon", "glyphicon glyphicon-folder-open");
                            	items.add(dir);
                            	dirList.add(fileparent.toString());
                            }
                    }
                    fileElem.add("parent", fileparent.toString());
            } else {
                fileElem.add("parent", prefix);
            }
            items.add(fileElem);
	    }
	    return items.build();
	}
	
	public static List<String> getMyRecentProjects(HttpServletRequest request) {
		return getMyRecentProjects(request, 10);
	}
	
	public static List<String> getMyRecentProjects(HttpServletRequest request, int limit) {
		if ( request.isUserInRole("admin") ) 
			return ProjectMembership.getRecentProjects(AppRequestListener.getEntityManager(request), limit);			
		else
			return ProjectMembership.getRecentProjects(AppRequestListener.getEntityManager(request), request.getRemoteUser(), limit);
	}
	
	public static List<Release> getMyActivity(HttpServletRequest request) {
		return Release.getReleasesForUser(AppRequestListener.getEntityManager(request), request.getRemoteUser());
	}
	
	public static JsonValue releaseJson(HttpServletRequest request) throws IOException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		
		if ( request.getParameter("draw") != null )
			builder.add("draw", request.getParameter("draw"));

		int length = ( request.getParameter("length") != null ? Integer.parseInt(request.getParameter("length")) : -1 );

		List<Release> releaseList;
		if ( request.isUserInRole("admin") ) {
			releaseList = Release.getAllReleases(AppRequestListener.getEntityManager(request));
		} else {
			releaseList = Release.getReleasesForUser(AppRequestListener.getEntityManager(request), request.getRemoteUser());
		}
		
		int count = 0;
		JsonArrayBuilder items = Json.createArrayBuilder();
		
		for ( Release release : releaseList ) {
			if ( count == length ) 
				break;
			items.add(release.toJSON());
			count++;
		}

		builder.add("recordsTotal", count);
		builder.add("recordsFiltered", count);
		builder.add("data", items.build());
		return builder.build();
	}
	
	public static String recentProjectsJson(HttpServletRequest request) throws IOException {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		
		if ( request.getParameter("draw") != null ) 
			builder.add("draw", request.getParameter("draw"));
		
//		int start = ( request.getParameter("start") != null ? Integer.parseInt(request.getParameter("start")) : 0 );
		int length = ( request.getParameter("length") != null ? Integer.parseInt(request.getParameter("length")) : 10 );
		
		if ( length < 0 ) length = 10;
		
		String sort = request.getParameter("order[0][column]");
		String dir = request.getParameter("order[0][dir]");

		if ( sort != null ) {
			sort = request.getParameter("column[" + sort + "][name]");
			sort = "[\"" + sort + " " + dir.toLowerCase() + "\"]";
			
		} else {
			sort = "[\"name asc\"]";
		}

		try {
			ArvadosAPI arv = AppRequestListener.getArvadosApi(request);

			List<String> projectList = ProjectsServlet.getMyRecentProjects(request, length);
			Map<String,Group> groupMap = new HashMap<String,Group>(length);
			HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User> userMap = new HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User>();
			JsonArrayBuilder items = Json.createArrayBuilder();

			builder.add("recordsTotal", projectList.size());
			builder.add("recordsFiltered", projectList.size());

			for ( String project : projectList ) {
				Group group = null;
				if ( ! groupMap.containsKey(project) ) {
					try {
						group = arv.getGroup(project);
						}catch(Exception e) {
							if(e.getMessage().contains("404"))
								continue;
						}
					if(group!=null)
					groupMap.put(project, group);
			 	} else {
			 		group = groupMap.get(project);
			 	}
				
				JsonObjectBuilder item = Json.createObjectBuilder();
				group.buildJson(item);
				
				edu.uic.rrc.portal.model.entities.arvados.User owner;
				
				// Look for the user that owns the project.  
				// If this is a subproject then backtrack until a project that has a user that owns it.
				String ownerID = group.getOwnerUUID();				
				while ( ArvadosAPI.isGroupUUID(ownerID) ) {
					Group parent = arv.getGroup(ownerID);
					ownerID = parent.getOwnerUUID();
				}
				if ( ! userMap.containsKey(ownerID) ) {
					owner = arv.getUser(ownerID);
					userMap.put(owner.getUuid(), owner);
				} else {
					owner = userMap.get(ownerID);
				}
				item.add("owner", owner.toJson());
				
				edu.uic.rrc.portal.model.entities.arvados.User modifier;
				
				if ( ! userMap.containsKey(group.getModifiedByUserUUID()) ) {
					modifier = arv.getUser(group.getModifiedByUserUUID());
					userMap.put(group.getModifiedByUserUUID(), modifier);
				} else {
					modifier = userMap.get(group.getModifiedByUserUUID());
				}
				
				item.add("modified_by", modifier.toJson());
				
				items.add(item.build());	
				}
			
			builder.add("data", items);
		} catch (Exception e) {
			builder.add("error", e.getMessage());
			e.printStackTrace();
		}
		return builder.build().toString();
	}
	public static void projectsJSON(HttpServletRequest request, List<Group> projectList, JsonGenerator generator) throws Exception {
		ArvadosAPI arv = AppRequestListener.getArvadosApi(request);

		HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User> userMap = new HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User>();

		for ( Group group : projectList ) {				
			JsonObjectBuilder item = Json.createObjectBuilder();
			group.buildJson(item);
			edu.uic.rrc.portal.model.entities.arvados.User owner;

			// Look for the user that owns the project.  
			// If this is a subproject then backtrack until a project that has a user that owns it.
			String ownerID = group.getOwnerUUID();
			while ( ArvadosAPI.isGroupUUID(ownerID) ) {
				Group parent = arv.getGroup(ownerID);
				ownerID = parent.getOwnerUUID();
			}
			if ( ! userMap.containsKey(ownerID) ) {
				owner = arv.getUser(ownerID);
				userMap.put(owner.getUuid(), owner);
			} else {
				owner = userMap.get(ownerID);
			}
			item.add("owner", owner.toJson());

			edu.uic.rrc.portal.model.entities.arvados.User modifier;

			if ( ! userMap.containsKey(group.getModifiedByUserUUID()) ) {
				modifier = arv.getUser(group.getModifiedByUserUUID());
				userMap.put(group.getModifiedByUserUUID(), modifier);
			} else {
				modifier = userMap.get(group.getModifiedByUserUUID());
			}
			item.add("modified_by", modifier.toJson());
			
			generator.write(item.build());
		}
	}
	
	public static List<Release> getReleasesForProject(Group project, HttpServletRequest request) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		if ( ! request.isUserInRole("admin") ) {
			if ( ! UsersServlet.isMember(em, project.getUuid(), request.getRemoteUser()) ) {	return null; }
		}		
		return Release.getReleasesForProject(em, project.getUuid());
	}
	
	public static List<ProjectMembership> getMembers(Group project, HttpServletRequest request) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		if ( ! request.isUserInRole("admin") ) {
			if ( ! UsersServlet.isMember(em, project.getUuid(), request.getRemoteUser()) ) {	return null; }
		}		
		return ProjectMembership.getProjectMembers(em, project.getUuid());
	}
	
	
	public static String getSFTPURL(String hostname, String projectUUID)  {
		String sftphost = AppConfigListener.getSFTPHost() != null ? AppConfigListener.getSFTPHost() : hostname;
		return "sftp://" + AppConfigListener.getSFTPUser() + "@" + sftphost + "/" + projectUUID;
	}
	
	
	
}
