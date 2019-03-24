package edu.uic.rrc.portal.listener;

import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import edu.uic.rrc.portal.jobs.JobManager;

/**
 * Application Lifecycle Listener implementation class AppSessionListener
 *
 */
@WebListener
public class AppSessionListener implements HttpSessionListener {

	private final static String ATTR_JOBS = "jobManager";
	
	/**
     * @see HttpSessionListener#sessionCreated(HttpSessionEvent)
     */
    public void sessionCreated(HttpSessionEvent se)  { 
         // TODO Auto-generated method stub
    }

	/**
     * @see HttpSessionListener#sessionDestroyed(HttpSessionEvent)
     */
    public void sessionDestroyed(HttpSessionEvent se)  { 
    	HttpSession session = se.getSession();
    	Object obj = session.getAttribute(ATTR_JOBS);
    	if ( obj instanceof JobManager ) {
    		((JobManager)obj).stop();
    	}
    }
	
    /**
     * Get the JobManager for this session
     * 
     * @param session
     * @return
     */
    public static JobManager getJobManager(HttpSession session) {
    	Object obj = session.getAttribute(ATTR_JOBS);
    	if ( obj instanceof JobManager ) {
    		return (JobManager)obj;
    	}
    	JobManager mgr = new JobManager();
    	session.setAttribute(ATTR_JOBS, mgr);
    	return mgr;
    }
    
    /**
     * Get the JobManager for the current session
     * 
     * @param request
     * @return
     */
    public static JobManager getJobManager(HttpServletRequest request) {
    	return getJobManager(request.getSession());
    }
    
}
