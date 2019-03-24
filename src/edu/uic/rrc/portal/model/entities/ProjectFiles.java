package edu.uic.rrc.portal.model.entities;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

/**
 * A JPA (hibernate) entity for the "project_files" table
 * @author SaiSravith
 *
 */
@Entity
@Table(name = "project_files")
@NamedQueries({
	@NamedQuery(
			name = "fetchFiles",
			query = "SELECT f FROM ProjectFiles f WHERE f.projectid = :projectId and f.type = :type AND f.hidden = FALSE AND f.group = NULL ORDER BY f.fileid"
	),
	@NamedQuery(
			name = "fetchFilesForGroup",
			query = "SELECT f FROM ProjectFiles f WHERE f.projectid = :projectId and f.type = :type AND f.hidden = FALSE AND f.group = :group ORDER BY f.fileid"
	),
	@NamedQuery(
			name = "listGroups",
			query = "SELECT DISTINCT f.group FROM ProjectFiles f WHERE f.projectid = :projectId AND f.group IS NOT NULL ORDER BY f.group"
	),	
	@NamedQuery(
			name = "fetchVisibleFiles",
			query = "SELECT f FROM ProjectFiles f WHERE f.projectid = :projectId AND f.hidden = FALSE ORDER BY f.fileid"
	),
	@NamedQuery(
			name = "fetchAllFiles",
			query = "SELECT f FROM ProjectFiles f WHERE f.projectid = :projectId ORDER BY f.fileid"
	),
	@NamedQuery(
			name = "fetchFilesForPath",
			query = "SELECT f FROM ProjectFiles f WHERE f.projectid = :projectid AND f.hidden = FALSE AND f.filepath = :path ORDER BY f.filename"
	),
//	@NamedQuery(
//			name = "revokeFiles",
//			query = "DELETE FROM ProjectFiles f WHERE f.fileid = :fileId"
//	),
//	@NamedQuery(
//			name = "revokeForRelease",
//			query = "DELETE FROM ProjectFiles f WHERE f.release.releaseid = :releaseId"
//	),
	@NamedQuery(
			name = "fetchFilesForRelease",
			query = "SELECT f FROM ProjectFiles f WHERE f.release.releaseid = :releaseId AND f.hidden = FALSE ORDER BY f.fileid"
	)
})
@NamedNativeQueries({	
	@NamedNativeQuery(
		name = "revokeFiles",
		query = "DELETE FROM project_files WHERE fileid = :fileId"
	),
	@NamedNativeQuery(
			name = "revokeForRelease",
			query = "DELETE FROM project_files WHERE releaseid = :releaseId"
	),
	@NamedNativeQuery(
			name = "subPaths",
			query = "SELECT DISTINCT SUBSTRING_INDEX(f.filepath,'/', LENGTH(:path) - LENGTH(REPLACE(:path, '\', '')) + 1) FROM project_files f WHERE f.projectid = :projectId AND f.filepath LIKE :pathQuery ORDER BY f.filepath ASC"
			// query = "SELECT DISTINCT f.filepath FROM project_files f WHERE f.projectid = :projectId AND f.filepath = CONCAT(:path,SUBSTRING_INDEX(f.filepath,'/',-2)) ORDER BY f.filepath ASC"
	)
})
public class ProjectFiles {
	
	/**
	 * Return a list of additional groups for this project and file type.
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @param type file type
	 * @return list of named groups
	 */
	public static List<String> getGroups(EntityManager em, String projectId) {
		TypedQuery<String> query = em.createNamedQuery("listGroups", String.class);
		query.setParameter("projectId", projectId);
		return query.getResultList();
	}

	/**
	 * Return a list of subpaths for a .
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @param parentPath 
	 * @return list of sub paths
	 */
	public static List<String> getSubPaths(EntityManager em, String projectId, String parentPath) {
		Query query = em.createNamedQuery("subPaths");
		query.setParameter("projectId", projectId);
		query.setParameter("path", parentPath);
		query.setParameter("pathQuery", parentPath + "_%");
		return (List<String>) query.getResultList();
	}

	
	/**
	 * Return a list of ProjectFiles for the designated project and file type.  This will only return files in the default (NULL) group
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @param type file type
	 * @return list of ProjectFiles
	 */
	public static List<ProjectFiles> getFiles(EntityManager em, String projectId, String type) {
		TypedQuery<ProjectFiles> query = em.createNamedQuery("fetchFiles", ProjectFiles.class);
		query.setParameter("projectId", projectId);
		query.setParameter("type", type);
		return query.getResultList();
	}
	
