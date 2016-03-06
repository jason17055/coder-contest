package dragonfin.contest.broker;

import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.modules.*;

import static dragonfin.contest.common.JobBrokerName.getJobBrokerName;

class BackgroundRunner implements Runnable
{
	final ServletContext ctx;

	private static final Logger log = Logger.getLogger(BackgroundRunner.class.getName());
	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
	ModulesService modulesApi = ModulesServiceFactory.getModulesService();
	Date expires = null; //earliest time we can shut down

	BackgroundRunner(ServletContext ctx)
	{
		this.ctx = ctx;
	}

	String getAddress()
	{
		return modulesApi.getInstanceHostname(
			modulesApi.getCurrentModule(),
			modulesApi.getCurrentVersion(),
			modulesApi.getCurrentInstanceId());
	}

	public void run()
	{
		this.expires = new Date(new Date().getTime() + INITIAL_AWAKE_INTERVAL);

		try {

		for (;;) {

			try {

			log.info("background thread updating datastore");
			boolean keepRunning = postStatus();

			Date curDate = new Date();
			if (!keepRunning && curDate.after(this.expires)) {
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

	static final long SLEEP_INTERVAL = 5*60*1000; //5 minutes
	static final long INITIAL_AWAKE_INTERVAL = 1*60*1000; //1 minute

	void maybeInvokeShutdown()
	{
		log.info("checking for module shutdown");

		String brokerName = getJobBrokerName(modulesApi.getCurrentInstanceId());
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

			Date retireAfter = (Date) ent.getProperty("retire_after");
			if (retireAfter != null && retireAfter.after(this.expires)) {
				// not ok to retire yet
				this.expires = retireAfter;
				return;
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

		try {
			log.info("shutting down broker");
			modulesApi.stopVersion(
				modulesApi.getCurrentModule(),
				modulesApi.getCurrentVersion()
				);
		}
		catch (ModulesException e) {
			log.warning("error while stopping broker: " + e.getMessage());
		}
	}

	/**
	 * @return true if the broker has recently dispatched a job
	 */
	boolean postStatus()
	{
		String brokerName = getJobBrokerName(modulesApi.getCurrentInstanceId());
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
			ent.setProperty("address", getAddress());

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
