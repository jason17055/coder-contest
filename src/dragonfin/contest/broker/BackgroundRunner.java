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
	Date startDate;

	BackgroundRunner(ServletContext ctx)
	{
		this.ctx = ctx;
	}

	public void run()
	{
		this.startDate = new Date();

		try {

		for (;;) {

			try {

			log.info("background thread updating datastore");
			boolean keepRunning = postStatus();

			if (!keepRunning && getRunTime() >= MIN_RUN_TIME) {
				maybeInvokeShutdown();
			}

			}
			catch (Exception e) {
				log.warning("background thread error: " + e.getMessage());
			}

			log.info("background thread going to sleep");
			Thread.sleep(SLEEP_INTERVAL); //five minutes

		}//end loop

		}
		catch (InterruptedException e) {
			log.info("background thread has been interrupted");
		}
	}

	static final long MIN_RUN_TIME = 30*60*1000; //30 minutes
	static final long SLEEP_INTERVAL = 5*60*1000; //5 minutes

	long getRunTime()
	{
		return new Date().getTime() - startDate.getTime();
	}

	void maybeInvokeShutdown()
	{
		log.info("invoking module shutdown");

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

			ent.setProperty("target_state", "shutdown");

			ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		modulesApi.stopVersion(
			modulesApi.getCurrentModule(),
			modulesApi.getCurrentVersion()
			);
	}

	/**
	 * @return true if the broker has recently dispatched a job
	 */
	boolean postStatus()
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

			ent.setProperty("last_started", startDate);
			ent.setProperty("last_heartbeat", new Date());

			ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		boolean keepRunning = true;

		JobQueue jq = (JobQueue) ctx.getAttribute("jobQueue");
		if (jq != null) {
			keepRunning = keepRunning && jq.postStatus(brokerKey);
		}

		return keepRunning;
	}
}
