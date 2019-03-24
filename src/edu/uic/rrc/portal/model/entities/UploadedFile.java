package edu.uic.rrc.portal.model.entities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

/**
 * Class to store information about uploaded/staged files.
 * 
 * @author George Chlipala
 *
 */
@Entity
@Table(name = "uploaded_files")
@NamedQueries({
	@NamedQuery(
			name = "deleteFiles",
			query = "DELETE FROM UploadedFile f WHERE f.fileID IN (:fileID)"
	),
	@NamedQuery(
			name = "filesForProject",
			query = "SELECT f FROM UploadedFile f WHERE f.projectID = :projectID ORDER BY f.uploadDate ASC"
	)
})
public class UploadedFile {
	
	public static DateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	@Id
	@Column(name = "file_id")
	private String fileID;
	
	@ManyToOne(fetch=FetchType.EAGER, cascade={CascadeType.MERGE})
	@JoinColumn(name="userid")
	private User uploadedBy;

	@Column(name = "upload_date", nullable = false)
	private Date uploadDate;

	@Column(name = "project_id", nullable = false)
	private String projectID;
	
	/**
	 * Delete uploaded file entries.
	 * 
	 * @param em EntityManager
	 * @param fileIDs String array of file IDs to delete.
	 * @return number of entries deleted.
	 */
	public static int removeFiles(EntityManager em, String[] fileIDs) {
		EntityTransaction tx = em.getTransaction();
		if(!tx.isActive()) {
		tx.begin();
		}
		Query query = em.createNamedQuery("deleteFiles");
		query.setParameter("fileID", Arrays.asList(fileIDs));
		int rowcount = query.executeUpdate();
		tx.commit();
	    return rowcount;
	}
	
	/**
	 * Get uploaded files for a project.
	 * 
	 * @param em EntityManager
	 * @param projectID Project UUID
	 * @return List of UploadedFiles for the project
	 */
	public static List<UploadedFile> getFilesForProject(EntityManager em, String projectID) {
		TypedQuery<UploadedFile> query = em.createNamedQuery("filesForProject", UploadedFile.class);
		query.setParameter("projectID", projectID);
		return query.getResultList();
	}
	
	public UploadedFile() {	}

	/**
	 * Create a new UploadedFile.
	 * 
	 * @param fileID File ID (project UUID/filename)
	 * @param user User who uploaded the file
	 * @param projectID Project UUID
	 */
	public UploadedFile(String fileID, User user, String projectID) {
		this.uploadDate = new Date();
		this.fileID = fileID;
		this.uploadedBy = user;
		this.projectID = projectID;
	}	
	
	/**
	 * Get the file ID (project UUID/filename)
	 * 
	 * @return
	 */
	public String getFileID() {
		return fileID;
	}

	/**
	 * User that uploaded the file.
	 * 
	 * @return
	 */
	public User getUploadedBy() {
		return uploadedBy;
	}

	/**
	 * Date the file was uploaded 
	 * 
	 * @return
	 */
	public Date getUploadDate() {
		return uploadDate;
	}

	/**
	 * UUID of the associated project
	 * 
	 * @return
	 */
	public String getProjectID() {
		return projectID;
	}
	
	public JsonObject toJSON() { 
		JsonObjectBuilder builder = Json.createObjectBuilder();
		
		builder.add("file_id", this.fileID);
		builder.add("project_id", projectID);
		String[] parts = this.fileID.split("/", 2);
		builder.add("name", parts[1]);

		builder.add("user", this.uploadedBy.toJSON());
		builder.add("date", jsonDateFormat.format(this.uploadDate));
		return builder.build();
	}
	
}
