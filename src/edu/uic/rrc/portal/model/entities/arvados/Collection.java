/**
 * 
 */
package edu.uic.rrc.portal.model.entities.arvados;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.commons.codec.binary.Hex;



/**This class is user to represent a Collection arvados entity
 * @author George Chlipala
 *
 */
public class Collection extends ArvadosResource {
	
	static final String NAME_ATTR = "name";
	static final String DESCRIPTION_ATTR = "description";
	static final String DATA_HASH_ATTR = "portable_data_hash";
	static final String MANIFEST_ATTR = "manifest_text";
	static final String REP_DESIRED_ATTR = "replication_desired";
	static final String REP_CONFIRMED_ATTR = "replication_confirmed";
	static final String REP_CONFIRMED_AT_ATTR = "replication_confirmed_at";
	static final String PROPERTIES_ATTR = "properties";
	
	String name;
	String description;
	String portableDataHash;
	String manifestText;
	int replicationDesired;
	int replicationConfirmed;
	LocalDate replicationConfirmedAt;
	
	private List<CollectionFile> files = null;
	private Map<String, List<ManifestEntry>> fileMap = null;
	
	private Map<String, Object> properties = new HashMap<String,Object>();
	
	private boolean desciptionUpdated = false;
	private boolean nameUpdated = false;
	private boolean manifestUpdated = false;
	
	static final Pattern LOCATOR_STRIP = Pattern.compile("\\+[^\\d][^\\+]*");
	
	/**
	 * 
	 * @author George Chlipala
	 *
	 */
	public class CollectionFile {
		protected String path;
		protected List<String> locators = new ArrayList<String>(3);
		protected FileToken fileToken;

		/**
		 * 
		 * @return path
		 */
		public String getPath() {
			return this.path;
		}
		/**
		 * 
		 * @return fileTokens
		 */
		public FileToken getFileToken() {
			return this.fileToken;
		}
		/**
		 * 
		 * @return locators
		 */
		public List<String> getLocators() {
			return this.locators;
		}
		/**
		 * 
		 * @return file name
		 */
		public String getFilename() {
			return this.fileToken.name;
		}
		/**
		 * 
		 * @return size
		 */
		public long getSize() {
			return this.fileToken.size;
		}

		/**
		 * 
		 * @return Collection
		 */
		public Collection getCollection() { 
			return Collection.this;
		}
		
		public String getFullPath() { 
			return this.fileToken.fullname;
		}
	}
	
	public class ManifestEntry {
		protected String path;
		protected List<String> locators = new ArrayList<String>(3);
		protected List<FileToken> files = new ArrayList<FileToken>();
		
		ManifestEntry(String line) {
			String[] parts = line.split(" ");
			boolean isLocator = true;	
			this.path = parts[0];
			for ( int i = 1; i < (parts.length); i++ ) {
				if ( isLocator ) {
					Matcher amatch = KeepService.LOCATOR_FORMAT.matcher(parts[i]);
					isLocator = amatch.matches();
				}
				
				if ( isLocator ) {
					this.locators.add(parts[i]);
				} else {
					FileToken filetoken = new FileToken(parts[i], this.path);
					this.files.add(filetoken);
				}
			}
		}
		
		public List<FileToken> getFiles() { 
			return this.files;
		}
		
		public String getPath() { 
			return this.path;
		}
		
		public int getIndexOf(String name) {
			for ( int i = 0; i < this.files.size(); i++ ) {
				if ( this.files.get(i).name.equalsIgnoreCase(name) ) {
					return i;
				}
			}
			return -1;
		}
		
