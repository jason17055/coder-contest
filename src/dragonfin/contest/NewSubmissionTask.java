package dragonfin.contest;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static dragonfin.contest.common.CommonFunctions.*;
import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;

public class NewSubmissionTask extends HttpServlet
{
	static void enqueueTask(Key submissionKey)
	{
		Key userKey = submissionKey.getParent();

		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(withUrl("/_task/new_submission")
			.param("submitter", userKey.getName())
			.param("submission", Long.toString(submissionKey.getId()))
			);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String userId = req.getParameter("submitter");
		String submissionId = req.getParameter("submission");

		Key userKey = KeyFactory.createKey("User", userId);
		Key submissionKey = KeyFactory.createKey(userKey, "Submission", Long.parseLong(submissionId));

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity submissionEnt;
		Entity problemEnt;
		Entity contestEnt;
		String contestId;

		try {
			submissionEnt = ds.get(submissionKey);

			Key problemKey = (Key) submissionEnt.getProperty("problem");
			problemEnt = ds.get(problemKey);

			Key contestKey = problemKey.getParent();
			contestEnt = ds.get(contestKey);
			contestId = contestKey.getName();
		}
		catch (EntityNotFoundException e) {

			// one or more relevant entities not found...
			// return 404-Not-Found in the hopes that when App Engine
			// requeues this task the relevant entities will be available

			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		// determine the number of minutes into the contest for this submission
		Date submitted = (Date) submissionEnt.getProperty("created");
		if (submitted == null) {
			submitted = new Date();
		}

		// determine basis time
		Date basisTime = (Date) problemEnt.getProperty("start_time");
		if (basisTime == null) {

			basisTime = (Date) contestEnt.getProperty("started");

			if (basisTime == null) {
				basisTime = new Date();
			}
		}

		long elapsedMillisec = submitted.getTime() - basisTime.getTime();
		long minutes = elapsedMillisec / 60000;
		if (minutes < 1) {
			minutes = 1;
		}

		Transaction txn = ds.beginTransaction();
		try {

			Entity ent = ds.get(submissionKey);
			ent.setProperty("minutes", new Long(minutes));
			ent.setProperty("judge", null);

			ds.put(ent);
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

		//
		// create TestResult entities and enqueue associated jobs
		//
		Key sourceFileKey = (Key) submissionEnt.getProperty("source");
		String sourceFileExt = null;
		if (sourceFileKey != null) {
			try {
				Entity fileEnt = ds.get(sourceFileKey);
				sourceFileExt = fileExtensionOf((String)fileEnt.getProperty("given_name"));
			}
			catch (EntityNotFoundException e) {
				//ignore
			}
		}

		Query q = new Query("SystemTest");
		q.setAncestor(problemEnt.getKey());

		PreparedQuery pq = ds.prepare(q);
		for (Entity systemTestEnt : pq.asIterable()) {

			Key resultKey = KeyFactory.createKey(submissionKey, "TestResult", systemTestEnt.getKey().getId());
			Key inputFileKey = (Key) systemTestEnt.getProperty("input");

			Entity ent = new Entity("TestJob");
			ent.setProperty("created", new Date());
			ent.setProperty("source", sourceFileKey);
			ent.setProperty("source_extension", sourceFileExt);
			ent.setProperty("input", inputFileKey);
			ent.setProperty("type", "S");
			ent.setProperty("contest", contestId);
			ent.setProperty("claimed", Boolean.FALSE);
			ent.setProperty("finished", Boolean.FALSE);
			ent.setProperty("owner", null);
			ent.setProperty("problem", problemEnt.getKey());

			Key jobKey = ds.put(ent);
			JobBroker.notifyNewJob(jobKey);

			madeNewJobFor(ds, resultKey, jobKey);
		}

		resp.setStatus(HttpServletResponse.SC_OK);
	}

	void madeNewJobFor(DatastoreService ds, Key resultKey, Key jobKey)
		throws ServletException
	{
		for (int attempt = 0; attempt < 10; attempt++) {

		Transaction txn = ds.beginTransaction();
		try {

			Entity ent;
			try {
				ent = ds.get(resultKey);
			}
			catch (EntityNotFoundException e) {

				ent = new Entity(resultKey);
			}

			ent.setProperty("job", jobKey);
			ent.setProperty("result_status", null);

			ds.put(ent);
			txn.commit();

			return;
		}
		catch (ConcurrentModificationException e) {
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		} // each attempt

		// if here, then we tried 10 times but got concurrent modification each time
		throw new ServletException("unable to modify TestResult entity");
	}
}
