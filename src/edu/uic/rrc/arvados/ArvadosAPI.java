package edu.uic.rrc.arvados;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.log4j.Logger;

import edu.uic.rrc.portal.model.entities.arvados.ArvadosResource;
import edu.uic.rrc.portal.model.entities.arvados.Collection;
import edu.uic.rrc.portal.model.entities.arvados.Filter;
import edu.uic.rrc.portal.model.entities.arvados.Group;
import edu.uic.rrc.portal.model.entities.arvados.KeepService;
import edu.uic.rrc.portal.model.entities.arvados.User;
import edu.uic.rrc.portal.model.entities.arvados.Filter.Operator;

/**
 * This class provides a java SDK interface to Arvados API server.
 *
 * Please refer to http://doc.arvados.org/api/ to learn about the
 *  various resources and methods exposed by the API server.
 *  
 *  Derived from code written by radhika
 *
 * @author George Chlipala
 */
public class ArvadosAPI {
	static {
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
	}

	private static final String GROUPS_PATH = "groups";
	private static final String COLLECTIONS_PATH = "collections";
//	private static final String JOBS_PATH = "jobs";
	private static final String KEEP_SERVICES_PATH = "keep_services";
	private static final String USERS_PATH = "users";
	private static final String LINKS_PATH = "links";
//	private static final String API_CLIENT_AUTHORIZATION_PATH = "api_client_authorizations";
	
	private static final String ITEMS_ATTR = "items";
	
	private static final String FILTERS_PARAM = "filters";
	private static final String ORDER_PARAM = "order";
	private static final String LIMIT_PARAM = "limit";
	private static final String UUID_PARAM = "uuid";
	private static final String OFFSET_PARAM = "offset";
	
	private static final Pattern RE_GROUP = Pattern.compile("^[a-z0-9]{5}\\-j7d0g\\-[a-z0-9]{15}$");
	private static final Pattern RE_COLLECTION = Pattern.compile("^[a-z0-9]{5}\\-4zz18\\-[a-z0-9]{15}$");
	private static final Pattern RE_USER = Pattern.compile("^[a-z0-9]{5}\\-tpzed\\-[a-z0-9]{15}$");
	
	private CloseableHttpClient http;

	private String arvadosApiToken;
	private String arvadosApiHost;
	private boolean arvadosApiHostInsecure;

	private String arvadosRootUrl;

	private static final Logger logger = Logger.getLogger(ArvadosAPI.class);

	// Get it once and reuse on the call requests
	String apiName = null;
	String apiVersion = null;
	
	public static boolean isGroupUUID(String uuid) {
		return RE_GROUP.matcher(uuid).matches();
	}
	
	public static boolean isCollectionUUID(String uuid) {
		return RE_COLLECTION.matcher(uuid).matches();
	}
	
	public static boolean isUserUUID(String uuid) {
		return RE_USER.matcher(uuid).matches();
	}

	/*
	 * @param apiName
	 * @param apiVersion
	 * @throws Exception
	 */
	public ArvadosAPI (String apiName, String apiVersion) throws Exception {
		this (apiName, apiVersion, null, null, null);
	}

