package edu.uic.rrc.portal.model.arvados;

import java.util.List;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.listener.AppConfigListener;
import edu.uic.rrc.portal.model.entities.arvados.Filter;
import edu.uic.rrc.portal.model.entities.arvados.Group;
import edu.uic.rrc.portal.model.entities.arvados.Filter.Operator;
/**
 * This class is used to make all necessary calls to arvados that relate to the Group = "group_class"
 * @author SaiSravith
 *
 */
public class GroupManager {
	private ArvadosAPI arv = null;
	/**
	 * 
	 * @throws Exception
	 */
	public GroupManager() throws Exception {
		this.arv = AppConfigListener.getArvadosApi(null);
	}
	/**
	 * 
	 * @param uuid
	 * @return Group
	 * @throws Exception
	 */
	public Group getGroup(String uuid) throws Exception {
		return this.arv.getGroup(uuid);
	}
	/**
	 * 
	 * @return Group list
	 * @throws Exception
	 */
	public List<Group> listGroups() throws Exception {
	    Filter[] filters = new Filter[1];
	    filters[0] = new Filter("group_class", Operator.EQUAL, "project");   
    	return this.arv.getGroups(filters);
	}

	//TODO
	public void createGroup(Group group){
		
	}
	//TODO
	public void updateGroup(Group group){
		
	}
	//TODO
	public void deleteGroup(Group group){
		
	}
	//TODO
	public void getGroupContents(Group group){
		
	}
	//TODO
	public void showGroups(Group group){
		
	}
	//TODO
	public void destroyGroups(Group group){
		
	}
}
