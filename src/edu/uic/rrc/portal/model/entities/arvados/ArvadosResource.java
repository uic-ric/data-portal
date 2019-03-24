/**
 * 
 */
package edu.uic.rrc.portal.model.entities.arvados;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**A generic arvados resource base class that has fields a typical Arvados Resource should have
 * @author George Chlipala
 *@see Collection
 *@see Filter
 *@see Group
 *@see Job
 *@see KeepService
 *@see User
 */
public abstract class ArvadosResource {

	//final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");

	final static DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS'Z'", Locale.ENGLISH);
	static final String UUID_ATTR = "uuid";
	static final String HREF_ATTR = "href";
	static final String KIND_ATTR = "kind";
	static final String ETAG_ATTR = "etag";
	static final String SELF_LINK_ATTR = "self_link";
	static final String OWNER_ATTR = "owner_uuid";
	static final String CREATED_AT_ATTR = "created_at";
	static final String MODIFIED_BY_CLIENT_ATTR = "modified_by_client_uuid";
	static final String MODIFIED_BY_USER_ATTR = "modified_by_user_uuid";
	static final String MODIFIED_AT_ATTR = "modified_at";

	String uuid;
	String href;
	String kind;
	String etag;
	String selfLink;
	String ownerUUID;
	LocalDateTime createdAt;
	String modifiedByClientUUID;
	String modifiedByUserUUID;
	LocalDateTime modifiedAt;
	Date lastRetrieved = new Date();
	

	/**
	 * 
	 * @param key
	 * @param event
	 * @param parser
	 * @return
	 * @throws ParseException
	 */
	protected boolean handleAttribute(String key, Event event, JsonParser parser) throws ParseException {
		if ( key.equals(UUID_ATTR) ) {
			this.uuid = parser.getString();
			return true;
		} else if ( key.equals(HREF_ATTR) ) {
			this.href = parser.getString();
			return true;
		} else if ( key.equals(KIND_ATTR)) {
			this.kind = parser.getString();
			return true;
		} else if ( key.equals(ETAG_ATTR) ) {
			this.etag = parser.getString();
			return true;
		} else if ( key.equals(SELF_LINK_ATTR) ) {
			this.selfLink = parser.getString();
			return true;
		} else if ( key.equals(OWNER_ATTR) ) {
			this.ownerUUID = parser.getString();
			return true;
		} else if ( key.equals(CREATED_AT_ATTR) ) {
			this.createdAt = LocalDateTime.parse(parser.getString(),DATE_FORMAT);
			return true;
		} else if ( key.equals(MODIFIED_BY_CLIENT_ATTR) ) {
			this.modifiedByClientUUID = parser.getString();
			return true;
		} else if ( key.equals(MODIFIED_BY_USER_ATTR) ) {
			this.modifiedByUserUUID = parser.getString();
			return true;
		} else if ( key.equals(MODIFIED_AT_ATTR) ) {
			this.modifiedAt = LocalDateTime.parse(parser.getString(),DATE_FORMAT);
			return true;
		}
		return false;
	}

	/**
	 * Method to handle JSON arrays.  Should be overwritten by classes that have array attributes.
	 * 
	 * @param key
	 * @param parser
	 * @throws ParseException
	 */
	protected void handleArray(String key, JsonParser parser) throws ParseException {
		int depth = 0;
		while (parser.hasNext() ) {
			switch (parser.next()) {
			case START_ARRAY:
				depth++;
				break;
			case END_ARRAY:
				if ( depth == 0 ) return;
				depth--;
			default:
				break;
			}
		}
	}
	/**
	 * 
	 * @param key
	 * @param parser
	 * @throws ParseException
	 */
	protected void handleObject(String key, JsonParser parser) throws ParseException {
		int depth = 0;
		while ( parser.hasNext() ) {
			switch ( parser.next() ) {
			case END_OBJECT:
				if ( depth == 0 )
					return;
				depth--;
				break;
			case START_OBJECT:
				depth++;
			default:
				break;
			}
		}
	}

