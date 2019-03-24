package edu.uic.rrc.portal.model.entities.arvados;

import java.util.Iterator;
import java.util.List;
/**
 * This class is used to send filters to the arv commands to fetch matching ArvadosResources.
 * @author George Chlipala
 *
 */
public class Filter {
	
	private String attribute;
	private Operator operator;
	private Object operand;
	/**
	 * 
	 * @author George Chlipala
	 *
	 */
	public enum Operator {
		LESS_THAN("<", true, false, false), LESS_THAN_EQUAL("<=", true, false, false), GREATER_THAN_EQUAL(">=", true, false, false), 
		GREATER_THAN(">", true, false, false), LIKE("like", true, false, false), ILIKE("ilike", true, false, false),
		EQUAL("=", true, true, false), NOT_EQUAL("!=", true, true, false),
		IN("in", false, false, true), NOT_IN("not in", false, false, true),
		ISA("is_a", true, false, false);
		
		String opstring;
		boolean allowString;
		boolean allowNull;
		boolean allowArray;
		
		/**
		 * 
		 * @param opstring
		 * @param allowString
		 * @param allowNull
		 * @param allowArray
		 */
		Operator(String opstring, boolean allowString, boolean allowNull, boolean allowArray) {
			this.opstring = opstring;
			this.allowString = allowString;
			this.allowNull = allowNull;
			this.allowArray = allowArray;
		}
	}
	
	/**
	 * 
	 * @param attribute
	 * @param operator
	 * @param operand
	 */
	public Filter(String attribute, Operator operator, String operand) {
		this.attribute = attribute;
		this.operator = operator;
		this.operand = operand;
	}
	
	public String toString() {
		String start = "[\"" + this.attribute + "\", \"" + 
				this.operator.opstring + "\"," ;
		if ( this.operand instanceof String ) {
			if ( this.operator.allowString ) {
				return start + "\"" + this.operand + "\"]";
			}
		} else if ( this.operand instanceof List ) {
			if ( this.operator.allowArray ) {
				StringBuffer ops = new StringBuffer();
				Iterator iter = ((List)this.operand).iterator();
				Object obj = iter.next();
				ops.append("\"");
				ops.append((String)obj);
				ops.append("\"");
				while ( iter.hasNext() ) {
					obj = iter.next();
					ops.append(", \"");
					ops.append((String)obj);
					ops.append("\"");
				}
				return start + "[" + ops.toString() + "]]";
			}
		} else if ( this.operand == null ) {
			if ( this.operator.allowNull ) {
				return start + " null ]";
			}
 		} 
		return null;
	}
}
