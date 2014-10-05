package dragonfin.contest;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ProblemTestServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_test.tt";
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	void doCreateSubmission(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		if (requireContest(req, resp)) { return; }

		String problemId = req.getParameter("problem");
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		// TODO- check parameters
		// TODO- check permission to submit solution to this problem at this time

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key userKey = getLoggedInUserKey(req);
			Entity ent = new Entity("Submission", userKey);

			ent.setProperty("created", new Date());

			String fileHash = POST.get("source_upload");
			if (fileHash != null) {
				Key fileKey = KeyFactory.createKey("File", fileHash);
				ent.setProperty("source", fileKey);
			}

			ent.setProperty("problem", problemKey);
			ent.setProperty("contest", contestId);
			ds.put(ent);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		String url = "..";
		resp.sendRedirect(url);
	}
}
