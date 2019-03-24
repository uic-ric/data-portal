package edu.uic.rrc.portal;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.stream.JsonGenerator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.arvados.ArvadosList;
import edu.uic.rrc.portal.listener.AppConfigListener;
import edu.uic.rrc.portal.listener.AppRequestListener;
import edu.uic.rrc.portal.listener.ProjectSyncScheduleListener;
import edu.uic.rrc.portal.mail.EmailType;
import edu.uic.rrc.portal.model.entities.ProjectFiles;
import edu.uic.rrc.portal.model.entities.ProjectMembership;
import edu.uic.rrc.portal.model.entities.Release;
import edu.uic.rrc.portal.model.entities.User;
import edu.uic.rrc.portal.model.entities.User.Role;
import edu.uic.rrc.portal.model.entities.arvados.Collection;
import edu.uic.rrc.portal.model.entities.arvados.Filter;
import edu.uic.rrc.portal.model.entities.arvados.Group;
import edu.uic.rrc.portal.model.entities.arvados.Collection.CollectionFile;
import edu.uic.rrc.portal.model.entities.arvados.Filter.Operator;

/**
 * This servlet acts as a controller for all the requests pertaining to portal administrative activities
 * @author Sai Sravith Reddy Marri, Amogh Venkatesh
 */
@WebServlet(urlPatterns = {"/manage-projects", "/manage-projects/*" , "/show-release/*",
		"/remove-projects", "/manage-users", "/show-releases", 
		"/remove-users", "/update-owner", "/manage-admins", 
		"/arv-tables/projects", "/arv-tables/collections"}, loadOnStartup = 1)