	/**
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @param type file type
	 * @param group the selected group
	 * @return list of ProjectFiles
	 */
	public static List<ProjectFiles> getFiles(EntityManager em, String projectId, String type, String group) {
		if ( group == null ) 
			return getFiles(em, projectId, type);
		
		TypedQuery<ProjectFiles> query = em.createNamedQuery("fetchFilesForGroup", ProjectFiles.class);
		query.setParameter("projectId", projectId);
		query.setParameter("type", type);
		query.setParameter("group", group);
		return query.getResultList();
	}
	
	public static List<ProjectFiles> getFilesForPath(EntityManager em, String projectID, String path, String... types) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<ProjectFiles> cq = cb.createQuery(ProjectFiles.class);
		Root<ProjectFiles> root = cq.from(ProjectFiles.class);
		ParameterExpression<String> projectid = cb.parameter(String.class);
		ParameterExpression<String> filepath = cb.parameter(String.class);
		ParameterExpression<Boolean> hidden = cb.parameter(Boolean.class);
		
		cq.select(root).where(
				cb.equal(root.get("projectid"), projectid),
				cb.equal(root.get("hidden"), hidden),
				cb.equal(root.get("filepath"), filepath),
				root.get("type").in(Arrays.asList(types))
		).orderBy(cb.asc(root.get("filename")));	

