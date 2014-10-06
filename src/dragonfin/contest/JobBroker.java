package dragonfin.contest;

import java.io.*;
import java.net.URL;
import java.util.logging.Logger;
import com.google.appengine.api.modules.*;

import static dragonfin.contest.CoreServlet.escapeUrl;

public class JobBroker
{
	private static final Logger log = Logger.getLogger(
		JobBroker.class.getName());

	public static void notifyNewJob(String jobId)
	{
		ModulesService modulesApi = ModulesServiceFactory.getModulesService();
		String jobBrokerHost = modulesApi.getVersionHostname("job_broker", "1");

		try {
			URL url = new URL("http://"+jobBrokerHost+"/notify?job="+escapeUrl(jobId));
			InputStream inStream = url.openStream();
			byte [] b = new byte[1024];
			while (inStream.read(b) != -1);
			inStream.close();
		}
		catch (Exception e) {
			log.warning("unable to notify job_broker: "+e.getMessage());
		}
	}
}
