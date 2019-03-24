package edu.uic.rrc.arvados;

/**
 * Generic exception that is used to denote exceptions thrown by arvadosApi calls
 * @author George Chlipala
 *
 */
public class ArvadosException extends Exception {

	private static final long serialVersionUID = -6684846368514434121L;

	public ArvadosException(String message) {
		super(message);
	}

}