	/*
	 * @param apiName
	 * @param apiVersion
	 * @param token
	 * @param host
	 * @param hostInsecure
	 * @throws Exception
	 */
	public ArvadosAPI (String apiName, String apiVersion, String token,
			String host, String hostInsecure) throws Exception {
		
		
		
		this.apiName = apiName;
		this.apiVersion = apiVersion;

		// Read needed environmental variables if they are not passed
		if (token != null) {
			arvadosApiToken = token;
		} else {
			arvadosApiToken = System.getenv().get("ARVADOS_API_TOKEN");
			if (arvadosApiToken == null) {
				throw new Exception("Missing environment variable: ARVADOS_API_TOKEN");
			}
		}

		if (host != null) {
			arvadosApiHost = host;
		} else {
			host=  System.getenv().get("ARVADOS_API_HOST");
			arvadosApiHost = System.getenv().get("ARVADOS_API_HOST");
			if (arvadosApiHost == null) {
				throw new Exception("Missing environment variable: ARVADOS_API_HOST");
			}
		}
		arvadosRootUrl = "https://" + arvadosApiHost;
		arvadosRootUrl += (arvadosApiHost.endsWith("/")) ? "" : "/";

		if (hostInsecure != null) {
			arvadosApiHostInsecure = Boolean.valueOf(hostInsecure);
		} else {
			arvadosApiHostInsecure =
					"true".equals(System.getenv().get("ARVADOS_API_HOST_INSECURE")) ? true : false;
		}

		if (arvadosApiHostInsecure) {
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}});
			this.http = HttpClients.custom().setSSLContext(builder.build()).build();
		} else {
			this.http = HttpClients.createSystem();
		}
	}
	public ArvadosAPI (String apiName, String apiVersion, String token,
			String host, String hostInsecure, boolean isAdmin) throws Exception {
		
		
		
		this.apiName = apiName;
		this.apiVersion = apiVersion;

		// Read needed environmental variables if they are not passed
			arvadosApiToken = System.getenv().get("ARVADOS_API_TOKEN");
			if (arvadosApiToken == null) {
				throw new Exception("Missing environment variable: ARVADOS_API_TOKEN");
			}
		

		if (host != null) {
			arvadosApiHost = host;
		} else {
			host=  System.getenv().get("ARVADOS_API_HOST");
			arvadosApiHost = System.getenv().get("ARVADOS_API_HOST");
			if (arvadosApiHost == null) {
				throw new Exception("Missing environment variable: ARVADOS_API_HOST");
			}
		}
		arvadosRootUrl = "https://" + arvadosApiHost;
		arvadosRootUrl += (arvadosApiHost.endsWith("/")) ? "" : "/";

		if (hostInsecure != null) {
			arvadosApiHostInsecure = Boolean.valueOf(hostInsecure);
		} else {
			arvadosApiHostInsecure =
					"true".equals(System.getenv().get("ARVADOS_API_HOST_INSECURE")) ? true : false;
		}

		if (arvadosApiHostInsecure) {
			SSLContextBuilder builder = SSLContexts.custom();
			builder.loadTrustMaterial(new TrustStrategy() {
				@Override
				public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					return true;
				}});
			this.http = HttpClients.custom().setSSLContext(builder.build()).build();
		} else {
			this.http = HttpClients.createSystem();
		}
	}
	
	
	/**
	 * Method to create a URIBuilder for Arvados API calls.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @return URIBuilder for the relative path
	 * @throws URISyntaxException 
	 */
	private URIBuilder startURI(String path) throws URISyntaxException {
		if ( path.startsWith("/"))
			path = path.substring(1);

		return new URIBuilder(this.arvadosRootUrl + this.apiName + "/" + this.apiVersion + "/" + path);
	}

	/**
	 * Method to create a URI using the path and parameters provided.  Will encode parameters as query string portion of the URI.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @param parameters request parameters, i.e. form data.
	 * @return URI for the HTTP request with parameters encoded as a query string.
	 * @throws URISyntaxException 
	 */
	private URI encodeURI(String path, Map<String,Object> parameters) throws URISyntaxException {
		URIBuilder builder = this.startURI(path);
		if ( parameters != null ) {
			for ( Entry<String,Object> entry : parameters.entrySet() ) {
				if ( entry.getValue() instanceof List ) {
					for ( Object item : (List)entry.getValue() ) {
						builder.addParameter(entry.getKey(), item.toString());
					}
				} else {
					builder.addParameter(entry.getKey(), entry.getValue().toString());
				}
			}
		}
		return builder.build();
	}


	/**
	 * Add Authorization header to HTTP request (Arvados API token).
	 * 
	 * @param request HTTP request object
	 */
	private void addAuth(HttpRequestBase request) {
		request.addHeader("Authorization", "OAuth2 " + this.arvadosApiToken);
	}


	/**
	 * Perform a HTTP GET.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @param parameters request parameters, i.e. form data.
	 * @return InputStream of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws ArvadosException 
	 */
	private CloseableHttpResponse doGet(String path, Map<String,Object> parameters) throws IOException, URISyntaxException, ArvadosException {
		HttpGet request = new HttpGet(this.encodeURI(path, parameters));
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
			response.close();
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		return response;
	}

	private CloseableHttpResponse doGetList(String object, String filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		URIBuilder builder = this.startURI(object);
		if ( filters != null ) {
			builder.addParameter(FILTERS_PARAM, filters);
		}
		if ( order != null )
			builder.addParameter(ORDER_PARAM, order);

		builder.addParameter(OFFSET_PARAM, Integer.toString(offset));
		builder.addParameter(LIMIT_PARAM, Integer.toString(limit));

		HttpGet request = new HttpGet(builder.build());
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
			response.close();
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		return response;
	
	}
	
	/**
	 * Perform a HTTP GET for a list path.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @return InputStream of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws ArvadosException 
	 */
	private CloseableHttpResponse doGetList(String object, List<Filter> filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		String filterString = null;
		if ( filters != null ) {
			StringBuffer filterList = new StringBuffer("[");
			Iterator<Filter> iter = filters.iterator();
			Filter filter = iter.next();
			filterList.append(filter.toString());
			while ( iter.hasNext() ) {
				filter = iter.next();
				filterList.append(",");
				filterList.append(filter.toString());
			}
			filterList.append("]");
			filterString = filterList.toString();
		}
		return this.doGetList(object, filterString, order, offset, limit);
	}
	
	/**
	 * Perform a HTTP GET for a list path.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @param filters array
	 * @param order
	 * @param offset
	 * @param limit
	 * @return InputStream of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 * @throws ArvadosException 
	 */
	private CloseableHttpResponse doGetList(String object, Filter[] filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		String filterString = null;
		if ( filters != null && filters.length > 0 ) {
			StringBuffer filterList = new StringBuffer("[");
			filterList.append(filters[0].toString());
			for ( int i = 1; i < filters.length; i++  ) {
				filterList.append(",");
				filterList.append(filters[i].toString());
			}
			filterList.append("]");
			filterString = filterList.toString();
		}
		return this.doGetList(object, filterString, order, offset, limit);
	}
	
	/**
	 * Perform a HTTP GET for a list path.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @return InputStream of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	private CloseableHttpResponse doGetItem(String object, String uuid) throws ArvadosException, IOException, URISyntaxException {
		if ( uuid == null )
			throw new ArvadosException("Arvados get method requires UUID: " + object);
		URIBuilder builder = this.startURI(object + "/" + uuid);
		
		HttpGet request = new HttpGet(builder.build());
		//System.out.println("get->"+request.getURI());
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
			List<Header> httpHeaders = Arrays.asList(request.getAllHeaders());        
		    for (Header header : httpHeaders) {
		        System.out.println("Headers.. name,value:"+header.getName() + "," + header.getValue());
		    }
		    InputStream in = response.getEntity().getContent(); 
		    BufferedReader br = null;
			StringBuilder sb = new StringBuilder();

			String line;
			try {

				br = new BufferedReader(new InputStreamReader(in));
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
				br.close();
				in.close();
			}
				catch(IOException e) {
					
				}
			
			System.out.println(sb.toString());
			response.close();
			
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		return response;
	}

	/**
	 * Perform a HTTP POST.
	 * 
	 * @param path relative path for API request, e.g. collections or groups
	 * @param parameters request parameters, i.e. form data.
	 * @return JSONObject of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	private CloseableHttpResponse doPost(String path, Map<String,Object> parameters,String uuid) throws ArvadosException, IOException, URISyntaxException {
		URIBuilder builder = this.startURI(path + "/"+uuid);
		HttpPost request = new HttpPost(builder.build());
		System.out.println(request.getURI());
		// If parameters are provided.  Create the encoded form data and add to the request.
		// Otherwise throw an exception.
		if ( parameters != null ) {
			List<NameValuePair> formdata = new ArrayList<NameValuePair>();
			for ( Entry<String,Object> entry : parameters.entrySet() ) {
				if ( entry.getValue() instanceof List ) {
					for ( Object item : (List)entry.getValue() ) {
						formdata.add(new BasicNameValuePair(entry.getKey(), item.toString()));
						System.out.println("66"+formdata);
					}
				} else {
					formdata.add(new BasicNameValuePair(entry.getKey(), entry.getValue().toString()));
					System.out.println(formdata);
				}
			}
			
			request.setEntity(new UrlEncodedFormEntity(formdata, Consts.UTF_8));
		} else {
			throw new ArvadosException("POST method requires content object " + path);
		}
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		System.out.println("respose code="+response.getAllHeaders().toString());
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
			response.close();
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		return response;
	}

	/**
	 * Method to update an ArvadosResource
	 * 
	 * @param path base URL path for the resource
	 * @param type 
	 * @param resource ArvadosResource to update
	 * @return
	 * @throws Exception 
	 */
	private CloseableHttpResponse doUpdate(String path, String type, ArvadosResource resource) throws Exception {	
		URIBuilder builder = this.startURI(path + "/" + resource.getUuid());
		HttpPut request = new HttpPut(builder.build());
		request.addHeader("Content-Type", "application/json");
		JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
		jsonBuilder.add(type, resource.updateJson());
		StringEntity str = new StringEntity(jsonBuilder.build().toString());	
		request.setEntity(str);
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
				response.close();
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		
		return response;
	}
	
	// For PUT "application/json"

	/**
	 * Perform a HTTP DELETE.
	 * 
	 * @param object type, e.g. collections or groups
	 * @param uuid ID of object to delete.
	 * @return JSONObject of the response.
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	private CloseableHttpResponse doDelete(String object, String uuid) throws ArvadosException, IOException, URISyntaxException {
		if ( uuid == null )
			throw new ArvadosException("Arvados get method requires UUID: " + object);

		URIBuilder builder = this.startURI(object + "/" + uuid);

		HttpDelete request = new HttpDelete(builder.build());
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		if ( response.getStatusLine().getStatusCode() != 200 ) {
			int statuscode = response.getStatusLine().getStatusCode();
			response.close();
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", statuscode));
		}
		return response;
	}
	
	public ArvadosList<Group> getGroups() throws IOException, URISyntaxException, ArvadosException {
		return this.getGroups((Filter[]) null, null, 0, 100);
	}

	public ArvadosList<Group> getGroups(List<Filter> filters) throws IOException, URISyntaxException, ArvadosException {
		return this.getGroups(filters, null, 0, 100);
	}

	public ArvadosList<Group> getGroups(Filter[] filters) throws IOException, URISyntaxException, ArvadosException {
		return this.getGroups(filters, null, 0, 100);
	}

	private ArvadosList<Group> parseGroupResponse(CloseableHttpResponse response) throws IOException, URISyntaxException, ArvadosException {
		ArvadosList<Group> groups = new ArvadosList<Group>();
		try {
			JsonParser parser = Json.createParser(response.getEntity().getContent());
			boolean initems = false;
			String key = null;

			while ( parser.hasNext() ) {
				Event event = parser.next();
				switch (event) {
				case KEY_NAME:
					key = parser.getString();
					break;
				case START_ARRAY:
					if ( key.equalsIgnoreCase(ITEMS_ATTR) )
						initems = true;
					break;
				case END_ARRAY:
					initems = false;
					break;
				case START_OBJECT:
					if ( initems )
						groups.add(new Group(parser));
					break;
				default:
					groups.handleValue(key, event, parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return groups;
	}
	
	public ArvadosList<Group> getGroups(Filter[] filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		return this.parseGroupResponse(this.doGetList(GROUPS_PATH, filters, order, offset, limit));	
	}
	
	public ArvadosList<Group> getGroups(List<Filter> filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		return this.parseGroupResponse(this.doGetList(GROUPS_PATH, filters, order, offset, limit));	
	}	
	
	public ArvadosList<Collection> getCollections(Filter[] filters) throws IOException, URISyntaxException, ArvadosException {
		return this.getCollections(filters, null, 0, 100);
	}

	public ArvadosList<Collection> getCollections(List<Filter> filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		return this.parseCollections(this.doGetList(COLLECTIONS_PATH, filters, order, offset, limit));
	}
		
	public ArvadosList<Collection> getCollections(Filter[] filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		return this.parseCollections(this.doGetList(COLLECTIONS_PATH, filters, order, offset, limit));
	}
		
	private ArvadosList<Collection> parseCollections(CloseableHttpResponse response) throws IOException, URISyntaxException, ArvadosException {
		ArvadosList<Collection> collections = new ArvadosList<Collection>();

		try {
			JsonParser parser = Json.createParser(response.getEntity().getContent());

			boolean initems = false;
			String key = null;

			while ( parser.hasNext() ) {
				Event event = parser.next();
				switch (event) {
				case KEY_NAME:
					key = parser.getString();
					break;
				case START_ARRAY:
					if ( key.equalsIgnoreCase(ITEMS_ATTR) )
						initems = true;
					break;
				case END_ARRAY:
					initems = false;
					break;
				case START_OBJECT:
					if ( initems )
						collections.add(new Collection(parser));
					break;
				default:
					collections.handleValue(key, event, parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return collections;
	}
	
	public ArvadosList<KeepService> getKeepServices(List<Filter> filters, String order, int offset, int limit) throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetList(KEEP_SERVICES_PATH, filters, order, offset, limit);
		ArvadosList<KeepService> services = null;

		try {
			JsonParser parser = Json.createParser(response.getEntity().getContent());
			String key = null;

			while ( parser.hasNext() ) {
				Event event = parser.next();
				switch (event) {
				case KEY_NAME:
					key = parser.getString();
					break;
				case START_ARRAY:
					if ( key.equalsIgnoreCase(ITEMS_ATTR) )
						services = this.parseKeepServices(parser);
					break;
				default:
					services.handleValue(key, event, parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return services;
	}
		
		
	protected ArvadosList<KeepService> parseKeepServices(JsonParser parser) {	
		ArvadosList<KeepService> services = new ArvadosList<KeepService>();

		int depth = 0;
		
		while ( parser.hasNext() ) {
			Event event = parser.next();
			switch (event) {
			case START_OBJECT:
				services.add(new KeepService(parser)); break;
			case START_ARRAY:
				depth++; break;
			case END_ARRAY:
				if ( depth == 0)
					return services;
			default:
				break;
			}
		}
		return services;
	}

	public ArvadosList<KeepService> getKeepServices() throws IOException, URISyntaxException, ArvadosException {
		return this.getKeepServices(null, null, 0, 100);
	}
	
	private final static Filter[] KEEP_PROXY_FILTER = { new Filter(KeepService.SERVICE_TYPE_ATTR, Operator.EQUAL, "proxy") }; 
	
	public ArvadosList<KeepService> getKeepProxies() throws IOException, URISyntaxException, ArvadosException { 
		CloseableHttpResponse response = this.doGetList(KEEP_SERVICES_PATH, KEEP_PROXY_FILTER, null, 0, 100);
		ArvadosList<KeepService> services = null;

		try {
			JsonParser parser = Json.createParser(response.getEntity().getContent());
			String key = null;

			while ( parser.hasNext() ) {
				Event event = parser.next();
				switch (event) {
				case KEY_NAME:
					key = parser.getString();
					break;
				case START_ARRAY:
					if ( key.equalsIgnoreCase(ITEMS_ATTR) )
						services = this.parseKeepServices(parser);
					break;
				default:
					break;
				}
			}
		} finally {
			response.close();
		}
		return services;
	}
	
	public Group getGroup(String uuid) throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetItem(GROUPS_PATH, uuid);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		Group group = null;
		try {
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT ) {
					group = new Group(parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return group;
	}

	public Collection getCollection(String uuid) throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetItem(COLLECTIONS_PATH, uuid);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		Collection collection = null;
		try { 
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT ) {
					collection = new Collection(parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return collection;
	}

	public KeepService getKeepService(String uuid) throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetItem(KEEP_SERVICES_PATH, uuid);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		KeepService service = null;
		try {
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT ) {
					service = new KeepService(parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return service;
	}
	
	public User getUser(String uuid) throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetItem(USERS_PATH, uuid);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		User user = null;
		try {
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT ) {
					user = new User(parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return user;
	}

	public User getCurrentUser() throws IOException, URISyntaxException, ArvadosException {
		CloseableHttpResponse response = this.doGetItem(USERS_PATH, "current");
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		User user = null;
		try {
			while (parser.hasNext()) {
				if (parser.next() == Event.START_OBJECT ) {
					user = new User(parser);
					break;
				}
			}
		} finally {
			response.close();
		}
		return user;
	}	
	
	/*
	public Collection renameCollection(String uuid,String method,String name) throws ArvadosException, IOException, URISyntaxException {
		CloseableHttpResponse response = this.doUpdate(COLLECTIONS_PATH,uuid,method,name );
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		Collection collection = null;
		try {
			while(parser.hasNext()) {
				if(parser.next() == Event.START_OBJECT) {
					collection = new Collection(parser);
					break;
				}
			}
		}finally {
			 response.close();
		}
		return collection;
	}
	 */
	
	/**
	 * Method to update an Arvados @see edu.uic.rrc.portal.model.entities.arvados.Collection.
	 * 
	 * @param collection Aravdos resource to update
	 * @return updated object or original object if no update was performed
	 * @throws Exception 
	 */
	public Collection updateResource(Collection collection) throws Exception {
		CloseableHttpResponse response = this.doUpdate(COLLECTIONS_PATH, "collection", collection);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		
		try {
			while( parser.hasNext() ) {
				if( parser.next() == Event.START_OBJECT ) {
					collection = new Collection(parser);
					break;
				}
			}
		} finally {
			 response.close();
		}
		return collection;
	}
	
	/**
	 * Method to update an Arvados @see edu.uic.rrc.portal.model.entities.arvados.Group.
	 * 
	 * @param group group to update
	 * @return updated object or original object if no update was performed
	 * @throws Exception 
	 */
	public Group updateResource(Group group) throws Exception {

		CloseableHttpResponse response = this.doUpdate(GROUPS_PATH, "group", group);
		JsonParser parser = Json.createParser(response.getEntity().getContent());
		
		try {
			while( parser.hasNext() ) {
				if( parser.next() == Event.START_OBJECT ) {
					group = new Group(parser);
					break;
				}
			}
		} finally {
			 response.close();
		}
		return group;
	}
