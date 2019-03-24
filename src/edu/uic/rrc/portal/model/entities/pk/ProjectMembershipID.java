package edu.uic.rrc.portal.model.entities.pk;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

/**
 * Primary Key for ProjectMembership entity class
 * @author SaiSravith
 *
 */
@Embeddable
public class ProjectMembershipID implements Serializable { 
	/**
	 * 
	 */
	private static final long serialVersionUID = -3918014679797881377L;

    @Column(name="projectid", nullable = false)
    private String projectid;

    @Column(name="userid", nullable = false)
    private String userid;

    public ProjectMembershipID() { 
    	
    }
    
    public ProjectMembershipID(String projectid, String userid) {
    	this();
    	this.projectid = projectid;
    	this.userid = userid;
    }
    
	public String getProjectid() {
		return projectid;
	}

	public void setProjectid(String projectid) {
		this.projectid = projectid;
	}

	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}

	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof ProjectMembershipID ) {
			ProjectMembershipID pk = (ProjectMembershipID)obj;
			return ( pk.projectid.equalsIgnoreCase(this.projectid) && pk.userid.equalsIgnoreCase(this.userid) );
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return this.projectid.concat(this.userid).hashCode();
	}

	
}