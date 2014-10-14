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

	void doCreateSubmission(HttpServletRequest req, HttpServletResponse resp)
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