		public boolean renameFile(String oldName, String newName) {
			for ( FileToken entry : this.files ) {
				if ( entry.name.equalsIgnoreCase(oldName) ) {
					entry.name = newName;
					return true;
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuffer line = new StringBuffer(this.path);
			for ( String locator : this.locators ) {
				line.append(" ");
				line.append(locator);
			}
			for ( FileToken file : this.files ) {
				line.append(" ");
				line.append(file.toString());
			}
			return line.toString();
		}
		
		public String strippedLine() { 
			StringBuffer line = new StringBuffer(this.path);
			for ( String locator : this.locators ) {
				line.append(" ");
				line.append(LOCATOR_STRIP.matcher(locator).replaceAll(""));
			}
			for ( FileToken file : this.files ) {
				line.append(" ");
				line.append(file.toString());
			}
			return line.toString();
			
		}
	}
	
	/**
	 * 
	 * @author George Chlipala
	 *
	 */
	public class FileToken {
		protected String name;
		protected String fullname;
		protected long position;
		protected long size;
		/**
		 * 
		 * @param token
		 */
		FileToken(String token, String path) {
			String[] parts = token.split(":",3);
			this.position = Long.parseLong(parts[0]);
			this.size = Long.parseLong(parts[1]);
			// This is a kludge, but will need to change to spaces.  I am sure this a better way. - GC, 3 Nov 2016 
			if(path.length()<2)
				path = "";
			else
				path = path.substring(2) + "/";
			this.name =  parts[2].replace("\\040", " ");
			this.fullname = path + this.name;
		}
		/**
		 * 
		 * @return name
		 */
		public String getName() { 
			return this.fullname;
		}
		
		/**
		 * 
		 * @return size
		 */
		public long getSize() { 
			return this.size;
		}
		/**
		 * 
		 * @return position
		 */
		public long getPosition() {
			return this.position;
		}
		/**
		 * 
		 * @return Collection
		 */
		public Collection getCollection() { 
			return Collection.this;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("%d:%d:%s", this.position, this.size, this.name.replace(" ", "\\040"));
		}
		
	}

	/**
	 * 
	 * @param parser
	 */
	public Collection(JsonParser parser) {
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
		} else if ( key.equalsIgnoreCase(DATA_HASH_ATTR) ) {
			this.portableDataHash = parser.getString();
			return true;
		} else if ( key.equalsIgnoreCase(MANIFEST_ATTR) ) {
			this.manifestText = parser.getString();
			return true;
		} else if ( key.equalsIgnoreCase(REP_CONFIRMED_ATTR) ) {
			this.replicationConfirmed = parser.getInt();
			return true;
		} else if ( key.equalsIgnoreCase(REP_DESIRED_ATTR) ) {
			this.replicationDesired = parser.getInt();
			return true;
		} else if ( key.equalsIgnoreCase(REP_CONFIRMED_AT_ATTR) ) {
			this.replicationConfirmedAt = LocalDate.parse(parser.getString(),DATE_FORMAT);
			return true;
		}
		return false;
	}
	
	@Override
	protected void handleObject(String key, JsonParser parser) throws ParseException {
		// Only process the "properties" attribute.
		if ( key.equalsIgnoreCase(PROPERTIES_ATTR) ) {
			parseProperties(this.properties, parser);
		} else {
			super.handleObject(key, parser);
			return;
		}
	}
	
	/**
	 * Method to parse the properties of a collection.  The properties are stored as a series of nested hashes.
	 * 
	 * @param props Map to store the properties
	 * @param parser JsonParser
	 * @return Map of the properties
	 * @throws ParseException
	 */
	private static Map<String, Object> parseProperties(Map<String, Object> props, JsonParser parser) throws ParseException {
		String currentKey = null;
		while ( parser.hasNext() ) {
			switch ( parser.next() ) {
			case END_OBJECT:
				return props;
			case START_OBJECT:
				props.put(currentKey, parseProperties(new HashMap<String, Object>(), parser)); break;
			case KEY_NAME:
				currentKey = parser.getString(); break;
			case VALUE_STRING:
				if ( currentKey != null )
					props.put(currentKey, parser.getString()); 
				break;
			case VALUE_TRUE:
				if ( currentKey != null )
					props.put(currentKey, Boolean.TRUE); 
				break;
			case VALUE_FALSE:
				if ( currentKey != null )
					props.put(currentKey, Boolean.FALSE); 
				break;
			case VALUE_NUMBER:
				if ( currentKey != null )
					props.put(currentKey, parser.getBigDecimal()); 
				break;
			case END_ARRAY:
				break;
			case START_ARRAY:
				props.put(currentKey, parseProperties(new ArrayList<Object>(), parser)); break;
			default:
				break;
			}
		}
		return props;
	}
	
