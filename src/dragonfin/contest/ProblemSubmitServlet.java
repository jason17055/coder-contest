package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import com.google.appengine.api.datastore.*;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.*;

public class ProblemSubmitServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_submit.tt";
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		doCreateSubmission(req, resp);
	}

	boolean checkSubmitAccess(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return true;
		}

		try {

		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);

		Date t = p.getEffective_start_time();
		if (t == null || t.after(tv.curTime)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
				"Oops. Problem start time is not defined or in the future."
				);
			return true;
		}

		Date t2 = p.getContest().getCurrent_phase_ends();
		if (t2 != null && t2.before(tv.curTime)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
				"Sorry, contest has ended. No more submissions can be accepted."
				);
			return true;
		}

		return false;

		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return true;
		}
	}

	void doCreateSubmission(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		if (checkProblemAccess(req, resp)) { return; }
		if (checkSubmitAccess(req, resp)) { return; }

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
		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData)
			req.getAttribute("POST");
		File sourceFile = POST.handleFileContent("source");

		if (sourceFile == null) {
			doFormError(req, resp, "Error: no file provided");
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
			ent.setProperty("type", "submission");
			ent.setProperty("problem", problemKey);
			ent.setProperty("contest", contestId);

			Key fileKey = KeyFactory.createKey("File", sourceFile.id);
			ent.setProperty("source", fileKey);

			submissionKey = ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		// enqueue a task for processing this new submission
		NewSubmissionTask.enqueueTask(submissionKey);

		String url = "..";
		resp.sendRedirect(url);
	}
}
