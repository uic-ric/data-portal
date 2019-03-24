package edu.uic.rrc.portal;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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

import edu.uic.rrc.portal.listener.AppRequestListener;
import edu.uic.rrc.portal.model.entities.ProjectMembership;
import edu.uic.rrc.portal.model.entities.User;
import edu.uic.rrc.portal.model.entities.User.Role;

/**
 * Servlet implementation class UsersServlet
 */
@WebServlet({"/user", "/users/*", "/update-profile", "/ssh-keys", "/update-password", "/reset-password"})
public class UsersServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	private static final String PROJECT_ID = "project_id";
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		EntityManager em = AppRequestListener.getEntityManager(req);

		if ( req.getServletPath().equals("/users") ) {
			// If there is more to the path, then there is a request for a project.
			String path = req.getPathInfo();
			if ( req.getHeader("Accept").startsWith("application/json") ) {
				resp.setContentType("application/json");
				
				if ( path == null ) {			
					List<User> users = ( req.getParameter("search") != null ? User.findUsers(em, req.getParameter("search")) : User.getAllUsers(em) );
					PrintWriter out = resp.getWriter();
					
					JsonGenerator generator = Json.createGenerator(out);
					generator.writeStartArray();
					
					for ( User user : users ) {
						generator.write(user.toJSON());
					}
					
					generator.writeEnd();
					generator.flush();
					generator.close();				
				}
			} else if ( path != null && path.length() > 1 ) {
				String[] pathparts = path.split("/", 3);
				String projectid = pathparts[1];    	

				// Checks to see if the user is an admin or member of the project.  Otherwise send 403 message.			
				if ( ! req.isUserInRole("admin") ) {
					ProjectMembership member = getMember(em, projectid, req.getRemoteUser());
					if ( member == null ) {
						em.close();
						resp.sendError(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
				}

				req.setAttribute(PROJECT_ID, projectid);
				//			resp.setContentType("application/json");
				//			PrintWriter out = resp.getWriter();
				req.setAttribute(ProjectsServlet.PROJECT_USERS_ATTR, ProjectMembership.getProjectMembers(em, projectid));
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/usertable.jsp");
				reqDispatcher.forward(req, resp);
			}
		} else if (req.getServletPath().equals("/reset-password") ) {
			RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/resources/password-reset.jsp");
			reqDispatcher.forward(req, resp);							
		} else if ( req.getServletPath().equals("/ssh-keys") ) {
			RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/ssh-keys.jsp");
			reqDispatcher.forward(req, resp);				
		}
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String path = req.getPathInfo();
		String accept = req.getHeader("Accept");
		if ( accept == null ) { accept = "text/html"; }

		EntityManager em = AppRequestListener.getEntityManager(req);

		if( req.getServletPath().equals("/update-profile") ){
			try {
				User user = em.find(User.class, req.getRemoteUser());
				em.getTransaction().begin();
				if ( user == null ) {
					user = new User(req.getRemoteUser(), req.getParameter("fullname"), req.getParameter("affiliation"));
					em.persist(user);
				} else {
					user.setAffiliation(req.getParameter("affiliation"));
					user.setName(req.getParameter("fullname"));
				}				
				em.getTransaction().commit();
				
				if ( req.getHeader("Accept").contains("application/json") ) {
					resp.setContentType("application/json");
					PrintWriter out = resp.getWriter();
					out.print(user.toJSON().toString());
				} else {
					String url = req.getParameter("url") != null ? req.getParameter("url") : req.getContextPath();
					resp.sendRedirect(url);					
					return;					
				}
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else if (req.getServletPath().equals("/update-password") ) {
			try {
				User user = em.find(User.class, req.getRemoteUser());
				if ( user != null ) {
					if ( user.checkPassword(req.getParameter("current_pass")) ) {
						if ( req.getParameter("new_pass").equals(req.getParameter("new_pass2"))) {
							em.getTransaction().begin();
							user.setNewPassword(req.getParameter("new_pass"));	
							em.getTransaction().commit();
							if ( req.getHeader("Accept").contains("application/json") ) {
								PrintWriter out = resp.getWriter();
								resp.setContentType("application/json");
								out.print(user.toJSON().toString());
							} else {
								String url = req.getParameter("url") != null ? req.getParameter("url") : req.getContextPath();
								resp.sendRedirect(url);					
								return;					
							}
						} else {
							PrintWriter out = resp.getWriter();
							resp.setContentType("application/json");
							out.print("{ \"error\": \"New passwords do not match.\"}");
							resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
							return;
						}
					} else {
						PrintWriter out = resp.getWriter();
						resp.setContentType("application/json");
						out.print("{\"error\": \"Current password incorrect.\"}");
						resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
						return;
					}
				}
				
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else if ( req.getServletPath().equals("/reset-password") ) {
			// Reset a password.
			try {
				User user = em.find(User.class, req.getParameter("email"));
				if ( req.getParameter("reset") != null ) {
					if ( user.getPass().equals(req.getParameter("reset")) ) {
						if ( req.getParameter("new_pass").equals(req.getParameter("new_pass2"))) {
							em.getTransaction().begin();
							user.setNewPassword(req.getParameter("new_pass"));	
							em.getTransaction().commit();
							RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/resources/password-reset-complete.jsp");
							reqDispatcher.forward(req, resp);														
						} else {
							req.setAttribute("error", "Passwords do not match.");
							RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/resources/password-reset.jsp");
							reqDispatcher.forward(req, resp);							
							return;
						}
					} else {
						req.setAttribute("error", "Invalid reset token.");
						RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/resources/password-reset.jsp");
						reqDispatcher.forward(req, resp);							
						return;						
					}					
				} else {
					em.getTransaction().begin();
					user.resetPassword();
					em.getTransaction().commit();
					AdministrationServlet.sendResetEmail(req, user);
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/resources/password-reset-sent.jsp");
					reqDispatcher.forward(req, resp);							
					return;											
				}
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else if ( req.getServletPath().equals("/ssh-keys") ) {
			RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/ssh-keys.jsp");
			reqDispatcher.forward(req, resp);				
		} else if ( path != null && path.length() > 1 ) {
			// If there is more to the path, then there is a request for a project.
			
			String[] pathparts = path.split("/", 3);
			if(pathparts[1].equals("add-admin")){
				try {
					User user = new User(req.getParameter("userid"), req.getParameter("name"), Role.ADMIN, req.getParameter("affiliation"));
					em.persist(user);
					RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/manage-users");
					reqDispatcher.forward(req, resp);
				} catch (Exception e) {
					req.setAttribute("message", e.getMessage());
				}
				return;
			}
			String projectid = pathparts[1];    	

			// Checks to see if the user is an admin or owner of the project.  Otherwise send 403 message.			
			if ( ! req.isUserInRole("admin") ) {
				ProjectMembership member = getMember(em, projectid, req.getRemoteUser());
				boolean isowner = member == null ? false : member.getOwner() == 0;
				if ( isowner ) {
					em.close();
					resp.sendError(HttpServletResponse.SC_FORBIDDEN);
					return;
				}
			}

			// Check to see the action performed on the users.
			if ( pathparts.length == 3 ) {
				EntityTransaction tx = em.getTransaction();

				tx.begin();
				if ( pathparts[2].equalsIgnoreCase("remove-user") ) {
					
					removeUsersFromProject(em, projectid, req.getParameter("userid"));
				} else if ( pathparts[2].equalsIgnoreCase("add-user") ) {
					User user = em.find(User.class, req.getParameter("userid"));
					if ( user == null ) {
						user = new User(req.getParameter("userid"));
						user.resetPassword();
						em.persist(user);
						AdministrationServlet.sendCreateUserEmail(req, user);
					}
					ProjectMembership member = new ProjectMembership(projectid, user, req.getParameter("owner") != null ? 1 : 0);
					em.persist(member);
					//sending email to user saying that he/she has been added to the project
					AdministrationServlet.sendUserEmail(req, member);
				} else if ( pathparts[2].equalsIgnoreCase("revoke-owner") ) {
					ProjectMembership member = getMember(em, projectid, req.getParameter("userid"));
					member.setOwner(0);
				} else if ( pathparts[2].equalsIgnoreCase("make-owner") ) {
					ProjectMembership member = getMember(em, projectid, req.getParameter("userid"));
					member.setOwner(1);
				}
				tx.commit();
			}
			
			List<ProjectMembership> members = ProjectMembership.getProjectMembers(em, projectid);
			
			if ( accept.startsWith("application/json") ) {
				resp.setContentType("application/json");
				PrintWriter out = resp.getWriter();
				out.println(ProjectsServlet.membersToJson(members));
			} else {
				req.setAttribute(PROJECT_ID, projectid);
				req.setAttribute(ProjectsServlet.PROJECT_USERS_ATTR, members);
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/usertable.jsp");
				reqDispatcher.forward(req, resp);				
			}
		}
	}
    /**
     * Remove a member from a project
     * @param projectid
     * @param users
     */
	public static void removeMembers(EntityManager em, String projectid, String[] users) {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		String hql = "DELETE FROM ProjectMembership WHERE projectId= :projectId AND userid= :userid";
		Query query = em.createQuery(hql);
		query.setParameter("projectId", projectid);
		query.setParameter("userid", users);
		query.executeUpdate();
		tx.commit();
	}
	/**
	 * Generate project membership list as a JSON
	 * @param members
	 * @return ProjectMembershipList in JSON format
	 */
	public static JsonObject getJSON(List<ProjectMembership> members) {
		JsonObjectBuilder builder = Json.createObjectBuilder();		
		JsonArrayBuilder users = Json.createArrayBuilder();
		Iterator<ProjectMembership> iter = members.iterator();
		
		if ( iter.hasNext() ) {
			users.add(getJSON(iter.next()));
		}
		builder.add("users", users);
		return builder.build();
	}
	
	/**
	 * Generate project membership list as a JSON.  Uses stream based JSON to generate output
	 * @param members
	 * @param out
	 */
	public static void writeJSON(List<ProjectMembership> members, Writer out) {
		JsonGenerator generator = Json.createGenerator(out);
		
		generator.writeStartArray("users");
		
		for ( ProjectMembership member : members ) {
			writeJSON(member, generator);
		}
		
		generator.writeEnd();
		generator.flush();
	}

	/**
	 * Generate membership information in JSON format.  Uses stream based JSON to generate output
	 * @param member
	 * @param generator
	 */
	public static void writeJSON(ProjectMembership member, JsonGenerator generator) {
		generator.writeStartObject();
		generator.write("id", member.getId().getUserid());
		generator.write("project", member.getId().getProjectid());
		generator.write("owner", member.getOwner() == 1);

		User user = member.getUser();
		if ( user.getName() != null ) {
			generator.write("name", user.getName());
		} else {
			generator.writeNull("name");
		}
		
		if ( user.getAffiliation() != null ) {
			generator.write("affiliation", user.getAffiliation());
		} else {
			generator.writeNull("affiliation");
		}
		generator.writeEnd();
	}

	
	/**
	 * Generate membership information in JSON format
	 * @param member
	 * @return ProjectMembership as JsonObject
	 */
	public static JsonObject getJSON(ProjectMembership member) {
		JsonObjectBuilder json = Json.createObjectBuilder();
		
		json.add("id", member.getId().getUserid());
		json.add("project", member.getId().getProjectid());
		json.add("owner", member.getOwner() == 1);

		User user = member.getUser();
		if ( user.getName() != null ) {
			json.add("name", user.getName());
		}
		if ( user.getAffiliation() != null ) {
			json.add("affiliation", user.getAffiliation());
		}
		return json.build();
	}
	/**
	 * 
	 * @param projectid
	 * @param userid
	 */
	public static void removeUsersFromProject(EntityManager em, String projectid,String userid) {
		ProjectMembership.removeUserFromProject(em, projectid, userid);
	}
	
	public static String getProjectID(HttpServletRequest request) {
		return (String)request.getAttribute(PROJECT_ID);
	}

	/**
	 * 
	 * @param projectid
	 * @param userid
	 * @return ProjectMembership
	 */
	public static ProjectMembership getMember(EntityManager em, String projectid, String userid) {
		TypedQuery<ProjectMembership> query = em.createQuery("FROM ProjectMembership WHERE id.projectid = :projectid AND userid = :userid", ProjectMembership.class);
		query.setParameter("projectid", projectid);
		query.setParameter("userid", userid);
		List<ProjectMembership> results = query.getResultList();
		return results.isEmpty() ? null : results.get(0);
	}
	
	public static boolean isMember(EntityManager em, String projectid, String userid) {
		Query query = em.createQuery("FROM ProjectMembership WHERE id.projectid = :projectid AND userid = :userid");
		query.setParameter("projectid", projectid);
		query.setParameter("userid", userid);
		return query.getResultList().size() == 1;
	}
	
	public static ProjectMembership getMyMembership(HttpServletRequest request, String projectid) {
		EntityManager em = AppRequestListener.getEntityManager(request);
		return getMember(em, projectid, request.getRemoteUser());
	}
	
	public static User getUser(EntityManager em, String userid) {
		return em.find(User.class, userid);
	}
	
	public static User getSelf(HttpServletRequest request) {
		return getUser(AppRequestListener.getEntityManager(request), request.getRemoteUser());
	}
}