	/**
	 * Method to parse the properties of a collection.  The properties are stored as a series of nested hashes.
	 * 
	 * @param props Map to store the properties
	 * @param parser JsonParser
	 * @return Map of the properties
	 * @throws ParseException
	 */
	private static List<Object> parseProperties(List<Object> props, JsonParser parser) throws ParseException {
		while ( parser.hasNext() ) {
			switch ( parser.next() ) {
			case START_OBJECT:
				props.add(parseProperties(new HashMap<String, Object>(), parser)); break;
			case VALUE_STRING:
				props.add(parser.getString()); 
				break;
			case VALUE_TRUE:
				props.add(Boolean.TRUE); 
				break;
			case VALUE_FALSE:
				props.add(Boolean.FALSE); 
				break;
			case VALUE_NUMBER:
				props.add(parser.getBigDecimal()); 
				break;
			case END_ARRAY:
				return props;
			case START_ARRAY:
				props.add(parseProperties(new ArrayList<Object>(), parser)); break;
			case KEY_NAME:
			case END_OBJECT:
			default:
				// These do not make sense for any array.
				break;
			}
		}
		return props;
	}
	
	/**
	 * 
	 * @return name
	 */
	public String getName() {
		return this.name;
	}
	/**
	 * 
	 * @return description
	 */
	public String getDescription() {
		return this.description;
	}
	/**
	 * 
	 * @return
	 */
	public String getPortableDataHash() {
		return this.portableDataHash;
	}
	/**
	 * 
	 * @return manifestText
	 */
	public String getManifestText() { 
		return this.manifestText;
	}
	
	public void setName(String name) {
		this.name = name;
		this.nameUpdated = true;
	}
	
	public void setDescription(String description) {
		this.description = description;
		this.desciptionUpdated = true;
	}
	
	/**
	 * Return CollectionFile objects for all files in this collection.
	 * @return Collectionfile List
	 */
	public List<CollectionFile> getManifest() {
		if ( this.files == null ) {
			String[] lines = this.getManifestText().split("\\n");
			this.files = new ArrayList<CollectionFile>(lines.length);
			// Iterate over the lines and add each file entry to the files List
			for ( String line : lines ) {
				String[] parts = line.split(" ");
				List<String> locators = new ArrayList<String>(3);
				boolean isLocator = true;		
				for ( int i = 1; i < (parts.length); i++ ) {
					if ( isLocator ) {
						Matcher amatch = KeepService.LOCATOR_FORMAT.matcher(parts[i]);
						isLocator = amatch.matches();
					}

					if ( isLocator ) {
						locators.add(parts[i]);
					} else {
						CollectionFile collectionFile = new CollectionFile();
						collectionFile.path = parts[0];
						collectionFile.locators = locators;
						collectionFile.fileToken = new FileToken(parts[i], collectionFile.path);
						this.files.add(collectionFile);
					}
				}
			}
		}
		return this.files;
	}
	
	/**
	 * Return a Map of manifest entries keyed by folder path.
	 * @return Map of ManifestEntry objects
	 */
	public Map<String, List<ManifestEntry>> getFileMap() { 
		if ( this.fileMap == null ) {
			this.fileMap = new HashMap<String, List<ManifestEntry>>();
			String[] lines = this.getManifestText().split("\\n");
			for ( String line : lines ) {
				ManifestEntry entry = new ManifestEntry(line);
				if ( ! this.fileMap.containsKey(entry.path) ) {
					this.fileMap.put(entry.path, new ArrayList<ManifestEntry>(3));
				}
				this.fileMap.get(entry.path).add(entry);
			}		
		}
		return this.fileMap;
	}
	