@ServletSecurity(@HttpConstraint(rolesAllowed="admin"))
public class AdministrationServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String PORTAL_USERS = "portal_users";
	public static final String ADMIN_USERS = "admin_users";
	public static final String PROJECT_USERS_ATTR = "project_users";
	public static final String RELEASES = "releases";
	public static final String PROJECT_FILES = "project_files";
	
	//public final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SXXX");
	public final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.ENGLISH);
	
	Context initCtx;
	Context envCtx;
	static Session session;
	
	@Override
	public void init() throws ServletException {
		super.init();
		try {
			initCtx = new InitialContext();
			envCtx = (Context) initCtx.lookup("java:comp/env");
			session = (Session) envCtx.lookup("mail/Session");
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletPath = req.getServletPath();
		String pathInfo = req.getPathInfo();	
		EntityManager em = AppRequestListener.getEntityManager(req);

		try {

			
			if (servletPath.equals("/remove-projects")) {

			} else if ( servletPath.equals("/manage-projects") ) {
				
				
				if ( pathInfo != null && pathInfo.length() > 1 ) {
					String[] parts = pathInfo.split("/", 3);
					ArvadosAPI arv = getArv(req);
					req.setAttribute("project", arv.getGroup(parts[1]));
					
					String view = "/admin/project.jsp";
					req.setAttribute(PROJECT_USERS_ATTR, getMembersForProject(em, parts[1]));
					
					// It may be better to return the collection as a JSON and have the web client parse and display in page (AJAX)
					if ( parts.length == 3) {
						view = "/admin/collection.jsp";
						req.setAttribute("collection", arv.getCollection(parts[2]));
					}
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);
					
					
				} else {  		
					if ( req.getHeader("Accept").startsWith("application/json") ) {
						projectsJson(req, resp);					
					} else {
						RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/admin/projects.jsp");
						reqDispatcher.forward(req, resp);
					}
				}
			} else if ( servletPath.equals("/manage-users") ) {
				String accept = req.getHeader("Accept");
				if ( accept.startsWith("application/json") ) {
					PrintWriter out = resp.getWriter();
					List<User> users;
					if ( req.getParameter("admin") != null ) {
						users = User.getAdmins(em);
					} else {
						users = User.getAllUsers(em);
	//					List<User> users = User.findUsers(em, queryString, orderBy, orderAsc, offset, length);
					}
					out.println(ProjectsServlet.usersToJson(users));
				} else {
					String view = "/admin/users.jsp";
					List<User> users = User.getAllUsers(em);
					req.setAttribute(PORTAL_USERS, users);
					List<User> adminUsers =User.getAdmins(em);
					req.setAttribute(ADMIN_USERS, adminUsers);
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);					
				}
			} else if ( servletPath.equals("/show-admins") ) {
				PrintWriter out = resp.getWriter();
				List<User> users = User.getAdmins(em);
				out.println(ProjectsServlet.usersToJson(users));
			} else if ( servletPath.equals("/show-releases") ) {
				String view = "/admin/releases.jsp";
				req.setAttribute(RELEASES, Release.getAllReleases(em));
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
				reqDispatcher.forward(req, resp);
			} else if ( servletPath.equals("/show-release") ) {
				if ( pathInfo != null && pathInfo.length() > 1 ) {
					String[] parts = pathInfo.split("/", 3);
					String view = "/admin/release.jsp";
					ProjectsServlet.setRelease(req, em.find(Release.class, new Long(parts[1])));
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);
				} else {
					String view = "/admin/releases.jsp";
					req.setAttribute(RELEASES, Release.getAllReleases(em));
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);
				}
			} else if ( servletPath.equals("/arv-tables/projects") ) {
				// Code to return JSONs for Arvados projects to use in dataTables.
				projectsJson(req, resp);
			} else if ( servletPath.equals("/arv-tables/collections") ) {
				// Code to return JSONs for Arvados projects to use in dataTables.
				collectionsJson(req, resp);
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String servletPath = req.getServletPath();
		String pathInfo = req.getPathInfo();
		EntityManager em = AppRequestListener.getEntityManager(req);
		
		String accept = req.getHeader("Accept");
		if ( accept == null ) { accept = "text/html"; }

		try {
			if ( servletPath.equals("/manage-projects") ) {				
				if ( pathInfo != null && pathInfo.length() > 1 ) {
					String[] parts = pathInfo.split("/", 3);
					ArvadosAPI arv = getArv(req);
					/* 
					 * Performing check for renaming the file and calling renameFile method 
					 */
					if ( parts.length == 3) {
						req.setAttribute("collection", arv.getCollection(parts[2]));
						// Code to rename a file.
						String submitButton = req.getParameter("ok");					
						if( submitButton != null && submitButton.equals("Confirm") ) {
							Collection collection1 = getCurrentCollection(req);
							String currPath = req.getParameter("myfieldname");
							String oldName = currPath.substring(currPath.lastIndexOf("/") + 1);
							String oldPath = currPath.substring(currPath.indexOf("/"), currPath.lastIndexOf("/"));
							if ( oldPath.length() == 0 ) { oldPath = "."; } else { oldPath = ".".concat(oldPath); }
							
							String newName = req.getParameter("newFilename");
							if ( collection1.renameFile(oldPath, oldName, newName) ) {
								collection1 = arv.updateResource(collection1);
							} else {
								// Cannot process entry send 422
								resp.setStatus(422);
							}
						} 
					}
					req.setAttribute("project", arv.getGroup(parts[1]));
					String view = "/admin/project.jsp";
					req.setAttribute(PROJECT_USERS_ATTR, getMembersForProject(em, parts[1]));

					// It may be better to return the collection as a JSON and have the web client parse and display in page (AJAX)
					if ( parts.length == 3) {
						view = "/admin/collection.jsp";
						req.setAttribute("collection", arv.getCollection(parts[2]));
						if ( "release-files".equalsIgnoreCase(req.getParameter("action")) ) {
							// If the action is "release-files" then release files
							EntityTransaction tx = em.getTransaction();
							tx.begin();
							
							/*
							String[] fileIDs = req.getParameterValues("fileList");
							Set<String> fileIDSet = new HashSet<String>(Arrays.asList(fileIDs));
							String[] hiddenVals = req.getParameterValues("hidden");
							Set<String> hidden=new HashSet<String>();
							if(hiddenVals!=null)
								hidden = new HashSet<String>(Arrays.asList(hiddenVals));
							*/
							
							String title = req.getParameter("release_title");
							if ( title.length() < 1 ) {
								DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT);
								title = "Release - " + format.format(new Date());
							}
							Release release = new Release(AppRequestListener.getSelf(req), req.getParameter("release_title"), req.getParameter("release_notes"));
							em.persist(release);

							JsonReader reader = Json.createReader(new StringReader(req.getParameter("files")));
							JsonArray fileList = reader.readArray();
							
							String parentPath = req.getParameter("parent_path");
							boolean hasParent = parentPath != null && parentPath.length() > 0 && ( ! parentPath.equals("/") );
							
							if ( hasParent && (! parentPath.startsWith("/")) ) {
								parentPath = "/" + parentPath;
							}
							
							for ( int i = 0; i < fileList.size(); i++ ) {
								JsonObject item = fileList.getJsonObject(i);
								String description = item.isNull("description") ? "" : item.getString("description");
								//System.out.println("^^^ "+parts[1]+"-^^id="+item.getString("id"));
								ProjectFiles file = new ProjectFiles(parts[1], item.getString("id"), item.getString("filetype"), description, 
										release, item.getBoolean("hide"));
								if ( hasParent ) {
									File newPath = new File(parentPath, file.getPath());
									file.setPath(newPath.getPath());
								}
								em.persist(file);
							}
							tx.commit();
							em.refresh(release);
							req.setAttribute("message", String.format("Added release to project - <a href=\"%s/release/%d\">View release</a>", 
									req.getContextPath(), release.getReleaseID()));
							sendReleaseEmail(req, release);
						}
						else if ( "revoke-files".equalsIgnoreCase(req.getParameter("action")) ) {
							String[] fileIDs = req.getParameterValues("revokeList");
							if(fileIDs==null)
								throw new Exception("File list is empty");
							ProjectFiles.revokeFiles(em, fileIDs);
							Release.clearEmptyReleases(em);
							req.setAttribute("message", "Revoked files from project");
						}
					}
					// If the Accept header is application/json then send the JSON of the collection.
					if ( accept.startsWith("application/json") ) {
						resp.setContentType("application/json");
						PrintWriter out = resp.getWriter();
						Collection collection = AdministrationServlet.getCurrentCollection(req);
						//renameFile(req, collection);
						out.print(fileJson(req, collection).toString());
					} else {
						// Otherwise forward to the collection view.
						RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
						reqDispatcher.forward(req, resp);
						
					}
				} else {  		
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/admin/projects.jsp");
					reqDispatcher.forward(req, resp);
				}
			} else if ( servletPath.equals("/show-releases") ) {
				String view = "/admin/releases.jsp";
				if ( "revoke-release".equalsIgnoreCase(req.getParameter("action")) ) {
					String[] releaseIDs = req.getParameterValues("revokeList");
					// Should not need to do this. Releases are setup to cascade...
//					for(String relId : releaseIDs){
//						ProjectFiles.revokeFilesForRelease(em, relId);
//					}
					Release.removeReleases(em, releaseIDs);
					
					req.setAttribute("message", "Removed releases from project");
					//send email to notify the release has been removed??
					req.setAttribute(RELEASES, Release.getAllReleases(em));
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);
				}
			} else if ( servletPath.equals("/manage-admins") ) {
				// Code to manage administrators
				if ( accept.startsWith("application/json") ) {
					EntityTransaction tx = em.getTransaction();
					User user = em.find(User.class, req.getParameter("userid"));
					if ( user != null ) {
						tx.begin();
						user = new User(req.getParameter("userid"), req.getParameter("fullname"), Role.ADMIN, req.getParameter("affiliation"));
						user.resetPassword();
						sendCreateUserEmail(req, user);
						em.persist(user);
						tx.commit();
					}
					PrintWriter out = resp.getWriter();
					List<User> users = User.getAdmins(em);
					out.println(ProjectsServlet.usersToJson(users));
				} else {
					String view = "/admin/users.jsp";
					List<User> users = User.getAllUsers(em);
					req.setAttribute(PORTAL_USERS, users);
					List<User> adminUsers =User.getAdmins(em);
					req.setAttribute(ADMIN_USERS, adminUsers);
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher(view);
					reqDispatcher.forward(req, resp);
				}
			}
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	public static void projectsJson(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");						
		PrintWriter out = response.getWriter();
		JsonObjectBuilder builder = Json.createObjectBuilder();
		
		if ( request.getParameter("draw") != null ) 
			builder.add("draw", request.getParameter("draw"));
		
		int start = ( request.getParameter("start") != null ? Integer.parseInt(request.getParameter("start")) : 0 );
		int length = ( request.getParameter("length") != null ? Integer.parseInt(request.getParameter("length")) : 50 );

		String sort = request.getParameter("order[0][column]");
		String dir = request.getParameter("order[0][dir]");

		if ( sort != null ) {
			sort = request.getParameter("columns[" + sort + "][data]");
			sort = "[\"" + sort + " " + dir.toLowerCase() + "\"]";
			
		} else {
			sort = "[\"name asc\"]";
		}

		String search = request.getParameter("search[value]");			
		try {
			ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
			List<Filter> filters = new ArrayList<Filter>(2);
			filters.add(new Filter("group_class", Operator.EQUAL, "project"));
			if ( search != null ) {
				filters.add(new Filter("name", Operator.ILIKE, "%" + search + "%"));
			} else {
				edu.uic.rrc.portal.model.entities.arvados.User user = arv.getCurrentUser();
				filters.add(new Filter("owner_uuid", Operator.EQUAL, user.getUuid()));
			}
			ArvadosList<Group> projects = arv.getGroups(filters, sort, start, length);			
			
			builder.add("recordsTotal", projects.getTotalSize());
			builder.add("recordsFiltered", projects.getTotalSize());

			HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User> userMap = new HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User>();
			JsonArrayBuilder items = Json.createArrayBuilder();
			Set<String> portalProjectSet = new HashSet<String>(AdministrationServlet.getPortalProjectList(request));
			
			for ( Group project : projects ) {		
				JsonObjectBuilder item = Json.createObjectBuilder();
				project.buildJson(item);
				
				edu.uic.rrc.portal.model.entities.arvados.User owner;

				// Look for the user that owns the project.  
				// If this is a subproject then backtrack until a project that has a user that owns it.
				String ownerID = project.getOwnerUUID();
				//System.out.println("^^^^^^^^^^^^project"+project.getName());
				while ( ArvadosAPI.isGroupUUID(ownerID) ) {
					Group parent = arv.getGroup(ownerID);
					ownerID = parent.getOwnerUUID();
				}
				if ( ! userMap.containsKey(ownerID) ) {
					owner = arv.getUser(ownerID);
					userMap.put(owner.getUuid(), owner);
					owner = arv.getUser(ownerID);
					userMap.put(owner.getUuid(), owner);
				} else {
					owner = userMap.get(ownerID);
				}
				item.add("owner", owner.toJson());
				//System.out.println("^^^^^^^^^^^^owner-"+owner.getUuid()+"--"+owner.getCreatedAt()+"---"+owner.getModifiedAt());
				edu.uic.rrc.portal.model.entities.arvados.User modifier;
				
				if ( ! userMap.containsKey(project.getModifiedByUserUUID()) ) {
					modifier = arv.getUser(project.getModifiedByUserUUID());
					userMap.put(project.getModifiedByUserUUID(), modifier);
				} else {
					modifier = userMap.get(project.getModifiedByUserUUID());
				}
				//System.out.println("^^^^^^^^^^^^^^modifier^"+modifier);
				item.add("modified_by", modifier.toJson());
				
				item.add("released", portalProjectSet.contains(project.getUuid())); 
				items.add(item.build());						
				
			}
			builder.add("data", items);
			
		} catch (Exception e) {
			builder.add("error", e.getMessage());
			e.printStackTrace();
		}
		out.print(builder.build().toString());
	}
	
	/**
	 * Write collections as a JSON
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 */
	public static void collectionsJson(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");						
		PrintWriter out = response.getWriter();
		JsonGenerator generator = Json.createGenerator(out);
		generator.writeStartObject();
		
		if ( request.getParameter("draw") != null ) 
			generator.write("draw", request.getParameter("draw"));
		
		int start = ( request.getParameter("start") != null ? Integer.parseInt(request.getParameter("start")) : 0 );
		int length = ( request.getParameter("length") != null ? Integer.parseInt(request.getParameter("length")) : 50 );

		String sort = request.getParameter("order[0][column]");
		String dir = request.getParameter("order[0][dir]");

		if ( sort != null ) {
			sort = request.getParameter("columns[" + sort + "][data]");
			sort = "[\"" + sort + " " + dir.toLowerCase() + "\"]";
			
		} else {
			sort = "[\"name asc\"]";
		}

		String search = request.getParameter("search[value]");			
		try {
			ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
			List<Filter> filters = new ArrayList<Filter>(2);

			if ( search != null )
				filters.add(new Filter("name", Operator.ILIKE, "%" + search + "%"));
			
			if ( request.getParameter("project") != null )
				filters.add(new Filter("owner_uuid", Operator.EQUAL, request.getParameter("project")));
			
			ArvadosList<Collection> collections = arv.getCollections(filters, sort, start, length);
			
			generator.write("recordsTotal", collections.getTotalSize());
			generator.write("recordsFiltered", collections.getTotalSize());

			HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User> userMap = new HashMap<String, edu.uic.rrc.portal.model.entities.arvados.User>();

			generator.writeStartArray("data");
			
			for ( Collection collection : collections ) {				
				try {
					JsonObjectBuilder item = Json.createObjectBuilder();
					collection.buildJson(item);
					edu.uic.rrc.portal.model.entities.arvados.User modifier;
					if ( ! userMap.containsKey(collection.getModifiedByUserUUID()) ) {
						modifier = arv.getUser(collection.getModifiedByUserUUID());
						userMap.put(collection.getModifiedByUserUUID(), modifier);
					} else {
						modifier = userMap.get(collection.getModifiedByUserUUID());
					}
					item.add("modified_by", modifier.toJson());					
					
					generator.write(item.build());
				} catch (Exception e) {
					generator.writeEnd();
					generator.write("error", e.getMessage());
					e.printStackTrace();
					break;
				} 
			}
			generator.writeEnd();
		} catch (Exception e) {
			generator.write("error", e.getMessage());
			e.printStackTrace();
		}
		generator.writeEnd();
		generator.flush();
		generator.close();
		out.flush();
	}
	
	/**
	 * 
	 * @param request
	 * @return List of Projects in Arvados
	 * @throws Exception
	 */
	public static List<Group> getAllProjects(HttpServletRequest request) throws Exception { 
    	ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
	    Filter[] filters = new Filter[1];
	    filters[0] = new Filter("group_class", Operator.EQUAL, "project");   
	    return arv.getGroups(filters);
    }
    
	/**
	 * 
	 * @param request
	 * @return ArvadosAPI
	 * @throws Exception
	 */
    public static ArvadosAPI getArv(HttpServletRequest request) throws Exception {
    	return AppRequestListener.getArvadosApi(request);
    }
    
    /**
     * 
     * @param request
     * @return current project
     */
    public static Group getCurrentProject(HttpServletRequest request) {
    	return (Group) request.getAttribute("project");
    }
    /**
     * 
     * @param request
     * @return collection
     */
    public static Collection getCurrentCollection(HttpServletRequest request) {
    	return (Collection) request.getAttribute("collection");
    }
    
    public static List<String> getPortalProjectList(HttpServletRequest request) {
    	return ProjectMembership.getProjectList(AppRequestListener.getEntityManager(request));
    }
    
    /**
     * 
     * @param request
     * @param uuid
     * @return project with this uuid
     * @throws Exception
     */
    public static Group getProject(HttpServletRequest request, String uuid) throws Exception {
    	ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
    	return arv.getGroup(uuid);
    }
    /**
     * get the collections for the project
     * @param request
     * @param uuid
     * @return list of collections
     * @throws Exception
     */
    public static List<Collection> getCollectionsForProject(HttpServletRequest request, String uuid) throws Exception {
    	ArvadosAPI arv = AppRequestListener.getArvadosApi(request);
    	Filter[] filters = new Filter[1];
	    filters[0] = new Filter("owner_uuid", Operator.EQUAL, uuid);
    	return arv.getCollections(filters);
    }
    /**
     * In memory filemap that gives fast retrieval of files for a particular project
     * @param request
     * @return Project files map
     */
    public static Map<String,ProjectFiles> getCurrentFileMap(HttpServletRequest request) {
    	Group project = getCurrentProject(request);
    	return ProjectFiles.getFileMap(AppRequestListener.getEntityManager(request), project.getUuid());
    }

    static List<ProjectMembership> getMembersForProject(EntityManager em, String projectID) {
    	TypedQuery<ProjectMembership> query = em.createNamedQuery("allMembers", ProjectMembership.class);
    	//System.out.println("^^^projid-"+projectID);
    	query.setParameter("projectid", projectID);
    	return query.getResultList();
    }
    
     
    static void sendReleaseEmail(HttpServletRequest request, Release release){
    	//get the resource bundle from mailtemplate.properties file
    	ResourceBundle bundle = ResourceBundle.getBundle("files-release-template", request.getLocale());
    	
    	User admin = AppRequestListener.getSelf(request);
    	
		try {
			EntityManager em = AppRequestListener.getEntityManager(request);
	    	Group project = ProjectSyncScheduleListener.getProject(release.getProjectID());
			URL projectURL = new URL(request.getScheme(), request.getLocalName(), request.getContextPath() + "/projects/" + release.getProjectID());
			URL releaseURL = new URL(request.getScheme(), request.getLocalName(), String.format("%s/release/%d", request.getContextPath(), release.getReleaseID()));
    	
			for(ProjectMembership member : ProjectMembership.getProjectMembers(em, release.getProjectID())){   	
				User user = member.getUser();
				releaseEmail(bundle, admin, user, project.getName(), projectURL.toExternalForm(),
						releaseURL.toExternalForm(), release.getDescription()); 
    		}
			releaseEmail(bundle, admin, admin, project.getName(), projectURL.toExternalForm(),
						releaseURL.toExternalForm(), release.getDescription());
			
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    static private void releaseEmail(ResourceBundle bundle, User admin, User user, String projectName, String projectURL, String releaseURL, String description) throws MessagingException, UnsupportedEncodingException {
 		Object[] arguments = { ( user.getName() == null ) ? "" : user.getName(), 
				projectName,
				projectURL,
				releaseURL,
				AppConfigListener.getConcatEmail(), description, 
				AppConfigListener.getBrand(), AppConfigListener.getContactName() }; 
		String messageContent = MessageFormat.format(bundle.getString("messageBody"), arguments);
		String messageSubject = MessageFormat.format(bundle.getString("messageSubject"),  new Object[]{projectName});
		Message message = new MimeMessage(session);

		InternetAddress adminEmail = new InternetAddress(admin.getUserid(), admin.getName());  			
		message.setFrom(adminEmail); //setting from address for the email
		InternetAddress to[] = new InternetAddress[1];
		to[0] = new InternetAddress(user.getUserid());//setting to address for the email
		message.setRecipients(Message.RecipientType.TO, to);
		message.setSubject(messageSubject);
		message.setContent(messageContent, "text/plain");
		Transport.send(message);
		System.out.println("Email sent successfully to : " + user.getUserid());
    }
    
    /**
     * Automatically sends email to the current user as
     * @param request
     * @param response
     * @param userList
     */
    static void sendUserEmail(HttpServletRequest request, ProjectMembership member){
    	//get the resource bundle from mailtemplate.properties file
    	ResourceBundle bundle = ResourceBundle.getBundle("user-add-template", request.getLocale());
    	
    	User admin = AppRequestListener.getSelf(request);
    	
		try {
	    	Group project = ProjectSyncScheduleListener.getProject(member.getId().getProjectid());
			URL url = new URL(request.getScheme(), request.getLocalName(), request.getContextPath() + "/projects/" + member.getId().getProjectid());
    	
			User user = member.getUser();
			Object[] arguments = { ( user.getName() == null ) ? "" : user.getName(), 
					project.getName(),
					url.toExternalForm(), 
					AppConfigListener.getConcatEmail(), AppConfigListener.getBrand(), AppConfigListener.getContactName() }; 
			String messageContent = MessageFormat.format(bundle.getString("messageBody"), arguments);
			String messageSubject = MessageFormat.format(bundle.getString("messageSubject"),  new Object[]{project.getName()});
			Message message = new MimeMessage(session);

			InternetAddress adminEmail = new InternetAddress(admin.getUserid(), admin.getName());  			
			message.setFrom(adminEmail); //setting from address for the email
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(user.getUserid());//setting to address for the email
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setContent(messageContent, "text/plain");
			Transport.send(message);
			System.out.println("Email sent successfully to : " + user.getUserid());
			
			// Send email to admin and owners
			
			EntityManager em = AppRequestListener.getEntityManager(request);
			List<ProjectMembership> owners = ProjectMembership.getProjectOwners(em, member.getId().getProjectid());
			url = new URL(request.getScheme(), request.getLocalName(), request.getContextPath() + "/projects/" + member.getId().getProjectid() + "/users");
			
			Object[] newarguments = { project.getName(),
					( user.getName() == null ) ? "" : user.getName(),
					user.getUserid(),
					url.toExternalForm(),
					AppConfigListener.getConcatEmail(), AppConfigListener.getBrand(), AppConfigListener.getContactName() }; 
			messageContent = MessageFormat.format(bundle.getString("adminBody"), newarguments);
			messageSubject = MessageFormat.format(bundle.getString("adminSubject"),  new Object[]{project.getName()});
			message = new MimeMessage(session);

			message.setFrom(adminEmail); //setting from address for the email			
			message.addRecipient(Message.RecipientType.TO, adminEmail);
			
			for ( ProjectMembership owner : owners ) {
				message.addRecipient(Message.RecipientType.BCC, new InternetAddress(owner.getUser().getUserid(), owner.getUser().getName()));
			}
			
			message.setSubject(messageSubject);
			message.setContent(messageContent, "text/plain");
			Transport.send(message);
			System.out.println("Email sent successfully to administrators.");
			
			
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    static void sendCreateUserEmail(HttpServletRequest request, User user){
    	//get the resource bundle from mailtemplate.properties file
    	ResourceBundle bundle = ResourceBundle.getBundle("user-add-template", request.getLocale());
    	
    	User admin = AppRequestListener.getSelf(request);
    	
		try {
			URL url = new URL(request.getScheme(), request.getServerName(), request.getContextPath() + "/reset-password?email=" + user.getUserid() + "&reset=" + user.getPass() );
    	
			Object[] arguments = { AppConfigListener.getBrand(), url.toExternalForm(), 
					AppConfigListener.getConcatEmail(), AppConfigListener.getContactName() }; 
			String messageContent = MessageFormat.format(bundle.getString("createUserMessageBody"), arguments);
			String messageSubject = MessageFormat.format(bundle.getString("createUserSubject"), new Object[] {AppConfigListener.getBrand()});
			Message message = new MimeMessage(session);

			InternetAddress adminEmail = new InternetAddress(admin.getUserid(), admin.getName());  			
			message.setFrom(adminEmail); //setting from address for the email
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(user.getUserid());//setting to address for the email
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setContent(messageContent, "text/plain");
			Transport.send(message);
			System.out.println("Email sent successfully to : " + user.getUserid());			
			
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    static void sendResetEmail(HttpServletRequest request, User user){
    	//get the resource bundle from mailtemplate.properties file
    	ResourceBundle bundle = ResourceBundle.getBundle("user-add-template", request.getLocale());
    	
		try {
			URL url = new URL(request.getScheme(), request.getServerName(), request.getContextPath() + "/reset-password?email=" + user.getUserid() + "&reset=" + user.getPass() );
    	
			Object[] arguments = { user.getName(), user.getUserid(), url.toExternalForm(), 
					AppConfigListener.getConcatEmail(), AppConfigListener.getContactName() }; 
			String messageContent = MessageFormat.format(bundle.getString("resetPassMessageBody"), arguments);
			String messageSubject = MessageFormat.format(bundle.getString("resetPassSubject"), new Object[] { user.getUserid() });
			Message message = new MimeMessage(session);

			InternetAddress adminEmail = new InternetAddress("no-reply@" + request.getServerName(), AppConfigListener.getBrand());  			
			message.setFrom(adminEmail); //setting from address for the email
			InternetAddress to[] = new InternetAddress[1];
			to[0] = new InternetAddress(user.getUserid());//setting to address for the email
			message.setRecipients(Message.RecipientType.TO, to);
			message.setSubject(messageSubject);
			message.setContent(messageContent, "text/plain");
			Transport.send(message);
			System.out.println("Email sent successfully to : " + user.getUserid());			
			
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    /**
     * Build JSON object from files in collection
     * @param request
     * @param collection
     * @return
     */
    public static JsonArray fileJson(HttpServletRequest request, Collection collection) {
    	JsonArrayBuilder builder = Json.createArrayBuilder();
    	Map<String,ProjectFiles> projectFiles = getCurrentFileMap(request);
    	
    	// Get file properties from the collection
    	Map<String, Object> fileDetails = null;  	
    	Object prop = collection.getProperty("file_details");
    	if ( prop instanceof Map ) 
    		fileDetails = (Map<String,Object>) prop;
    	
    	for (CollectionFile file : collection.getManifest() ) {
    		JsonObjectBuilder filejson = Json.createObjectBuilder();
    		filejson.add("name", file.getFullPath());
    		filejson.add("path", file.getPath());
    		filejson.add("size", file.getSize());
    		filejson.add("collection", collection.getUuid());
    		String fileID = collection.getUuid() + "/" + file.getFullPath();
    		filejson.add("id", fileID);
    		
    		if ( projectFiles.containsKey(fileID) ) {
				ProjectFiles pfile = projectFiles.get(fileID);
				filejson.add("released", true);
				filejson.add("description", pfile.getDescription());
				filejson.add("hidden", pfile.isHidden());
				filejson.add("filetype", pfile.getType());
			} else {
				filejson.add("released", false);
				String description = null;
				Boolean hidden = false;
				String filetype = null;
				
				// If the file_details were present in the collection.  Use to set default values.
				if ( fileDetails != null ) {
					// Details for a file will be keyed by the relative path of the file.
					Object detailObj = fileDetails.get(file.getFullPath());
					if ( detailObj instanceof Map ) {
						Map<String,String> details = (Map<String,String>) detailObj;
						description = details.get("description");
						if ( description == null || description.length() < 1 ) {
							hidden = true;
							description = null;
						}
						filetype = details.get("type");
						if ( filetype == null || filetype.length() < 1 ) {
							filetype = null;
						}	
					}
				}
				// Set the values for the file JSON entry
				if ( description != null )
					filejson.add("description", description);
				else
					filejson.addNull("description");
				filejson.add("hidden", hidden);
				if ( filetype != null )
					filejson.add("filetype", filetype);
				else
					filejson.addNull("filetype");
			}
			builder.add(filejson);
    	}
    	return builder.build();
    }
    
    public static void setArvadosToken(HttpServletRequest request, String token) {
    	EntityManager em = AppRequestListener.getEntityManager(request);
    	EntityTransaction tx = em.getTransaction();
		tx.begin();
		User self = AppRequestListener.getSelf(request);
		self.setToken(token);
		tx.commit();
    }
 
}
