package dragonfin.contest;

import java.io.*;
import java.net.URL;
import java.util.logging.Logger;
import com.google.appengine.api.modules.*;

import static dragonfin.contest.CoreServlet.escapeUrl;

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
		return "http://" + modulesApi.getVersionHostname("job_broker", v);
	}

	public static String getFeedUrl(String contestId, String workerId)
	{
		return getJobBrokerUrl() + "/feed?"
			+ (contestId != null ? "contest="+escapeUrl(contestId)+"&" : "")
			+ "worker=" + escapeUrl(workerId);
	}

	public static void notifyNewJob(String jobId)
	{
		String url = getJobBrokerUrl()+"/notify?job="+escapeUrl(jobId);
		try {
			InputStream inStream = new URL(url).openStream();
			byte [] b = new byte[1024];
			while (inStream.read(b) != -1);
			inStream.close();
		}
		catch (Exception e) {
			log.warning("unable to notify job_broker: "+e.getMessage());
		}
	}
}
