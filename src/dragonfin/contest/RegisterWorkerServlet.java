package dragonfin.contest;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import dragonfin.contest.common.FileUploadFormHelper;
import static dragonfin.contest.TemplateVariables.makeFileUrl;

public class RegisterWorkerServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		doRegister(req, resp);
	}

	void doRegister(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String contestId = req.getParameter("contest");
		String languages = req.getParameter("languages");
		String workerName = req.getParameter("name");
		String workerDescription = req.getParameter("description");
		String systemDescription = req.getParameter("system");

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);

		// check to ensure that contest exists
		// TODO- check security
		try {
			Entity contestEnt = ds.get(contestKey);
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Transaction txn = ds.beginTransaction();

		String workerId;
		try {

			Entity ent = new Entity("Worker", contestKey);
			ent.setProperty("created", new Date());
			ent.setProperty("accepted_languages", languages);
			ent.setProperty("name", workerName);
			ent.setProperty("description", workerDescription);
			ent.setProperty("system", systemDescription);

			Key workerKey = ds.put(ent);
			workerId = Long.toString(workerKey.getId());

			Key statusKey = KeyFactory.createKey(workerKey, "WorkerStatus", 1);
			Entity statusEnt = new Entity(statusKey);
			statusEnt.setProperty("busy", Boolean.FALSE);
			statusEnt.setProperty("last_refreshed", new Date());
			ds.put(statusEnt);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		resp.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.printf("worker_id %s\n", workerId);
		out.printf("feed_url %s\n", JobBroker.getFeedUrl(contestId, workerId));
		out.close();
	}
}