/*	
	public CloseableHttpResponse getCollectionResource(KeepService service, Collection collection, String filePath) throws URISyntaxException, IOException, ArvadosException {
		return this.getCollectionResource(service, collection.getUUID(), filePath);
	}
	
	public CloseableHttpResponse getCollectionResource(KeepService service, CollectionFile file) throws URISyntaxException, IOException, ArvadosException {
		String filePath = file.getPath();
		if ( filePath.startsWith(".") )
			filePath = filePath.substring(1);
		
		if ( filePath.length() > 1 ) {
			filePath = filePath + "/" + file.getFilename();
		} else {
			filePath = file.getFilename();
		}
		
		return this.getCollectionResource(service, file.getCollection(), filePath);
	}
	
	public CloseableHttpResponse getCollectionResource(CollectionFile file) throws URISyntaxException, IOException, ArvadosException {
		List<KeepService> services = this.getKeepProxies();
		if ( services.size() == 0 ) {
			throw new ArvadosException("Unable to fetch file. No registered keep proxy.");
		} else if ( services.size() > 1 ) {
			logger.warn("More than one keep proxy available. Using first keep proxy to fetch file.");
		}
		return this.getCollectionResource(services.get(0), file);
	}
	*/
	/**
	 * Get a collection resource.  Returns an CloseableHttpResponse object.  This should be closed once the input is read.
	 * 
	 * @param service Keep service to access the file
	 * @param collectionUUID UUID of the parent collection
	 * @param filePath Relative file path of the resource
	 * @return CloseableHttpResponse object
	 * @throws URISyntaxException
	 * @throws IOException
	 * @throws ArvadosException
	 */
	public CloseableHttpResponse getCollectionResource(String keepwebURL, String collectionUUID, String filePath) throws URISyntaxException, IOException, ArvadosException {
		URI uri = new URI(keepwebURL + "/c=" + collectionUUID + "/" + filePath.replace(" ", "%20"));
		HttpGet request = new HttpGet(uri);
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		int statuscode = response.getStatusLine().getStatusCode();
		String message;
		switch ( statuscode ) {
		case 200:
			return response;
		case 404:
			message = "Resource not found";
			break;
		default:
			message = String.format("Invalid HTTP response code (%d).", statuscode);
		}
		response.close();
		throw new ArvadosException(message);
	}
	
	/**
	 * Check if the current user (API token) could manage the object, e.g. a project.
	 * 
	 * @param uuid UUID of the object to manage
	 * @return true if can manage, false if not
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws ArvadosException
	 */
	public boolean canManage(String uuid) throws IOException, URISyntaxException, ArvadosException {
		URIBuilder builder = this.startURI("permissions/" + uuid);
		HttpGet request = new HttpGet(builder.build());
		this.addAuth(request);
		CloseableHttpResponse response = this.http.execute(request);
		int status = response.getStatusLine().getStatusCode();
		response.close();

		switch (status) {
		case 200:
			return true;
		case 403:
			return false;
		default:
			throw new ArvadosException(String.format("Invalid HTTP response code (%d).", status));
		}
	}

}
