package dragonfin.contest;

import java.io.IOException;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class NewSubmissionTask extends HttpServlet
{
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String userId = req.getParameter("submitter");
		String submissionId = req.getParameter("submission");

		Key userKey = KeyFactory.createKey("User", userId);
		Key submissionKey = KeyFactory.createKey(userKey, "Submission", Long.parseLong(submissionId));

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity submissionEnt;
		Entity problemEnt;
		Entity contestEnt;

		try {
			submissionEnt = ds.get(submissionKey);

			Key problemKey = (Key) submissionEnt.getProperty("problem");
			problemEnt = ds.get(problemKey);

			Key contestKey = problemKey.getParent();
			contestEnt = ds.get(contestKey);
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

		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
