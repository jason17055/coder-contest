package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static dragonfin.contest.TemplateVariables.makeSubmissionId;
import static dragonfin.contest.TemplateVariables.parseSubmissionId;

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
		if (id != null) {
			TemplateVariables.Submission s = tv.fetchSubmission(contestId, id);
			ctx.put("submission", s);

			HashMap<String,Object> form = new HashMap<String,Object>();
			form.put("submitter", s.getSubmitter());
			form.put("problem", s.getProblem());
			ctx.put("f", form);
		}
		else {
			String submitterUsername = tv.req.getParameter("submitter");
			String problemId = tv.req.getParameter("problem");

			HashMap<String,Object> form = new HashMap<String,Object>();
			form.put("submitter", tv.fetchUser(contestId, submitterUsername));
			form.put("problem", tv.fetchProblem(contestId, problemId));
			ctx.put("f", form);
		}
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		if (POST.containsKey("action:cancel")) {
			doCancel(req, resp);
		}
		else if (POST.containsKey("action:delete_submission")) {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, "Not Implemented");
		}
		else if (POST.containsKey("action:create_submission")) {
			doCreateSubmission(req, resp);
		}
		else if (POST.containsKey("action:redo_tests")) {
			doRedoTests(req, resp);
		}
		else {
			doUpdateSubmission(req, resp);
		}
	}

	void doRedoTests(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String submissionId = req.getParameter("id");
		if (contestId == null || submissionId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// enqueue a task for processing the submission
		Key submissionKey = parseSubmissionId(contestId, submissionId);
		NewSubmissionTask.enqueueTask(submissionKey);

		// show the current page again
		resp.sendRedirect(getMyUrl(req));
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

	void doCreateSubmission(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		if (requireDirector(req, resp)) { return; }

		// url parameters
		String contestId = req.getParameter("contest");
		String submitterUsername = req.getParameter("submitter");
		String problemId = req.getParameter("problem");

		Key userKey = TemplateVariables.makeUserKey(contestId, submitterUsername);
		Key problemKey = TemplateVariables.makeProblemKey(contestId, problemId);

		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData)req.getAttribute("POST");

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity ent = new Entity("Submission", userKey);
		ent.setProperty("created", new Date());
		ent.setProperty("type", "submission");
		ent.setProperty("problem", problemKey);
		ent.setProperty("contest", contestId);

		updateFromForm(ent, POST);

		Key submissionKey = ds.put(ent);

		// enqueue a task for processing this new submission
		NewSubmissionTask.enqueueTask(submissionKey);

		// return user to where they came from
		doCancel(req, resp);
	}

	void doUpdateSubmission(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		boolean statusChanged;
		boolean firstJudgment;

		String contestId = req.getParameter("contest");
		String id = req.getParameter("id");
		Key submissionKey = TemplateVariables.parseSubmissionId(contestId, id);
		Key userKey = submissionKey.getParent();

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		Entity ent;
		Key problemKey;
		try {
			ent = ds.get(submissionKey);

			problemKey = (Key) ent.getProperty("problem");

			String oldStatus = (String) ent.getProperty("status");
			if (oldStatus == null) {
				firstJudgment = true;
				oldStatus = "";
			}
			else {
				firstJudgment = false;
			}

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

			// send a message to the submitter
			notifyJudgment(ds, ent, firstJudgment);

			// enqueue a task for processing this submission status change
			Queue taskQueue = QueueFactory.getDefaultQueue();
			taskQueue.add(UpdateResultTask.makeUrl()
				.param("user", userKey.getName())
				.param("problem", Long.toString(problemKey.getId()))
				);
		}

		doCancel(req, resp);
	}

	void notifyJudgment(DatastoreService ds, Entity subEnt, boolean firstJudgment)
	{
		Key userKey = subEnt.getKey().getParent();
		String contestId = (String) subEnt.getProperty("contest");
		Key problemKey = (Key) subEnt.getProperty("problem");

		String problemName;
		try {
			Entity problemEnt = ds.get(problemKey);
			problemName = (String) problemEnt.getProperty("name");
		}
		catch (EntityNotFoundException e) {
			problemName = "Problem "+Long.toString(problemKey.getId());
		}

		String message = firstJudgment ?
			String.format("Your solution for %s has been judged.", problemName) :
			String.format("Your solution for %s has been re-judged.", problemName);

		String url = makeContestUrl(
			contestId, "submission",
			String.format("id=%s", makeSubmissionId(subEnt.getKey()))
			);

		Entity messageEnt = new Entity("Message", userKey);
		messageEnt.setProperty("created", new Date());
		messageEnt.setProperty("message", message);
		messageEnt.setProperty("url", url);
		ds.put(messageEnt);
	}

	void updateFromForm(Entity ent, Map<String,String> POST)
	{
		ent.setProperty("status", POST.get("status"));
		updateFromFormInt(ent, POST, "minutes");
		updateFromForm_file(ent, POST, "source");
	}
}
