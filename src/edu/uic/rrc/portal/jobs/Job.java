/**
 * 
 */
package edu.uic.rrc.portal.jobs;

import javax.json.JsonObject;

/**
 * @author George Chlipala
 *
 */
public abstract class Job implements Runnable {
	
	protected final StringBuilder output = new StringBuilder();
	protected Exception error = null;
	
	protected boolean running = false;
	protected boolean runJob = true;
	
	public void stop() {
		this.runJob = false;
	}
	
	/**
	 * Returns true if the arv-put process is currently running.
	 * 
	 * @return
	 */
	public boolean isRunning() { 
		return this.running;
	}
	
	/**
	 * Return any error thrown by job
	 * 
	 * @return
	 */
	public Exception getError() { 
		return this.error;
	}
	
	/**
	 * Return the output from the arv-put command
	 * 
	 * @return
	 */
	public String getOutput() { 
		return this.output.toString();
	}
	
	/**
	 * Get the progress information for the current job
	 * 
	 * @return
	 */
	public abstract JsonObject getDetails();
	
	
	/**
	 * Get current job progress (0 to 1, or -1 for indeterminate)
	 * 
	 * @return
	 */
	public abstract double getProgress();
	

}
