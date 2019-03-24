package edu.uic.rrc.portal.model.entities;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
/**
 * A JPA (hibernate) entity for the "project_release" table
 * @author SaiSravith
 *
 */
@Entity
@Table(name = "project_release")
@NamedNativeQueries({	
	@NamedNativeQuery(
		name = "activityForUser",
		query = "SELECT r.* FROM project_release r WHERE r.releaseid IN (SELECT DISTINCT f.releaseid FROM project_membership p JOIN project_files f ON(p.projectid = f.projectid) WHERE p.userid= :userid) ORDER BY r.releaseid DESC",
		resultClass = Release.class
	),
	@NamedNativeQuery(
		name = "clearOrphans",
		query = "DELETE FROM project_release WHERE releaseid NOT IN (SELECT DISTINCT f.releaseid FROM project_files f)"
	)
})
@NamedQueries({
	@NamedQuery(
			name = "deleteReleases",
			query = "DELETE FROM Release r WHERE r.releaseid = :releaseID"
	),
	@NamedQuery(
			name = "allReleases",
			query = "SELECT r FROM Release r WHERE r.releaseid IN (SELECT DISTINCT f.release FROM ProjectFiles f) ORDER BY r.release_date ASC"
	),
	@NamedQuery(
			name = "releasesForProject",
			query = "SELECT DISTINCT r FROM Release r WHERE r.releaseid IN (SELECT DISTINCT f.release FROM ProjectFiles f WHERE f.projectid = :projectid) ORDER BY r.release_date ASC"
	)	
//	@NamedQuery(
//			name = "activtyForUser",
//			query = "SELECT r.* FROM Release r WHERE r.releaseid IN (SELECT DISTINCT f.release FROM ProjectMembership p JOIN p.files f WHERE p.id.userid= :userid) ORDER BY r.releaseid DESC"
//	)
})
public class Release {

	public static DateFormat jsonDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	public static int removeReleases(EntityManager em, String[] releaseIDs) {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Query query = em.createNamedQuery("deleteReleases");
		int rowcount = 0;
		for ( String id : releaseIDs ) {
			query.setParameter("releaseID", new Long(id));
			rowcount = rowcount + query.executeUpdate();
		}
		tx.commit();
	    return rowcount;
	}
	
	public static List<Release> getAllReleases(EntityManager em) {
		TypedQuery<Release> query = em.createNamedQuery("allReleases", Release.class);
		return query.getResultList();
	}
	
	public static List<Release> getReleasesForUser(EntityManager em, String userid) {
		TypedQuery<Release> query = em.createNamedQuery("activityForUser", Release.class);
		query.setParameter("userid", userid);
		return query.getResultList();
	}
	
	public static List<Release> getReleasesForProject(EntityManager em, String projectID) {
		TypedQuery<Release> query = em.createNamedQuery("releasesForProject", Release.class);
		query.setParameter("projectid", projectID);
		return query.getResultList();
	}
	
	public static int clearEmptyReleases(EntityManager em) {
		EntityTransaction tx = em.getTransaction();
		tx.begin();
		Query query = em.createNamedQuery("clearOrphans");
		int rowcount = query.executeUpdate();
		tx.commit();
		return rowcount;
	}	
	
	Release() { }


	public Release(Date release_date, User released_by, String title, String description) {
		this.release_date = release_date;
		this.description = description;
		if ( title.length() < 1 ) {
			DateFormat format = DateFormat.getDateInstance(DateFormat.DEFAULT);
			this.title = "Release - " + format.format(this.release_date);
		} else {
			this.title = title;
		}
		this.user = released_by;
	}

	public Release(User released_by, String title, String description) {
		this(new Date(), released_by, title, description);
	}

	@Id @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name = "releaseid")
	private Long releaseid;
	
	@ManyToOne(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH})
	@JoinColumn(name="released_by", referencedColumnName="userid")
	private User user;
	
	@Column(name = "release_date", nullable = false)
	private Date release_date;

	@Column(name = "description", nullable = false)
	private String description;
	
	@Column(name = "title", nullable = false)
	private String title;
	
	@OneToMany(fetch=FetchType.LAZY, cascade={CascadeType.REFRESH, CascadeType.REMOVE})
	@JoinColumn(name="releaseid")
	private List<ProjectFiles> files;	
	
	public Long getReleaseID() {
		return releaseid;
	}
	
	public Date getRelease_date() {
		return release_date;
	}
	public void setRelease_date(Date release_date) {
		this.release_date = release_date;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}

	public String getRelease_by() {
		return this.user.getUserid();
	}
	
	public User getReleasedBy() { 
		return this.user;
	}

	public List<ProjectFiles> getFiles() { 
		return this.files;
	}
	
	public String getProjectID() { 
		if ( this.files.size() > 0 )
			return this.files.get(0).getProjectID();
		else
			return null;
	}
	
	public String toString(){
		return " releaseid: " + releaseid + " release_date: " + release_date + " description: " + description;
	}
	
	public JsonObject toJSON(boolean withFiles) { 
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("id", this.releaseid);
		
		builder.add("title", this.title);
		builder.add("date", jsonDateFormat.format(this.release_date));
		builder.add("user", this.user.toJSON());
		builder.add("description", this.description);
		
		if ( withFiles ) {
			JsonArrayBuilder jsonArray = Json.createArrayBuilder();
			for ( ProjectFiles file : this.files ) {
				jsonArray.add(file.toJSON(false));
			}
			builder.add("files", jsonArray);
		}
		
		return builder.build();
	}
	
	public JsonObject toJSON() { 
		return this.toJSON(false);
	}
}
