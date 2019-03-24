package edu.uic.rrc.portal.mail;

public enum EmailType {
	FILE_RELEASE("file_release"), USER_ADD("user-add");
	
	private final String type;
	/**
	 * 
	 * @param type
	 */
	EmailType(String type) { this.type = type; }
    /**
     * 
     * @return type
     */
    public String getValue() { return type; }
}