		TypedQuery<ProjectFiles> query = em.createQuery(cq);
		query.setParameter(projectid, projectID);
		query.setParameter(filepath, path);
		query.setParameter(hidden, Boolean.FALSE);
		return query.getResultList();
	}
	
	/**
	 * Return all visible files for a project
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @return list of ProjectFiles
	 */
	public static List<ProjectFiles> getFiles(EntityManager em, String projectId) {
		return getFiles(em, projectId, false);
	}
	
	/**
	 * Return all visible files for a project
	 * @param em EntityManager
	 * @param projectId UUID of the project
	 * @return list of ProjectFiles
	 */
	public static List<ProjectFiles> getFiles(EntityManager em, String projectId, Boolean showHidden) {
		TypedQuery<ProjectFiles> query = em.createNamedQuery((showHidden ? "fetchAllFiles" : "fetchVisibleFiles" ), ProjectFiles.class);
		query.setParameter("projectId", projectId);
		return query.getResultList();
	}

	/**
	 * Revoke the selected file IDs
	 * @param em EntityManager
	 * @param fileIDs file IDs to revoke
	 * @return number of files revoked
	 */
	public static int revokeFiles(EntityManager em, String[] fileIDs) {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Query query = em.createNamedQuery("revokeFiles");
		int rowcount = 0;
		for ( String id : fileIDs ) {
			query.setParameter("fileId", id);
			rowcount = rowcount + query.executeUpdate();
		}
		tx.commit();
	    return rowcount;
	}

	/**
	 * Revoke the files associated with the specified release ID
	 * @param em EntityManager
	 * @param releaseID ID of the release
	 * @return number of files revoked
	 */
	public static int revokeFilesForRelease(EntityManager em, String releaseID) {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Query query = em.createNamedQuery("revokeForRelease");
		query.setParameter("releaseId", releaseID);
		int rowcount = query.executeUpdate();
		tx.commit();
	    return rowcount;
	}
	
	/**
	 * Retieve all visible files for a specified release
	 * @param em EntityManager
	 * @param releaseID ID of the release
	 * @return list of ProjectFiles
	 */
	public static List<ProjectFiles> getFilesForRelease(EntityManager em, Long releaseID) {
		TypedQuery<ProjectFiles> query = em.createNamedQuery("fetchFilesForRelease", ProjectFiles.class);
		query.setParameter("releaseId", releaseID);
		return query.getResultList();
	}
	
	/**
	 * Returns the visible files for a project as a Map keyed by the file ID.
	 * @param em EntityManager object
	 * @param projectId UUID of the project
	 * @return HashMap of files keyed by file ID
	 */
	public static Map<String,ProjectFiles> getFileMap(EntityManager em, String projectId) {
		List<ProjectFiles> files = getFiles(em, projectId, true);
		Map<String, ProjectFiles> fileMap = new HashMap<String, ProjectFiles>(files.size());
		for ( ProjectFiles file : files ) {
			fileMap.put(file.getID(), file);
		}
		return fileMap;
	}
	
	@ManyToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH})
	@JoinColumn(name="releaseid")
	private Release release;	
	
	@ManyToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH})
	@JoinColumn(name="projectid")
	private List<ProjectMembership> members;
	
	@Id
	@Column(name = "fileid")
	private String fileid;
	
	@Column(name = "projectid", nullable = false)
	private String projectid;

	@Column(name = "filepath", nullable = false)
	private String filepath;

	@Column(name = "filename", nullable = false)
	private String filename;

	@Column(name = "type", nullable = false)
	private String type;
	
	@Column(name = "description", nullable = false)
	private String description = "";
	
	@Column(name = "hidden", nullable = false)
	private Boolean hidden = false;
	
	@Column(name = "file_group", nullable = true)
	private String group = null;
	
	
	ProjectFiles() {
		
	}

	/**
	 * Add a new file to a project.
	 * 
	 * @param projectID Arvados project UUID
	 * @param fileID Arvados locator ID (collection UUID/path)
	 * @param type File type (raw_data, report, result)
	 * @param description Brief description for the file
	 * @param release Release associated with this file
	 */
	public ProjectFiles(String projectID, String fileID, String type, String description, Release release) {
		this.fileid = fileID;
		this.projectid = projectID;
		this.type = type;
		this.description = description;
		this.release = release;
		String[] path = fileID.split("/+", 2);
		File file = new File(path[1]);
		this.filename = file.getName();
		this.filepath = file.getParent();
		if ( file.getParent() == null )
				this.filepath = "/";
		else 
			this.filepath = "/" + file.getParent() + "/";
	}
	/**
	 * Add a new file to a project
	 * 
	 * @param projectID Arvados project UUID
	 * @param fileID Arvados locator ID (collection UUID/path)
	 * @param type File type (raw_data, report, result)
	 * @param description Brief description for the file
	 * @param release Release associated with this file
	 * @param hidden If true the file will not be displayed
	 */
	public ProjectFiles(String projectID, String fileID, String type, String description, Release release, Boolean hidden) {
		this(projectID, fileID, type, description, release);
		this.hidden = hidden;
	}
	
	/**
	 * Get the Arvados locator ID for the file
	 * @return fileid
	 */
	public String getID() {
		return this.fileid;
	}
	/**
	 * Get the Arvados project UUID for the file.
	 * @return projectid
	 */
	public String getProjectID() {
		return this.projectid;
	}
	/**
	 * Return the file type (raw_data, report, result)
	 * @return type
	 */
	public String getType() {
		return type;
	}
	/**
	 * Set the file type (raw_data, report, result)
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/**
	 * Return the path for this file
	 * @return path
	 */
	public String getPath() {
		return this.filepath;
	}
	
	/**
	 * Set the (data portal) path for the file
	 * @param filepath
	 */
	public void setPath(String filepath) {
		this.filepath = filepath;
	}
	
	/**
	 * Get the (data portal) filename for the file
	 * @return filename
	 */
	public String getFilename() {
		return this.filename;
	}
	
	/**
	 * Set the (data portal) filename for the file
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Get the date the file was released.
	 * @return modified_at
	 */
	public Date getReleasedAt() {
		return this.release.getRelease_date();
	}
	/**
	 * Get the email address of the admin that released the file.
	 * @return modified_by
	 */
	public String getReleasedBy() {
		return this.release.getRelease_by();
	}
	/**
	 * Get a brief description of the file.
	 * @return description
	 */
	public String getDescription() {
		return description;
	}
	/**
	 * Set the brief description of the file.
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Return the release associated with this file.
	 * @return release
	 */
	public Release getRelease(){
		return this.release;
	}
	
	/**
	 * Set the release associated with this file.
	 * @param release
	 */
	public void setRelease(Release release){
		this.release = release;
	}
	
	public boolean isHidden() { 
		return this.hidden;
	}

	/**
	 * Get the group for the file, default is null.
	 * @return group
	 */
	public String getGroup() {
		return this.group;
	}
	
	
	/**
	 * Set the group for the file.  Null will be the default group.
	 * @param group
	 */
	public void setGroup(String group) {
		this.group = group;
	}
	
	/**
	 * Return members for a project
	 * @return list of project members
	 */
	public List<ProjectMembership> getMembers() {
		return members;
	}
	
	/**
	 * Generate JsonObject for this ProjectFile
	 * @param boolean if true then include the associated release object
	 * @return JsonObject
	 */
	public JsonObject toJSON(boolean withRelease) { 
		JsonObjectBuilder builder = Json.createObjectBuilder();

		builder.add("id", this.fileid);
		builder.add("name", this.filename);
		builder.add("path", this.filepath);
		
		builder.add("type", "file");

		builder.add("description", this.description);
		if ( this.group != null ) {
			builder.add("group", this.group);			
		} else {
			builder.addNull("group");
		}
		
		builder.add("datatype", this.type);
		
		builder.add("release", withRelease ? this.release.toJSON(false) : Json.createObjectBuilder().add("id", this.release.getReleaseID()).build() );
		return builder.build();
	}
	
	/**
	 * Generate JsonObject for this ProjectFile.  This method will include associated release information.
	 * @return JsonObject
	 */
	public JsonObject toJSON() { 
		return this.toJSON(true);
	}

}
