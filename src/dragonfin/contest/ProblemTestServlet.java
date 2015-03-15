package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Date;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.*;

public class ProblemTestServlet extends ProblemCoreServlet
{
	@Override
	public String getTemplate()
	{
		return "problem_test.tt";
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws IOException, EntityNotFoundException
	{
		super.moreVars(tv, ctx);

		HashMap<String,Object> form = new HashMap<String,Object>();
		if (tv.req.getParameter("source") != null) {
			try {
				File fileObj = tv.fetchFile(tv.req.getParameter("source"));
				form.put("source", fileObj);
			}
			catch (EntityNotFoundException e) {
				// safe to ignore
			}
		}
		if (tv.req.getParameter("input") != null) {
			try {
				File fileObj = tv.fetchFile(tv.req.getParameter("input"));
				form.put("input", fileObj);
			}
			catch (EntityNotFoundException e) {
				// safe to ignore
			}
		}
		ctx.put("f", form);
	}

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
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData) req.getAttribute("POST");
		File sourceFile = POST.handleFileContent("source");
		File inputFile = POST.handleFileContent("input");

		// TODO- check parameters
		if (sourceFile == null) {
			doFormError(req, resp, "Error: source file must be provided.");
			return;
		}

		if (!isAcceptedFileType(req, sourceFile)) {
			doFormError(req, resp, "Error: source file is not of an accepted type.");
			return;
		}

		// TODO- check permission

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key userKey = getLoggedInUserKey(req);
			Entity ent = new Entity("TestJob");

			ent.setProperty("created", new Date());

			if (sourceFile != null) {
				Key fileKey = KeyFactory.createKey("File", sourceFile.id);
				ent.setProperty("source", fileKey);
				ent.setProperty("source_extension", fileExtensionOf(sourceFile.name));
			}
			if (inputFile != null) {
				Key fileKey = KeyFactory.createKey("File", inputFile.id);
				ent.setProperty("input", fileKey);
			}

			ent.setProperty("user", userKey);
			ent.setProperty("contest", contestId);
			ent.setProperty("type", "U");
			ent.setProperty("claimed", Boolean.FALSE);
			ent.setProperty("finished", Boolean.FALSE);
			ent.setProperty("owner", null);
			ent.setProperty("problem", problemKey);
			Key jobKey = ds.put(ent);

			txn.commit();

			JobBroker.notifyNewJob(jobKey);

			String jobId = Long.toString(jobKey.getId());
			String url = makeContestUrl(contestId, "problem."+problemId+"/test_result?id="+escapeUrl(jobId));
			resp.sendRedirect(url);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
