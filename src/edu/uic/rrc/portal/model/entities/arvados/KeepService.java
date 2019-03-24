/**
 * 
 */
package edu.uic.rrc.portal.model.entities.arvados;

import java.text.ParseException;
import java.util.regex.Pattern;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
/**Class to interact with the Arvados KeepServer.
 * @author George Chlipala
 *
 */
public class KeepService extends ArvadosResource {

	static final String SERVICE_HOST_ATTR = "service_host";
	static final String SERVICE_PORT_ATTR = "service_port";
	static final String SERVICE_SSL_FLAG_ATTR = "service_ssl_flag";
	public static final String SERVICE_TYPE_ATTR = "service_type";
	
	static final Pattern LOCATOR_FORMAT = Pattern.compile("^([0-9a-f]{32})\\+([0-9]+)(\\+[A-Z][-A-Za-z0-9@_]*)*$");
	static final Pattern FILE_TOKEN_FORMAT = Pattern.compile("^[0-9]+:[0-9]+:");


	String serviceHost;
	int servicePort;
	boolean serviceSSLFlag;
	String serviceType;
	
	public KeepService(JsonParser parser) {
		this.handleParser(parser);
	}
	
	protected boolean handleAttribute(String key, Event event, JsonParser parser) throws ParseException {
		boolean processed = super.handleAttribute(key, event, parser);
		if ( processed )
			return true;
		
		if ( key.equalsIgnoreCase(SERVICE_HOST_ATTR) ) {
			this.serviceHost = parser.getString();
			return true;
		} else if ( key.equalsIgnoreCase(SERVICE_PORT_ATTR) ) {
			this.servicePort = parser.getInt();
			return true;
		} else if ( key.equalsIgnoreCase(SERVICE_SSL_FLAG_ATTR) ) {
			this.serviceSSLFlag = (event == Event.VALUE_TRUE);
			return true;
		} else if ( key.equalsIgnoreCase(SERVICE_TYPE_ATTR) ) {
			this.serviceType = parser.getString();
			return true;
		}
		return false;
	}

	public String getServiceHost() {
		return this.serviceHost;
	}
	
	public int getServicePort() {
		return this.servicePort;
	}
	
	public boolean getServiceSSLFlag() {
		return this.serviceSSLFlag;
	}
	
	public String getServiceType() { 
		return this.serviceType;
	}
}
