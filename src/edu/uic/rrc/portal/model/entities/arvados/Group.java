package edu.uic.rrc.portal.model.entities.arvados;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
/**
 * This class is used to represent an arvados entity denoted by group_class
 * @author George Chlipala
 *
 */
public class Group extends ArvadosResource {
	
	static final String NAME_ATTR = "name";
	static final String GROUP_CLASS_ATTR = "group_class";
	static final String DESCRIPTION_ATTR = "description";
	static final String WRITABLE_BY_ATTR = "writable_by";
	
	private String name;
	private String groupClass;
	private String description;
	//private final List<String> writableBy = new ArrayList<String>();
private List<String> writableBy = new ArrayList<String>();
	
	public List<String> getWritableBy() {
		return writableBy;
	}

	public void setWritableBy(List<String> writableBy) {
		this.writableBy = writableBy;
	}
	
	public Group() {}

	public Group(JsonParser parser) {
		this.handleParser(parser);
	}
	/**
	 * @param key
	 * @param event
	 * @param parser
	 * @throws ParseException
	 */
	protected boolean handleAttribute(String key, Event event, JsonParser parser) throws ParseException {
		boolean processed = super.handleAttribute(key, event, parser);
		if ( processed )
			return true;
		
		if ( key.equalsIgnoreCase(NAME_ATTR) ) {
			this.name = parser.getString();
			return true;
		} else if ( key.equalsIgnoreCase(DESCRIPTION_ATTR) ) {
			this.description = parser.getString();
			return true;
		} else if ( key.equalsIgnoreCase(GROUP_CLASS_ATTR) ) {
			this.groupClass = parser.getString();
			return true;
		}
		return false;
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
			case VALUE_NULL:
				this.writableBy.add(parser.getString());
			default:
				break;
			}
		}
	}
	
	/**
	 * Get the name of the group.
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}


	/**
	 * Get the type of the group.  This does not affect behavior, but determines how the group is presented in the user interface. 
	 * For example, project indicates that the group should be displayed by Workbench and arv-mount as a project for organizing and naming objects.
	 * 
	 * @return groupClass
	 */
	public String getGroupClass() {
		return this.groupClass;
	}
	
	/**
	 * Description of the group
	 * 
	 * @return description
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * 
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}
	/**
	 * Returns true if the UUID specified (user or group) have write permission for this group.
	 * 
	 * @param uuid 
	 * @return isWritableBy
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
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
	    for (String wrString : writableBy) {
	        arrayBuilder.add(wrString);
	    }
	    builder.add("writable_by", arrayBuilder);
		if ( this.description != null )
			builder.add(DESCRIPTION_ATTR, this.description);
		else 
			builder.addNull(DESCRIPTION_ATTR);
		builder.add(NAME_ATTR, this.name);			
	}	
}
 