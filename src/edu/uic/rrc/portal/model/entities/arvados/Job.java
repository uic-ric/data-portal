/**
 * 
 */
package edu.uic.rrc.portal.model.entities.arvados;

/**This class is used to represent a Job arvados entity 
 * @author George Chlipala
 *
 */
public class Job extends ArvadosResource {

	static final String SCRIPT_ATTR = "script";
	static final String SCRIPT_PARAMETERS_ATTR = "script_parameters";
	static final String REPOSITORY_ATTR = "repository";
	static final String SCRIPT_VERSION_ATTR = "script_version";
	static final String CANCELLED_BY_CLIENT_ATTR = "cancelled_by_client_uuid";
	static final String CANCELLED_BY_USER_ATTR = "cancelled_by_user_uuid";
	static final String CANCELLED_AT_ATTR = "cancelled_at";
}
