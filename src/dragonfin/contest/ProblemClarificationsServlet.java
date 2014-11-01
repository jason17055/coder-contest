package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;

public class ProblemClarificationsServlet extends ProblemCoreServlet
{
	public String getTemplate() {
		return "problem_clarifications.tt";
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		FileUploadFormHelper.FormData POST = uploadForm.processMultipartForm(req);
		doClarificationRequest(POST, req, resp);
	}

	void doClarificationRequest(FileUploadFormHelper.FormData POST, HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		//
		// check form parameters
		//
		String message = POST.get("message");
		if (message == null || message.equals("")) {
			doFormError(req, resp, "Error: type a message before clicking Submit");
			return;
		}

		// TODO- check permission to submit

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		Key userKey = getLoggedInUserKey(req);
		Key submissionKey;
		try {

			Entity ent = new Entity("Submission", userKey);

			ent.setProperty("created", new Date());
			ent.setProperty("type", "question");
			ent.setProperty("problem", problemKey);
			ent.setProperty("contest", contestId);

			ent.setProperty("question", message);

			submissionKey = ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		// enqueue a task for processing this new submission
		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(withUrl("/_task/new_submission")
			.param("submitter", userKey.getName())
			.param("submission", Long.toString(submissionKey.getId()))
			);

		String url = "..";
		resp.sendRedirect(url);
	}
}
