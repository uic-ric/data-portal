package edu.uic.rrc.portal.model.entities;
/**
 * Enum to hold the file types that are released.
 * @author SaiSravith
 *
 */
public enum FileTypes {
	RAW("raw"), REPORT("report"),RESULT("result"); 
	
	private final String type;
	/**
	 * 
	 * @param type
	 */
    FileTypes(String type) { this.type = type; }
    /**
     * 
     * @return type
     */
    public String getValue() { return type; }

}
