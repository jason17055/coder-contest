package dragonfin.contest.broker;

import java.util.logging.Logger;
import javax.servlet.*;

class BackgroundRunner implements Runnable
{
	final ServletContext ctx;

	private static final Logger log = Logger.getLogger(BackgroundRunner.class.getName());

	BackgroundRunner(ServletContext ctx)
	{
		this.ctx = ctx;
	}

	public void run()
	{
		try {

		for (;;) {

			log.info("background thread going to sleep");
			Thread.sleep(5*60*1000); //five minutes

			log.info("background thread updating datastore");
			postStatus();

		}//end loop

		}
		catch (InterruptedException e) {
			log.info("background thread has been interrupted");
		}
	}

	void postStatus()
	{
		JobQueue jq = (JobQueue) ctx.getAttribute("jobQueue");
		if (jq != null) {
			jq.postStatus();
		}
	}
}
