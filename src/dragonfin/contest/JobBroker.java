package dragonfin.contest;

import java.io.*;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.modules.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

/**
 * Methods allowing front-end to interface with Job Broker backend.
 */
public class JobBroker
{
	private static final Logger log = Logger.getLogger(
		JobBroker.class.getName());

	public static String getJobBrokerUrl()
	{
		ModulesService modulesApi = ModulesServiceFactory.getModulesService();
		String v = modulesApi.getCurrentVersion();
		return "http://" + modulesApi.getVersionHostname("job-broker", v);
	}

	public static String getFeedUrl(String contestId, String workerId)
	{
		return getJobBrokerUrl() + "/feed?"
			+ (contestId != null ? "contest="+escapeUrl(contestId)+"&" : "")
			+ "worker=" + escapeUrl(workerId);
	}

	public static void notifyNewJob(Key jobKey)
	{
		startBrokerIfNeeded();

		String jobId = Long.toString(jobKey.getId());
		String url = getJobBrokerUrl()+"/notify?job="+escapeUrl(jobId);
		try {
			InputStream inStream = new URL(url).openStream();
			byte [] b = new byte[1024];
			while (inStream.read(b) != -1);
			inStream.close();
		}
		catch (Exception e) {
			log.warning("unable to notify job-broker: "+e.getMessage());
		}
	}

	static final long ALIVE_THRESHOLD = 30000;     //30 seconds
	static final long MIN_RETIREMENT = 3*60*60*1000; //3 hours

	public static void startBrokerIfNeeded()
	{
		Date curTime = new Date();

		// determine broker name
		ModulesService modulesApi = ModulesServiceFactory.getModulesService();
		String brokerName = modulesApi.getInstanceHostname(
				"job-broker",
				modulesApi.getCurrentVersion(),
				"0"
				);
		Key brokerKey = KeyFactory.createKey("JobBroker", brokerName);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		boolean shouldStart = false;
		boolean shouldExtend = false;
		try {
			Entity ent;
			try {
				ent = ds.get(brokerKey);
			}
			catch (EntityNotFoundException e) {
				ent = new Entity(brokerKey);
			}

			String targetState = (String) ent.getProperty("target_state");
			if (targetState == null || !targetState.equals("running")) {

				shouldStart = true;
				ent.setProperty("target_state", "running");
			}

			Date lastHeartbeat = (Date) ent.getProperty("last_heartbeat");
			boolean isAlive = (lastHeartbeat != null && (curTime.getTime() - lastHeartbeat.getTime()) < ALIVE_THRESHOLD);

			Date lastAttempt = (Date) ent.getProperty("last_start_attempt");
			boolean recentlyAttempted = (lastAttempt != null && (curTime.getTime() - lastAttempt.getTime()) < ALIVE_THRESHOLD);

			Date retireAfter = (Date) ent.getProperty("retire_after");
			boolean retiresSoon = (retireAfter == null || (retireAfter.getTime() - curTime.getTime()) < ALIVE_THRESHOLD);

			if (!isAlive && !recentlyAttempted) {
				shouldStart = true;
			}
			else if (retiresSoon) {
				shouldExtend = true;
			}

			if (shouldStart) {
				ent.removeProperty("last_started"); //cleanup obsolete property
				ent.setProperty("last_start_attempt", curTime);
				ent.setProperty("retire_after", new Date(curTime.getTime()+MIN_RETIREMENT));
				ds.put(ent);
			}
			else if (shouldExtend) {

				ent.setProperty("retire_after", new Date(curTime.getTime()+MIN_RETIREMENT));
				ds.put(ent);
			}

			txn.commit();
		}
		catch (ConcurrentModificationException e) {

			// ignore
			log.warning("when trying to start broker: " + e);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (shouldStart) {

			log.info("attempting to start broker");
			modulesApi.startVersion(
				"job-broker",
				modulesApi.getCurrentVersion()
				);
		}
	}
}
