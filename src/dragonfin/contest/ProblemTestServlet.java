package dragonfin.contest;

import java.io.IOException;
import java.io.PrintWriter;
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

		doCreateTestJob(req, resp);
	}

	void doCreateTestJob(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		// TODO- check parameters
		// TODO- check permission

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key userKey = getLoggedInUserKey(req);
			Entity ent = new Entity("TestJob");

			ent.setProperty("created", new Date());

			String fileHash = POST.get("source_upload");
			if (fileHash != null) {
				Key fileKey = KeyFactory.createKey("File", fileHash);
				ent.setProperty("source", fileKey);
			}
			String fileHash2 = POST.get("input_upload");
			if (fileHash2 != null) {
				Key fileKey = KeyFactory.createKey("File", fileHash2);
				ent.setProperty("input", fileKey);
			}

			ent.setProperty("user", userKey);
			ent.setProperty("contest", contestId);
			ent.setProperty("type", "U");
			ent.setProperty("claimed", Boolean.FALSE);
			ent.setProperty("finished", Boolean.FALSE);
			ent.setProperty("problem", problemKey);
			Key resultKey = ds.put(ent);

			txn.commit();

			String jobId = Long.toString(resultKey.getId());

			JobBroker.notifyNewJob(jobId);

			String url = req.getContextPath()+"/"+contestId+"/problem."+problemId+"/test_result?id="+escapeUrl(jobId);
			resp.sendRedirect(url);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
