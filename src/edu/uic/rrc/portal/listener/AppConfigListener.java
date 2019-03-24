package edu.uic.rrc.portal.listener;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.model.entities.User;

/**
 * Application Lifecycle Listener implementation class AppConfigListener
 *
 */
@WebListener
public class AppConfigListener implements ServletContextListener {

	private static final String DBNAME = "portaldb";
	private static final String DATASOURCE = "datasource";
	public static final String JPA_UNIT_NAME = "data-portal";
	
	private static DataSource ds = null;
    
	private static String APINAME = null;
	private static String API_VERSION = null;
	private static String ARVADOS_API_TOKEN = null;
	private static String ARVADOS_API_HOST = null;
	private static String ARVADOS_API_HOST_INSECURE = null;
	private static String CONTACT_EMAIL = null;
	private static String CONTACT_NAME = null;
	private static String FILE_PATH = null;
	private static String BRAND = null;

	private static String SFTP_HOST = null;
	private static String SFTP_USER = null;
	
	private static EntityManagerFactory factory = null;


    public void contextInitialized(ServletContextEvent event)  { 
    	ServletContext context = event.getServletContext();

    	if ( APINAME == null )
    		APINAME = context.getInitParameter("apiName");    	
    	if ( API_VERSION == null )
    		API_VERSION = context.getInitParameter("apiVersion");
    	if ( ARVADOS_API_TOKEN == null )
    		ARVADOS_API_TOKEN = context.getInitParameter("ARVADOS_API_TOKEN");
    	if ( ARVADOS_API_HOST == null ) 
    		ARVADOS_API_HOST = context.getInitParameter("ARVADOS_API_HOST");
    	if ( ARVADOS_API_HOST_INSECURE == null ) 
    		ARVADOS_API_HOST_INSECURE = context.getInitParameter("ARVADOS_API_HOST_INSECURE");
    	if ( CONTACT_NAME == null )
    		CONTACT_NAME = context.getInitParameter("CONTACT_NAME");
    	if ( CONTACT_EMAIL == null ) 
    		CONTACT_EMAIL = context.getInitParameter("CONTACT_EMAIL");
    	if ( FILE_PATH == null ) 
    		FILE_PATH = context.getInitParameter("FILE_PATH");
    	if ( BRAND == null )
    		BRAND = context.getInitParameter("BRAND");
    	if ( SFTP_HOST == null )
    		SFTP_HOST = context.getInitParameter("SFTP_HOST");
    	if ( SFTP_USER == null )
    		SFTP_USER = context.getInitParameter("SFTP_USER");
    	
    	try {
			Context initCtx = new InitialContext();
			
			if ( ds == null ) {
				ds  = (DataSource) initCtx.lookup("java:comp/env/jdbc/" + DBNAME);
			}
			context.setAttribute(DATASOURCE,ds);
    	} catch ( NamingException e) {
    		context.log("",e);
    	}

    	// Create the EntityManagerFactory if it does not exist.
    	if ( factory == null )
    		factory = Persistence.createEntityManagerFactory(AppConfigListener.JPA_UNIT_NAME);
    	
   }    

    public void contextDestroyed(ServletContextEvent arg0)  { 

    }

    static DataSource getDatasource() { 
    	return ds;
    }

    public static Connection getConnection() throws SQLException { 
    	return ds.getConnection();
    }
	
    public static ArvadosAPI getArvadosApi(User user) throws Exception {
    	String token = ( user != null ? user.getToken() : null );
    	return new ArvadosAPI(APINAME, API_VERSION, ( token != null ? token : ARVADOS_API_TOKEN), ARVADOS_API_HOST, ARVADOS_API_HOST_INSECURE);
    }
    
    public static String getArvadosAPIServer() { 
    	return ARVADOS_API_HOST;
    }

	public static String getConcatEmail() {
		return CONTACT_EMAIL;
	} 
	
	public static String getContactName() { 
		if ( CONTACT_NAME == null )
			return getBrand() + " Administrators";
		return CONTACT_NAME;
	}
	
	public static String getBrand() {
		if ( BRAND == null ) 
			return "Data Portal";
		return BRAND;
	}
	
	public static String getSFTPHost() { 
		return SFTP_HOST;
	}
	
	public static String getSFTPUser() { 
		return SFTP_USER;
	}

	
	/**
	 * Create a new EntityManager.  It is the reponsibility of the downstream code to properly close the object.
	 * Perferred method is to use {@link AppRequestListener#getEntityManager(javax.servlet.http.HttpServletRequest)}.
	 * @return EntityManager
	 */
	public static EntityManager getEntityManager() { 
		return factory.createEntityManager();
	}
}
