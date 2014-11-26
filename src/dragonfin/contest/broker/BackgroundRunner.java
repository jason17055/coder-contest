package dragonfin.contest.broker;

import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.modules.*;

class BackgroundRunner implements Runnable
{
	final ServletContext ctx;

	private static final Logger log = Logger.getLogger(BackgroundRunner.class.getName());
	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
	ModulesService modulesApi = ModulesServiceFactory.getModulesService();

	BackgroundRunner(ServletContext ctx)
	{
		this.ctx = ctx;
	}

	public void run()
	{
		try {

		for (;;) {

//TODO- catch datastore exceptions (esp. concurrent modifications)
//  and retry

			log.info("background thread updating datastore");
			postStatus();

			log.info("background thread going to sleep");
			Thread.sleep(5*60*1000); //five minutes

		}//end loop

		}
		catch (InterruptedException e) {
			log.info("background thread has been interrupted");
		}
	}

	void postStatus()
	{
		String brokerName = modulesApi.getInstanceHostname(
				modulesApi.getCurrentModule(),
				modulesApi.getCurrentVersion(),
				modulesApi.getCurrentInstanceId()
			);
		Key brokerKey = KeyFactory.createKey("JobBroker", brokerName);

		Transaction txn = ds.beginTransaction();
		try {
			Entity ent;
			try {
				ent = ds.get(brokerKey);
			}
			catch (EntityNotFoundException e) {
				ent = new Entity(brokerKey);
			}

			ent.setProperty("last_heartbeat", new Date());

			ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		JobQueue jq = (JobQueue) ctx.getAttribute("jobQueue");
		if (jq != null) {
			jq.postStatus(brokerKey);
		}
	}
}