	/**
	 * 
	 * @param parser
	 */
	protected void handleParser(JsonParser parser) {
		String currentKey = null;
		while ( parser.hasNext() ) {
			Event event = parser.next();
			try {
				switch (event) {
				case START_OBJECT:
					this.handleObject(currentKey, parser); break;
				case END_OBJECT:
					return;
				case KEY_NAME:
					currentKey = parser.getString(); break;
				case VALUE_NULL:
				case VALUE_STRING:
				case VALUE_NUMBER:
				case VALUE_FALSE:
				case VALUE_TRUE:
					this.handleAttribute(currentKey, event, parser);
					break;
				case END_ARRAY:
					break;
				case START_ARRAY:
					this.handleArray(currentKey, parser);
					break;
				}
			} catch (Exception e) {
				// TODO do something with the exception.
			}
		}
	}

	/**
	 * 
	 * @return modifiedAt
	 */
	public LocalDateTime getModified() { 
		return this.modifiedAt;
	}

	/**
	 * 
	 * @return uuid
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * 
	 * @param uuid
	 */
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * 
	 * @return href
	 */
	public String getHref() {
		return href;
	}

	/**
	 * 
	 * @param href
	 */
	public void setHref(String href) {
		this.href = href;
	}

	/**
	 * 
	 * @return etag
	 */
	public String getEtag() {
		return etag;
	}

	/**
	 * 
	 * @param etag
	 */
	public void setEtag(String etag) {
		this.etag = etag;
	}

	/**
	 * 
	 * @return createdAt
	 */
	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	/**
	 * 
	 * @param createdAt
	 */
	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	/**
	 * 
	 * @param createdAt
	 */
	public void setCreatedAt(String createdAt) throws ParseException {
		this.createdAt = LocalDateTime.parse(createdAt,DATE_FORMAT);
	}

	/**
	 * 
	 * @return modifiedAt
	 */
	public LocalDateTime getModifiedAt() {
		return modifiedAt;
	}

	/**
	 * 
	 * @param modifiedAt
	 */
	public void setModifiedAt(LocalDateTime modifiedAt) {
		this.modifiedAt = modifiedAt;
	}
	
	/**
	 * 
	 * @param modifiedAt
	 */
	public void setModifiedAt(String modifiedAt) throws ParseException {
		this.modifiedAt = LocalDateTime.parse(modifiedAt,DATE_FORMAT);
	}

	/**
	 * 
	 * @return kind
	 */
	public String getKind() {
		return this.kind;
	}

	/**
	 * 
	 * @param kind
	 */
	public void setKind(String kind) {
		this.kind = kind;
	}

	/**
	 * 
	 * @return selfLink
	 */
	public String getSelfLink() { 
		return this.selfLink;
	}
	/**
	 * 
	 * @param selfLink
	 */
	public void setSelfLink(String selfLink) {
		this.selfLink = selfLink;
	}
	/**
	 * 
	 * @return ownerUUID
	 */
	public String getOwnerUUID() { 
		return this.ownerUUID;
	}

	/**
	 * 
	 * @param ownerUUID
	 */
	public void setOwnerUUID(String ownerUUID) {
		this.ownerUUID = ownerUUID;
	}

	/**
	 * 
	 * @return modifiedByClientUUID
	 */
	public String getModifiedByClientUUID() { 
		return this.modifiedByClientUUID;
	}

	/**
	 * 
	 * @param modifiedByClientUUID
	 */
	public void setModifiedByClientUUID(String modifiedByClientUUID) {
		this.modifiedByClientUUID = modifiedByClientUUID;
	}

	/**
	 * 
	 * @return modifiedByUserUUID
	 */
	public String getModifiedByUserUUID() { 
		return this.modifiedByUserUUID;
	}
	/**
	 * 
	 * @param modifiedByUserUUID
	 */
	public void setModifiedByUserUUID(String modifiedByUserUUID) {
		this.modifiedByUserUUID = modifiedByUserUUID;
	}

	/**
	 * 
	 * @return lastRetrieved
	 */
	public Date getRetrieved() {
		return lastRetrieved;
	}

	public JsonObject toJson() { 
		JsonObjectBuilder builder = Json.createObjectBuilder();
		this.buildJson(builder);
		return builder.build();
	}
	
	public void buildJson(JsonObjectBuilder builder) {
		builder.add(UUID_ATTR, this.uuid);
		if(this.createdAt !=null)
			builder.add(CREATED_AT_ATTR, DATE_FORMAT.format(this.createdAt));
		if(this.modifiedAt != null)
			builder.add(MODIFIED_AT_ATTR, DATE_FORMAT.format(this.modifiedAt));
	}

	public JsonObject updateJson() throws Exception {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		return builder.build();
	}
} 
