package edu.uic.rrc.portal.model.entities;

import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.MapsId;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Query;
import javax.persistence.Table;
import javax.persistence.TypedQuery;

import edu.uic.rrc.portal.model.entities.pk.ProjectMembershipID;
/**
 * A JPA (hibernate) entity for the "project_membership" table
 * @author SaiSravith
 *
 */
@Entity
@Table(name = "project_membership")
@NamedQueries({
	@NamedQuery(
			name = "allMembers",
			query = "SELECT p FROM ProjectMembership p WHERE p.id.projectid = :projectid ORDER BY p.owner DESC, p.id.userid ASC"
	),
	@NamedQuery(
			name = "owners",
			query = "SELECT p FROM ProjectMembership p WHERE p.id.projectid = :projectid AND p.owner = 1 ORDER BY p.id.userid ASC"
	),
	@NamedQuery(
		name = "projectEmails",
		query = "SELECT p.id.userid FROM ProjectMembership p WHERE p.id.projectid = :projectid ORDER BY p.id.userid ASC"
	),
	@NamedQuery(
			name = "projectList",
			query = "SELECT DISTINCT p.id.projectid FROM ProjectMembership p ORDER BY p.id.projectid"
	),
//	@NamedQuery(
//			name = "recentProjects",
//			query = "SELECT DISTINCT p.id.projectid FROM ProjectMembership p JOIN p.files f WHERE p.id.userid= :userid GROUP BY p.id.projectid ORDER BY max(f.release.releaseid) DESC"
//	)
})
@NamedNativeQueries({
	@NamedNativeQuery(
			name = "deleteMember",
			query = "DELETE FROM project_membership WHERE projectid= :projectid AND userid= :userid"
	)	
})
public class ProjectMembership {
	
	public static List<String> getEmailsForProject(EntityManager em, String projectID) {
		TypedQuery<String> query = em.createNamedQuery("projectEmails", String.class);
		query.setParameter("projectid", projectID);
		return query.getResultList();
	}
	
	public static List<String> getProjectList(EntityManager em) {
		TypedQuery<String> query = em.createNamedQuery("projectList", String.class);
		return query.getResultList();
	}
	
	public static List<ProjectMembership> getProjectMembers(EntityManager em, String projectid) {
		TypedQuery<ProjectMembership> query = em.createNamedQuery("allMembers", ProjectMembership.class);
		query.setParameter("projectid", projectid);
		return query.getResultList();
	}
	
	public static List<ProjectMembership> getProjectOwners(EntityManager em, String projectid) {
		TypedQuery<ProjectMembership> query = em.createNamedQuery("owners", ProjectMembership.class);
		query.setParameter("projectid", projectid);
		return query.getResultList();
	}
	
	public static List<String> getRecentProjects(EntityManager em, String userid, int limit) {
		Query query = em.createNativeQuery("SELECT p.projectid FROM project_membership p JOIN project_files f ON(p.projectid = f.projectid) WHERE p.userid = :userid GROUP BY p.projectid ORDER BY MAX(f.releaseid) DESC");		
	//	TypedQuery<String> query = em.createNamedQuery("recentProjects", String.class);
		query.setParameter("userid", userid);
		query.setMaxResults(limit);
		return (List<String>) query.getResultList();
	}
	
	public static List<String> getRecentProjects(EntityManager em, int limit) {
		Query query = em.createNativeQuery("SELECT p.projectid FROM project_membership p JOIN project_files f ON(p.projectid = f.projectid) GROUP BY p.projectid ORDER BY MAX(f.releaseid) DESC");		
	//	TypedQuery<String> query = em.createNamedQuery("recentProjects", String.class);
		query.setMaxResults(limit);
		return (List<String>) query.getResultList();
	}

	public static List<String> getRecentProjects(EntityManager em, String userid) { 
		return getRecentProjects(em, userid, 10);
	}

	
	/**
	 * Delete an user from a project
	 * @param em
	 * @param projectid
	 * @param userid
	 */
	public static void removeUserFromProject(EntityManager em, String projectid, String userid) {
		Query query = em.createNamedQuery("deleteMember");
        query.setParameter("userid", userid);
        query.setParameter("projectid", projectid);
        query.executeUpdate();
	}
	

	@EmbeddedId
	private ProjectMembershipID id;

	@Column(name = "owner", nullable = false)	  
	private int owner;

	@MapsId("userid")
	@JoinColumn(name = "userid")		
	@OneToOne(fetch = FetchType.LAZY, optional = true, cascade={CascadeType.REFRESH}) 
	private User user;

	@JoinColumn(name="projectid")
	@ManyToMany(fetch = FetchType.LAZY, cascade={CascadeType.REFRESH})
	private List<ProjectFiles> files;
	
	public ProjectMembership() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 
	 * @param projectid
	 * @param userid
	 * @param owner
	 */
	@Deprecated
	public ProjectMembership(String projectid, String userid, int owner) {
		this.id = new ProjectMembershipID();
		this.id.setProjectid(projectid);
		this.id.setUserid(userid);
		this.owner = owner;
	}
	
	public ProjectMembership(String projectid, User user, int owner) {
		this.id = new ProjectMembershipID();
		this.id.setProjectid(projectid);
		this.id.setUserid(user.getUserid());
		this.owner = owner;
		this.user = user;
	}

	/**
	 * 
	 * @return id
	 */
	public ProjectMembershipID getId() {
		return id;
	}
	/**
	 * 
	 * @return owner
	 */
	public int getOwner() {
		return owner;
	}
	/**
	 * 
	 * @param owner
	 */
	public void setOwner(int owner) {
		this.owner = owner;
	}
	
	public boolean isOwner() { 
		return this.owner == 1;
	}
	
	public User getUser() {
		return user;
	}
	
	public JsonObject toJSON() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("project_id", this.id.getProjectid());
		builder.add("email", this.user.getUserid());
		String name = this.user.getName();
		if ( name == null ) {
			builder.addNull("name");
		} else {
			builder.add("name", name);			
		}
		
		name = this.user.getAffiliation();
		if ( name == null ) {
			builder.addNull("affiliation");
		} else {
			builder.add("affiliation", name);			
		}
		builder.add("owner", this.isOwner());
		
		return builder.build();
	}
}
