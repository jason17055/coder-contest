package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ProblemCoderServlet extends CoreServlet
{
	public String getTemplate() {
		return "problem_write.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("problem");

		TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
		ctx.put("problem", p);

		HashMap<String,Object> form = new HashMap<String,Object>();
		form.put("source_name", "Main.java");
		form.put("source_content", "Hello world.");
		ctx.put("f", form);
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		doSaveCode(req, resp);
	}

	void doSaveCode(HttpServletRequest req, HttpServletResponse resp)
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
		Key userKey = getLoggedInUserKey(req);
		Key resultKey = KeyFactory.createKey(userKey, "Result", Long.parseLong(problemId));

		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData) req.getAttribute("POST");
		File sourceFile = POST.handleFileContent("source");
		Key sourceFileKey = KeyFactory.createKey("File", sourceFile.id);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Entity ent;
			try {
				ent = ds.get(resultKey);
			}
			catch (EntityNotFoundException e) {
				ent = new Entity(resultKey);
				ent.setProperty("opened", new Date());
			}

			ent.setProperty("source", sourceFileKey);
			ds.put(ent);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		String url = req.getContextPath()+"/"+contestId+"/problem."+problemId+"/write";
		resp.sendRedirect(url);
	}
}
