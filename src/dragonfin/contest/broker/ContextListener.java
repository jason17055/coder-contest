package dragonfin.contest.broker;

import java.util.logging.Logger;
import javax.servlet.*;
import com.google.appengine.api.modules.*;
import com.google.appengine.api.ThreadManager;

public class ContextListener implements ServletContextListener
{
	ModulesService modulesApi = ModulesServiceFactory.getModulesService();
	Thread backgroundThread;

	private static final Logger log = Logger.getLogger(ContextListener.class.getName());

	//implements ServletContextListener
	public void contextInitialized(ServletContextEvent evt)
	{
		ServletContext ctx = evt.getServletContext();
		ctx.setAttribute("jobQueue", new JobQueue("_default"));

		if (backgroundThread != null) {
			log.info("background thread already exists!");
		}
		else {
			log.info("creating background thread for broker #" + modulesApi.getCurrentInstanceId());
			this.backgroundThread = ThreadManager.createBackgroundThread(
				new BackgroundRunner(ctx)
				);
			this.backgroundThread.start();
		}
	}

	//implements ServletContextListener
	public void contextDestroyed(ServletContextEvent evt)
	{
		if (backgroundThread != null) {
			log.info("interrupting background thread");
			backgroundThread.interrupt();
			try {
				backgroundThread.join();
				backgroundThread = null;
				log.info("background thread has terminated");
			}
			catch (InterruptedException e) {
				log.warning("interrupted while waiting for background thread to terminate");
			}
		}
	}
}
