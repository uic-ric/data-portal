package edu.uic.rrc.portal.listener;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.model.entities.User;


/**
 * Application Lifecycle Listener implementation class AppRequestListener
 *
 */
@WebListener
public class AppRequestListener implements ServletRequestListener {

	private static final String ARVADOS = "ARVADOS";
	private static final String DBCONNECTION = "dbconnection";
	private static final String ENTITY_MGR = "entity_mgr";
	
    /**
     * Default constructor. 
     */
    public AppRequestListener() {
        // TODO Auto-generated constructor stub
    }

	/**
     * @see ServletRequestListener#requestDestroyed(ServletRequestEvent)
     */
    public void requestDestroyed(ServletRequestEvent ev)  { 
    	// Close the entity manager object, if it exists and is open.
    	EntityManager em = (EntityManager) ev.getServletRequest().getAttribute(ENTITY_MGR);
   	   	if ( em != null && em.isOpen() ) {
   	   		em.close();
   	   		ev.getServletRequest().removeAttribute(ENTITY_MGR);
   	   	}

   	   	// Close DB connection, if it exists
    	Connection dbConn = (Connection) ev.getServletRequest().getAttribute(DBCONNECTION);
    	try {
    		if ( dbConn != null) {
    			dbConn.close();
    			ev.getServletRequest().removeAttribute(DBCONNECTION);
    		}
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	
    }

	/**
     * @see ServletRequestListener#requestInitialized(ServletRequestEvent)
     */
    public void requestInitialized(ServletRequestEvent arg0)  { 
 //   	ServletContext context = arg0.getServletContext();
	}
			
    public static Connection getDbConnection(HttpServletRequest request) throws SQLException {
    	Object conn = request.getAttribute(DBCONNECTION);
    	if ( conn == null ) {
    		conn = AppConfigListener.getConnection();
    		request.setAttribute(DBCONNECTION, conn);
		}
    	return (Connection) conn;
    }
    
    public static ArvadosAPI getArvadosApi(HttpServletRequest request) throws Exception {
    	Object arv = request.getAttribute(ARVADOS);
    	if ( arv == null ) {
    		User self = getSelf(request);
    		arv = AppConfigListener.getArvadosApi(self);
    		request.setAttribute(ARVADOS, arv);
    }
    	return (ArvadosAPI) arv;
    }
    
    /**
     * Static method to retrieve/generate the entity manager object for the current request
     * @param request
     * @return EntityManager object
     */
    public static EntityManager getEntityManager(HttpServletRequest request) {
    	Object em = request.getAttribute(ENTITY_MGR);
    	if ( em == null ) {
    		em = AppConfigListener.getEntityManager();
    		request.setAttribute(ENTITY_MGR, em);
    	}
    	return (EntityManager) em;
    }
    
	public static User getUser(EntityManager em, String userid) {
		return em.find(User.class, userid);
	}
	
	public static User getSelf(HttpServletRequest request) {
		return getUser(getEntityManager(request), request.getRemoteUser());
	}

	public static User getUser(HttpServletRequest request, String userid) {
		return getUser(getEntityManager(request), userid);
	}
}
