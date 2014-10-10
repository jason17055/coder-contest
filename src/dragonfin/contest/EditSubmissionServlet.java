package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

public class EditSubmissionServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(EditSubmissionServlet.class.getName());

	String getTemplate()
	{
		return "edit_submission.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		renderTemplate(req, resp, getTemplate());
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		String id = tv.req.getParameter("id");
		ctx.put("submission", tv.fetchSubmission(contestId, id));
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		if (POST.containsKey("action:cancel")) {
			doCancel(req, resp);
		}
		else {
			doUpdateSubmission(req, resp);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "submissions", null);
		}
		resp.sendRedirect(u);
	}

	void doUpdateSubmission(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		boolean statusChanged;

		String contestId = req.getParameter("contest");
		String id = req.getParameter("id");
		Key submissionKey = TemplateVariables.parseSubmissionId(contestId, id);
		Key userKey = submissionKey.getParent();

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		Key problemKey;
		try {
			Entity ent = ds.get(submissionKey);

			problemKey = (Key) ent.getProperty("problem");

			String oldStatus = (String) ent.getProperty("status");
			if (oldStatus == null) { oldStatus = ""; }

			updateFromForm(ent, POST);

			String newStatus = (String) ent.getProperty("status");
			statusChanged = !oldStatus.equals(newStatus);

			ds.put(ent);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (statusChanged) {
			// enqueue a task for processing this submission status change
			Queue taskQueue = QueueFactory.getDefaultQueue();
			taskQueue.add(UpdateResultTask.makeUrl()
				.param("user", userKey.getName())
				.param("problem", Long.toString(problemKey.getId()))
				);
		}

		doCancel(req, resp);
	}

	void updateFromForm(Entity ent, Map<String,String> POST)
	{
		ent.setProperty("status", POST.get("status"));
		updateFromFormInt(ent, POST, "minutes");
		updateFromForm_file(ent, POST, "source");
	}
}
