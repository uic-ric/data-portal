/**
 * 
 */
package edu.uic.rrc.portal.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author George Chlipala
 *
 */
public class JobManager implements Runnable {

	private final BlockingQueue<Job> jobs = new LinkedBlockingQueue<Job>();

	private Job currJob;
	
	private final List<Job> completedJobs = Collections.synchronizedList(new ArrayList<Job>());
	
	private boolean runJob = true;
	private boolean running = false;
	
	private Thread myThread;
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while ( this.runJob ) {
			try {
				this.running = true;
				this.currJob = jobs.take();
				currJob.run();
				this.completedJobs.add(currJob);
				this.running = false;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}	
	
	/**
	 * Stop the job, if running, also prevent from running if it hasn't started yet.
	 */
	public void stop() { 
		this.runJob = false;
		this.currJob.stop();
	}
	
	/**
	 * Create a thread for the job, start it and return
	 * 
	 * @return
	 */
	public Thread start() {
		if ( this.myThread == null || ( ! this.myThread.isAlive() ) ) {
			this.runJob = true;
			this.myThread = new Thread(this);
			this.myThread.start();
		}
		return this.myThread;					
	}
	
	public int queueLength() { 
		return this.jobs.size();
	}
	
	public List<Job> getCompletedJobs() {
		return this.completedJobs;
	}
	
	public boolean addJob(Job job) {
		return this.addJob(job, true);
	}
	
	public boolean addJob(Job job, boolean start) {
		if ( this.jobs.add(job) ) {
			this.start();
			return true;
		}
		return false;
	}
	
	public boolean isRunning() { 
		return this.running;
	}
	
	public Job getCurrentJob() { 
		return this.currJob;
	}
}
