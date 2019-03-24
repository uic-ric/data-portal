package edu.uic.rrc.portal.listener;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import edu.uic.rrc.arvados.ArvadosAPI;
import edu.uic.rrc.portal.model.entities.arvados.Group;

/**
 * Application Lifecycle Listener implementation class TaskSchedulingListener
 * This listener initiates a timer task that runs every 60 mins to update the Project Map that holds the updated group entity of Arvados
 */
@WebListener
public class ProjectSyncScheduleListener implements ServletContextListener {

	public static Map<String,Group> projectMap = new HashMap<String,Group>();
    /**
     * Default constructor. 
     */
    public ProjectSyncScheduleListener() {
        // TODO Auto-generated constructor stub
    }

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		Timer time = new Timer();
		SyncProjectMapTask st = new SyncProjectMapTask(); 
		time.schedule(st, 0, 1000*60*30); 
		
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		// TODO Auto-generated method stub
		
	}
	
	class SyncProjectMapTask extends TimerTask {
		public void run() {			
			Date epires = new Date(System.currentTimeMillis()-60*60*1000); 
			for(Entry<String,Group> entry: projectMap.entrySet()){
				Date lastRetrieved = entry.getValue().getRetrieved();
				if(lastRetrieved.before(epires)){
					projectMap.remove(entry.getKey());
				}
			}
				System.out.println("ProjectSyncScheduleListener:: SyncProjectMapTask updatin project map at :" + new Date()); 
		}
		
	}
	
	public static Group getProject(String projectid) throws Exception{
		if(!projectMap.containsKey(projectid)){
			ArvadosAPI arv = AppConfigListener.getArvadosApi(null);
			projectMap.put(projectid, arv.getGroup(projectid));
		}
		return projectMap.get(projectid);
	}


}
