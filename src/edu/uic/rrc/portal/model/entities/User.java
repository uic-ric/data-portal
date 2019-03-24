package edu.uic.rrc.portal.model.entities;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Random;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

/**
 * A JPA (hibernate) entity for the "users" table
 * @author SaiSravith
 *
 */
@Entity
@Table(name = "users")
@NamedQueries({
	@NamedQuery(
		name = "allUsers",
		query = "SELECT u FROM User u WHERE u.userid IN (SELECT DISTINCT p.id.userid FROM ProjectMembership p)"
	),
	@NamedQuery(
		name = "adminUsers",
		query = "SELECT u FROM User u WHERE u.role = 'admin'"
	),
	@NamedQuery(
		name = "findUsers",
		query = "SELECT u from User u WHERE u.userid LIKE :query OR u.name LIKE :query OR u.affiliation LIKE :query ORDER BY u.userid"
	)
})
public class User {
	
	public static List<User> getAllUsers(EntityManager em) {
		TypedQuery<User> query = em.createNamedQuery("allUsers", User.class);
		return query.getResultList();
	}

	public static List<User> getAdmins(EntityManager em) {
		TypedQuery<User> query = em.createNamedQuery("adminUsers", User.class);
		return query.getResultList();
	}
	
	public static List<User> findUsers(EntityManager em, String queryString, String orderBy, boolean orderAsc, int offset, int pagesize) {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> cq = cb.createQuery(User.class);
		Root<User> u = cq.from(User.class);
		ParameterExpression<String> param = cb.parameter(String.class);
		cq.select(u).where(cb.or(cb.like(u.get("userid"), param), cb.like(u.get("name"), param), cb.like(u.get("affiliation"), param)));
		if ( orderAsc ) {
			cq.orderBy(cb.asc(u.get(orderBy)));
		} else {
			cq.orderBy(cb.desc(u.get(orderBy)));
		}
		TypedQuery<User> query = em.createQuery(cq);
		query.setParameter(param, queryString);
		query.setFirstResult(offset);
		query.setMaxResults(pagesize);
		return query.getResultList();
	}
	
	public static List<User> findUsers(EntityManager em, String queryString) {
		TypedQuery<User> query = em.createNamedQuery("findUsers", User.class);
		query.setParameter("query", "%" + queryString + "%");
		return query.getResultList();
	}
	
	/**
	 * Enum to hold roles for users
	 * @author George Chlipala
	 */
	public enum Role { 
		ADMIN("admin"),
		CUSTOMER("customer");

		private String text;

		Role(String text) {
			this.text = text;
		}

		public String getText() {
			return this.text;
		}
	}

	// ------------------------
	// PRIVATE FIELDS
	// ------------------------

	// The user's email
	@Id
	@Column(name = "userid")
	private String userid;

	// The user's name

	@Column(name = "name")
	private String name;

	@Column(name = "pass")
	private String pass;

	@Column(name = "affiliation")
	private String affiliation;

	@Column(name = "role", nullable = false)
	private String role;
	
	@Column(name = "token")
	private String token;
	
	@ElementCollection
	@CollectionTable(name="ssh_keys", joinColumns={@JoinColumn(name="userid")})
	private List<SSHKey> sshKeys;
	
	// ------------------------
	// PUBLIC METHODS
	// ------------------------

	public User() { }

	/**
	 * 
	 * @param userid
	 */
	public User(String userid) { 
		this.userid = userid;
		this.role = Role.CUSTOMER.text;
	}

	/**
	 * 
	 * @param userid
	 * @param role
	 */
	public User(String userid, Role role) {
		this.userid = userid;
		this.role = role.text;
	}

	/**
	 * 
	 * @param userid
	 * @param name
	 * @param role
	 * @param affiliation
	 */
	public User(String userid, String name, Role role, String affiliation) {
		this.userid = userid;
		this.name = name;
		this.role = role.text;
		this.affiliation = affiliation;
	}
	/**
	 * 
	 * @param userid
	 * @param name
	 * @param affiliation
	 */
	public User(String userid, String name, String affiliation) {
		this.userid = userid;
		this.name = name;
		this.role = Role.CUSTOMER.text;
		this.affiliation = affiliation;
	}

	// Getter and setter methods

	/**
	 * 
	 * @return userid
	 */
	public String getUserid() {
		return userid;
	}
	/**
	 * 
	 * @param value
	 */
	public void setUserid(String value) {
		this.userid = value;
	}
	/**
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param value
	 */
	public void setName(String value) {
		this.name = value;
	}
	/**
	 * 
	 * @return affiliation
	 */
	public String getAffiliation() {
		return affiliation;
	}
	/**
	 * 
	 * @param affiliation
	 */
	public void setAffiliation(String affiliation) {
		this.affiliation = affiliation;
	}
	/**
	 * 
	 * @return pass
	 */
	public String getPass() {
		return pass;
	}
	
	public String getToken() { 
		return this.token;
	}
	
	public List<SSHKey> getSSHKeys() { 
		return this.sshKeys;
	}
	
	public void addSSHKey(String key) throws NoSuchAlgorithmException {
		this.sshKeys.add(new SSHKey(key.trim()));
	}
	
	public void removeKey(String hash) {
		for ( int i = 0; i < this.sshKeys.size(); i++ ) {
			SSHKey key = this.sshKeys.get(i);
			if ( key.equals(hash) ) {
				this.sshKeys.remove(i);
				break;
			}
		}
	}
	
	/**
	 * 
	 * @param pass
	 */
	public void setPass(String pass) {
		this.pass = pass;
	}
	
	public void resetPassword() {
        Random random = new Random(); 
        // Create a StringBuffer to store the result 
        StringBuffer r = new StringBuffer(20); 
  
        for (int i = 0; i < 20; i++) { 
            // take a random value between 97 and 122 
            int nextRandomChar = 97 + (int)(random.nextFloat() * (122 - 97 + 1)); 
            // append a character at the end of string buffer 
            r.append((char)nextRandomChar); 
        } 
        this.pass = r.toString();
	}
	/**
	 * 
	 * @return role
	 */
	public String getRole() {
		return role;
	}
	/**
	 * 
	 * @param role
	 */
	public void setRole(String role) {
		this.role = role;
	}
	
	
	public void setToken(String token) {
		this.token = token;
	}
		
	/**
	 * String representation of user fields
	 */
	public String toString(){
		return "userid: "+ userid+" name : "+name+" affiliation: "+affiliation+" role: "+role;
	}
	
	public static String byteArrayToHex(byte[] a) {
		StringBuilder sb = new StringBuilder(a.length * 2);
		for ( byte b: a ) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
	
	public boolean checkPassword(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(password.getBytes());
		return this.getPass().equalsIgnoreCase(byteArrayToHex(digest));
	}
	
	public void setNewPassword(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		byte[] digest = md.digest(password.getBytes());
		this.pass = byteArrayToHex(digest);
	}
	
	@Transient
	public JsonObject toJSON() {
		JsonObjectBuilder json = Json.createObjectBuilder();
		json.add("id", this.userid);
		if ( this.name != null ) {
			json.add("name", this.name);
		} else {
			json.addNull("name");
		}
		if ( this.affiliation != null ) {
			json.add("affiliation", this.affiliation);
		} else {
			json.addNull("affiliation");
		}
		return json.build();
	}

}