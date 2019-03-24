/**
 * 
 */
package edu.uic.rrc.portal.jobs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/**
 * A Runnable class to perform arv-put commands as a separate thread
 * 
 * @author George Chlipala
 *
 */
public class ArvPutJob extends Job {
	
	private String apiHost;
	private String apiToken;
	private File rootDirectory;
	
	private String projectUUID = null;
	private String collectionName = null;
	private String collectionUUID = null;
	
	private final List<String> files = new ArrayList<String>();
	
	private Process proc = null;
	private boolean delete = false;
	
	/**
	 * 
	 */
	public ArvPutJob(String apiHost, String apiToken, File rootDirectory) {
		this.apiHost = apiHost;
		this.apiToken = apiToken;
		this.rootDirectory = rootDirectory;
	}

	/**
	 * Tell the arv-put job to add the files to a new collection in the specified project.
	 * 
	 * @param projectUUID UUID of the project
	 * @param collectionName name of the new collection
	 */
	public void setNewCollection(String projectUUID, String collectionName) {
		this.projectUUID = projectUUID;
		this.collectionName = collectionName;
	}
	
	/**
	 * Add the files to an existing collection
	 * 
	 * @param collectionUUID UUID of existing collection
	 */
	public void setCollection(String collectionUUID) {
		this.collectionUUID = collectionUUID;
	}
	
	/**
	 * Add a file to be added to the collection
	 * 
	 * @param file
	 */
	public void addFile(String file) {
		this.files.add(file);
	}
	
	public void deleteFiles() { 
		this.delete = true;
	}
	
	public void retainFiles() {
		this.delete = false;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		List<String> command = this.buildCommand();
		
		if ( command != null ) {
			ProcessBuilder pb = new ProcessBuilder(command);			
			// Set the Arvados environmental variables for arv-put.
			pb.environment().put("ARVADOS_API_HOST", this.apiHost);
			pb.environment().put("ARVADOS_API_TOKEN", this.apiToken);
			
			pb.directory(this.rootDirectory);
			
			pb.redirectErrorStream(true);
			
			if ( this.runJob ) {
				try {
					this.proc = pb.start();
					this.running = true;
					BufferedReader procSTDOUT = new BufferedReader(new InputStreamReader(this.proc.getInputStream()));
					String line;
					while ( (line = procSTDOUT.readLine()) != null ) {
						this.output.append(line);
						this.output.append("\n");
					}				
					procSTDOUT.close();
					if ( proc.exitValue() != 0 ) {
						this.error = new Exception("arv-put exited with non-zero exit code.");
					} else if ( this.delete ) {
						for ( String file : this.files ) {
							File fileobj = new File(this.rootDirectory, file);
							fileobj.delete();
						}
					}
				} catch (IOException e) {
					this.error = e;
					e.printStackTrace();
				} finally {
					this.running = false;
				}				
			}	
		}		
	}
	
	
	/**
	 * Build the command 
	 * 
	 * @return
	 */
	private List<String> buildCommand() { 
		if ( this.files.isEmpty() ) {
			this.error = new Exception("No files specified to push into Arvados.");
			return null;		
		}
		
		List<String> command = new ArrayList<String>();
		command.add("arv-put");
		command.add("--no-cache");
		command.add("--batch-progress");
		
		if ( this.collectionUUID != null ) {
			command.add("--update-collection");
			command.add(this.collectionUUID);
		} else if ( this.projectUUID != null ){
			command.add("--project-uuid");
			command.add(this.projectUUID);
			if ( this.collectionName != null ) {
				command.add("--name");
				command.add(this.collectionName);
			}
		} else {
			this.error = new Exception("Need to specify either an existing collection or project for the new collection.");
			return null;
		}
		
		command.addAll(this.files);
		
		return command;
	}
	
	/**
	 * Stop the job, if running, also prevent from running if it hasn't started yet.
	 */
	public void stop() { 
		super.stop();
		if ( this.proc != null && this.proc.isAlive() ) {
			this.proc.destroy();
		}
	}

	@Override
	public JsonObject getDetails() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("job_type", "ArvPut");
		if ( this.error != null ) {
			builder.add("error", this.error.getMessage());
		}
		builder.add("messages", this.output.toString());
		return builder.build();
	}
	@Override
	public double getProgress() {
		if ( this.running )
			return -1;
		if ( this.proc != null )
			return 1;
		return 0;
	}

}
