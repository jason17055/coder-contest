package dragonfin.contest;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static dragonfin.contest.TemplateVariables.defaultResultEntity;
import static dragonfin.contest.TemplateVariables.getContestFromUserKey;

public class JobCompletedTask extends HttpServlet
{
	private static final Logger log = Logger.getLogger(JobCompletedTask.class.getName());

	static void enqueueTask(long jobId)
	{
		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(TaskOptions.Builder.withUrl("/_task/job_completed")
			.param("job", Long.toString(jobId))
			);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		MyHandler h = new MyHandler(req, resp);
		h.doPost();
	}

	class MyHandler
	{
		final HttpServletRequest req;
		final HttpServletResponse resp;
		DatastoreService ds;
		Entity ent;

		MyHandler(HttpServletRequest req, HttpServletResponse resp) {
			this.req = req;
			this.resp = resp;
			this.ds = DatastoreServiceFactory.getDatastoreService();
		}

		void doPost()
			throws IOException
		{
			String jobId_s = req.getParameter("job");
			long jobId = Long.parseLong(jobId_s);

			Key jobKey = KeyFactory.createKey("TestJob", jobId);
			try {
				this.ent = ds.get(jobKey);
			}
			catch (EntityNotFoundException e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			String jobType = (String)ent.getProperty("type");
			if ("S".equals(jobType)) {

				systemTest_step1();
			}
		}		

		void systemTest_step1()
			throws IOException
		{
			Key testResultKey = (Key)ent.getProperty("test_result");
			if (testResultKey == null) {
				return;
			}

			String resultStatus = (String)ent.getProperty("result_status");
			//if (!"No Error".equals(resultStatus)) {
				resolveSystemTest(testResultKey, resultStatus);
			//}
		}

		void resolveSystemTest(Key testResultKey, String status)
			throws IOException
		{
			Transaction txn = ds.beginTransaction();
			try {

				Entity resultEnt = ds.get(testResultKey);
				resultEnt.setProperty("job", ent.getKey());
				resultEnt.setProperty("result_status", status);
				ds.put(resultEnt);
				txn.commit();

			}
			catch (EntityNotFoundException e) {

				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}
	}
}
