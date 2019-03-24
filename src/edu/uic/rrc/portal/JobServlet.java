package edu.uic.rrc.portal;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonGenerator;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.uic.rrc.portal.jobs.Job;
import edu.uic.rrc.portal.jobs.JobManager;
import edu.uic.rrc.portal.listener.AppSessionListener;

/**
 * Servlet implementation class JobServlet
 */
@WebServlet(
		asyncSupported = true, 
		urlPatterns = { 
				"/jobs", 
				"/jobs/*"
		})
public class JobServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
 	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JobManager jobMgr = AppSessionListener.getJobManager(request);
		
		if ( request.getParameter("status") != null ) {
			if ( request.getHeader("Accept").contains("application/json") ) {
				JsonGenerator generator = Json.createGenerator(response.getWriter());
				generator.writeStartObject();
				generator.write("running", jobMgr.isRunning());
				Job job = jobMgr.getCurrentJob();
				if ( job != null ) {
					generator.writeStartObject("job");
					generator.write("progress", job.getProgress());
					generator.write("details", job.getDetails());
					generator.writeEnd();
				}
				generator.write("queue_length",jobMgr.queueLength());
				generator.write("completed_jobs", jobMgr.getCompletedJobs().size());
				generator.writeEnd();
				generator.flush();
			} else {
				RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/job-status.jsp");
				reqDispatcher.forward(request, response);
			}
		} else {
			String path = request.getPathInfo();
			if ( path != null && path.length() > 1 ) {
				String[] parts = path.split("/+");
				if ( parts.length == 2 ) {
					Job job = null;
					if ( parts[1].equalsIgnoreCase("current") ) {
						job = jobMgr.getCurrentJob();
					} else if ( Pattern.matches("^[0-9]+$", parts[1]) ) {
						job = jobMgr.getCompletedJobs().get(Integer.parseInt(parts[1]));
					}
					
					if ( request.getHeader("Accept").contains("application/json") ) {
						JsonObjectBuilder builder = Json.createObjectBuilder();
						if ( job != null ) {
							builder.add("progress", job.getProgress());
							builder.add("details", job.getDetails());
						} else {
							builder.add("error", "Job not found");
						}
						response.getWriter().print(builder.build());
						return;
					} else {
						request.setAttribute("job", job);
						RequestDispatcher reqDispatcher = getServletContext().getRequestDispatcher("/job.jsp");
						reqDispatcher.forward(request, response);
					}
				} else { 
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
					return;
				}				
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	public static Job getJob(HttpServletRequest request) {
		Object obj = request.getAttribute("job");
		if ( obj instanceof Job ) 
			return (Job)obj;
		return null;
	}

}
