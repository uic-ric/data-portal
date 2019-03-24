/**
 * 
 */
package edu.uic.rrc.portal.model.entities.arvados;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**This class is used to represent a user arvados entity
 * @author George Chlipala
 *
 */
public class User extends ArvadosResource {
	static final String EMAIL_ATTR = "email";
	static final String USERNAME_ATTR = "username";
	static final String FIRST_NAME_ATTR = "first_name";
	static final String LAST_NAME_ATTR = "last_name";
	static final String IDENTITY_ATTR = "identity_url";
	static final String IS_ADMIN_ATTR = "is_admin";
	static final String PREFS_ATTR = "prefs";
	static final String DEFAULT_OWNER_ATTR = "default_owner_uuid";
	static final String IS_ACTIVE_ATTR = "is_active";
	static final String WRITABLE_BY_ATTR = "writable_by";
	
	
	private String email;
	private String username;
	private String firstName;
	private String lastName;
	private String identityURL;
	private boolean isAdmin;
	private String defaultOwner;
	private boolean isActive;
	private final Map<String,Object> prefs = new HashMap<String,Object>();
	private final List<String> writableBy = new ArrayList<String>();
	
	public User(JsonParser parser) {
		this.handleParser(parser);
	}
	
	protected boolean handleAttribute(String key, Event event, JsonParser parser) throws ParseException {
		boolean processed = super.handleAttribute(key, event, parser);
		if ( processed )
			return true;
		
		if ( key.equalsIgnoreCase(EMAIL_ATTR) ) {
			this.email = parser.getString();
		} else if ( key.equalsIgnoreCase(USERNAME_ATTR) ) {
			this.username = parser.getString();
		} else if ( key.equalsIgnoreCase(FIRST_NAME_ATTR) ) {
			this.firstName = parser.getString();
		} else if ( key.equalsIgnoreCase(LAST_NAME_ATTR) ) {
			this.lastName = parser.getString();
		} else if ( key.equalsIgnoreCase(IDENTITY_ATTR)) {
			this.identityURL = parser.getString();
		} else if ( key.equalsIgnoreCase(IS_ADMIN_ATTR) ) {
			this.isAdmin = ( event == Event.VALUE_TRUE );
		} else if ( key.equalsIgnoreCase(DEFAULT_OWNER_ATTR) ) {
			this.defaultOwner = parser.getString();
		} else if ( key.equalsIgnoreCase(IS_ACTIVE_ATTR) ) {
			this.isActive = ( event == Event.VALUE_TRUE);
		} else {
			return false;
		}
		return true;
	}
	
	@Override
	protected void handleObject(String key, JsonParser parser) throws ParseException {
		// If this is writable_by attribute do NOT process.
		if ( ! key.equalsIgnoreCase(PREFS_ATTR) ) {
			super.handleObject(key, parser);
			return;
		}
		
		String currentKey = null;
		while ( parser.hasNext() ) {
			int depth = 0;
			switch ( parser.next() ) {
			case END_OBJECT:
				if ( depth == 0 )
					return;
				depth--;
				break;
			case START_OBJECT:
				currentKey = null;
				depth++; break;
			case KEY_NAME:
				currentKey = parser.getString(); break;
			case VALUE_STRING:
				if ( currentKey != null )
					this.prefs.put(currentKey, parser.getString()); 
				break;
			case VALUE_TRUE:
				if ( currentKey != null )
					this.prefs.put(currentKey, Boolean.TRUE); 
				break;
			case VALUE_FALSE:
				if ( currentKey != null )
					this.prefs.put(currentKey, Boolean.FALSE); 
				break;
			case VALUE_NUMBER:
				if ( currentKey != null )
					this.prefs.put(currentKey, parser.getBigDecimal()); 
				break;
			default:
				break;
			}
		}
	}
	
	@Override
	protected void handleArray(String key, JsonParser parser) throws ParseException {
		// If this is writable_by attribute do NOT process.
		if ( ! key.equalsIgnoreCase(WRITABLE_BY_ATTR) ) {
			super.handleArray(key, parser);
			return;
		}
		int depth = 0;
		while ( parser.hasNext() ) {
			switch (parser.next()) {
			case START_ARRAY:
				depth++;
				break;
			case END_ARRAY:
				if ( depth == 0 ) return;
				depth--;
			case VALUE_STRING:
				this.writableBy.add(parser.getString());
			default:
				break;
			}
		}
	}
	/**
	 * 
	 * @return email
	 */
	public String getEmail() {
		return this.email;
	}
	
	/**
	 * The username used for the user’s git repositories and virtual machine logins. Usernames must start with a letter, and contain only alphanumerics. 
	 * When a new user is created, a default username is set from their e-mail address. Only administrators may change the username.
	 * @return username
	 */
	public String getUserName() {
		return this.username;
	}
	/**
	 * 
	 * @return firstName
	 */
	public String getFirstName() { 
		return this.firstName;
	}
	/**
	 * 
	 * @return lastName
	 */
	public String getLastName() { 
		return this.lastName;
	}
	/**
	 * 
	 * @return identityURL
	 */
	public String getIdentityURL() { 
		return this.identityURL;
	}
	/**
	 * 
	 * @return isAdmin
	 */
	public boolean isAdmin() { 
		return this.isAdmin;
	}
	/**
	 * 
	 * @return isActive
	 */
	public boolean isActive() { 
		return this.isActive;
	}
	/**
	 * 
	 * @return defaultOwner
	 */
	public String getDefaultOwnerUUID() { 
		return this.defaultOwner;
	}
	
	/**
	 * Returns true if the UUID specified (user or group) can modify this user	.
	 * 
	 * @param uuid 
	 * @return
	 */
	public boolean isWritableBy(String uuid) {
		return this.writableBy.contains(uuid);
	}

	/* (non-Javadoc)
	 * @see edu.uic.rrc.portal.model.entities.arvados.ArvadosResource#buildJson(javax.json.JsonObjectBuilder)
	 */
	@Override
	public void buildJson(JsonObjectBuilder builder) {
		super.buildJson(builder);
		if ( this.username != null )
			builder.add("username", this.username);
		else 
			builder.addNull("username");
			
		builder.add("firstName", this.firstName);
		builder.add("lastName", this.lastName);
		builder.add("email", this.email);
	}
	
	
}