	/**
	 * Check to see if a file is present
	 * 
	 * @param path directory path for file
	 * @param filename name of file
	 * @return true if the file is present
	 */
	public boolean isFilePresent(String path, String filename) {
		getFileMap();
		
		List<ManifestEntry> entries = this.fileMap.get(path);
		if ( entries != null ) {
			for ( ManifestEntry entry : entries ) {
				if ( entry.getIndexOf(filename) > -1 ) { 
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Rename a file
	 * 
	 * @param path directory path of file
	 * @param oldName old name of the file
	 * @param newName new name of the file
	 * @return true if a file was renamed
	 */
	public boolean renameFile(String path, String oldName, String newName) { 
		getFileMap();

		if ( ! isFilePresent(path, newName) ) {
			List<ManifestEntry> entries = this.fileMap.get(path);
			if ( entries != null ) {
				for ( ManifestEntry entry : entries ) {
					if ( entry.renameFile(oldName, newName) ) { 
						this.manifestUpdated = true;
						return true;
					}
				}
			}			
		}
		return false;
	}
	
/*
	// adds the released attribute from the project files map at the server side
	public JsonObject fillCheckBoxes(JsonObject root, HttpServletRequest request){
		String fileId = this.getUuid()+"/";
		Map<String,ProjectFiles> projectFiles = AdministrationServlet.getCurrentFileMap(request);
		JsonArray subDirs = root.getAsJsonArray("dirs");
		JsonArray files = root.getAsJsonArray("files");
		int count=0;
		if(files!=null){
			for(JsonElement obj : files){
				if(projectFiles.containsKey(fileId + ((JsonObject)obj).get("name").getAsString())){
					ProjectFiles file = projectFiles.get(fileId + ((JsonObject)obj).get("name").getAsString());
					((JsonObject) obj).addProperty("released", true);
					((JsonObject) obj).addProperty("description", file.getDescription());
					count++;
				}
			}
			if(count==files.size())
				root.addProperty("released", true);
		}
		
		if(subDirs!=null){
			for(JsonElement elem : subDirs){
				fillCheckBoxes(((JsonObject)elem), request);
			}
		}
		return root;
	}
*/
	/**
	 * 
	 * @return replicationDesired
	 */
	public int getReplicationDesired() {
		return this.replicationDesired;
	}
	/**
	 * 
	 * @return replicationConfirmed
	 */
	public int getReplicationConfirmed() {
		return this.replicationConfirmed;
	}
	/**
	 * 
	 * @return replicationConfirmedAt
	 */
	public LocalDate getReplicationConfirmedAt() {
		return this.replicationConfirmedAt;
	}
	
	/* (non-Javadoc)
	 * @see edu.uic.rrc.portal.model.entities.arvados.ArvadosResource#buildJson(javax.json.JsonObjectBuilder)
	 */
	@Override
	public void buildJson(JsonObjectBuilder builder) {
		super.buildJson(builder);
		builder.add(NAME_ATTR, this.name);
		if ( this.description != null )
			builder.add(DESCRIPTION_ATTR, this.description);
		else
			builder.addNull(DESCRIPTION_ATTR);
	}
	
	/**
	 * Create JSON for updating the collection in Arvados.  Currently you can only update the name, description and manifest.
	 * 
	 * @return JsonObject of updated attributes.
	 * @throws NoSuchAlgorithmException 
	 */
	public JsonObject updateJson() throws Exception {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		if ( this.nameUpdated ) 
			builder.add(NAME_ATTR, this.name);
		if ( this.desciptionUpdated ) 
			builder.add(NAME_ATTR, this.description);
		if ( this.manifestUpdated ) {
			StringBuffer newManifest = new StringBuffer();
			StringBuffer strippedManifest = new StringBuffer();
			for ( Entry<String, List<ManifestEntry>> entry : this.fileMap.entrySet() ) {
				for ( ManifestEntry line : entry.getValue() ) {
					newManifest.append(line.toString());
					newManifest.append("\n");
					strippedManifest.append(line.strippedLine());
					strippedManifest.append("\n");
				}
			}
			builder.add(MANIFEST_ATTR, newManifest.toString());
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] digest = md.digest(strippedManifest.toString().getBytes());
			builder.add(DATA_HASH_ATTR, Hex.encodeHexString(digest));
		}
		
		return builder.build();
		
	}
	
	public Object getProperty(String property) {
		return this.properties.get(property);
	}
	
	
}
