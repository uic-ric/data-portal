/**
 * 
 */
package edu.uic.rrc.portal.model.entities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;

//import org.apache.tomcat.util.codec.binary.Base64;

/**
 * @author George Chlipala
 *
 */
@Embeddable
public class SSHKey {

	@Column(name="md5_hash", unique=true)
	private String sshMD5Hash;
	
	@Column(name="sha256_hash", unique=true)
	private String sshSHA255Hash;

	@Column(name="ssh_key")
	private String sshKey;
	
	@Column(name="date_added")
	private Date dateAdded = new Date();
	
	/**
	 * 
	 */
	public SSHKey() { }
	
	public SSHKey(String key) throws NoSuchAlgorithmException {
		// Code to calculate the SSH key hash
		key = key.replaceAll("\n", "").replaceAll("\r", "");
		String[] pubkey = key.split("\\s+", 3);
		
		byte[] keydata = org.apache.commons.codec.binary.Base64.decodeBase64(pubkey[1]);
		
		// Create MD5 hash and store in field sshMD5Hash
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(keydata);
		byte[] digest = md.digest();
		
		StringBuffer md5 = new StringBuffer(String.format("%02x", digest[0]));
		for ( int b = 1; b < digest.length; b++ ) {
			md5.append(String.format(":%02x", digest[b]));		
		}
		this.sshMD5Hash = md5.toString();
		
		// Create SHA256 hash and store in field sshSHA256Hash
		md = MessageDigest.getInstance("SHA-256");
		md.update(keydata);
		
		String shaHash = org.apache.commons.codec.binary.Base64.encodeBase64String(md.digest());
		shaHash = shaHash.replaceFirst("=+$", "");
		this.sshSHA255Hash = shaHash;
		
		// Set the SSH public key for the user
		this.sshKey = key;		
	}
	
	public String getKey() {
		return this.sshKey;
	}
	
	public String getMD5Hash() { 
		return this.sshMD5Hash;
	}
	
	public String getSHA256Hash() {
		return this.sshSHA255Hash;
	}
	
	public Date getDateAdded() {
		return this.dateAdded;
	}
	
	public String getInfo() {
		String[] parts = this.sshKey.split("\\s+", 3);
		if ( parts.length == 3 ) {
			return parts[2];
		} else {
			return "";
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if ( obj instanceof SSHKey ) {
			SSHKey aKey = (SSHKey) obj;
			if ( aKey.sshSHA255Hash != null ) {
				return aKey.sshSHA255Hash.equalsIgnoreCase(this.sshSHA255Hash);
			} else if ( aKey.sshMD5Hash != null ) {
				return aKey.sshMD5Hash.equalsIgnoreCase(this.sshMD5Hash);
			} else {
				return aKey.sshKey.equalsIgnoreCase(this.sshKey);
			}
		} else if ( obj instanceof String ) {
			// Assumes string is hash then the key itself.
			String aString = (String) obj;
			if ( aString.equalsIgnoreCase(this.sshSHA255Hash) ) {
				return true;
			} else if ( aString.equalsIgnoreCase(this.sshMD5Hash) ) {
				return true;
			} 
			return aString.equalsIgnoreCase(this.sshKey);
		}
		return super.equals(obj);
	}
}
