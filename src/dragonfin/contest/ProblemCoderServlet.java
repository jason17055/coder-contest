package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.defaultResultEntity;

public class ProblemCoderServlet extends ProblemCoreServlet
{
	public String getTemplate() {
		return "problem_write.tt";
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException, IOException
	{
		super.moreVars(tv, ctx);

		TemplateVariables.Result r = (TemplateVariables.Result) ctx.get("result");

		HashMap<String,Object> form = new HashMap<String,Object>();
		form.put("source_name", "Main.java");
		form.put("source_content", "");

		if (r != null) {
			File sourceFile = r.getSource();
			if (sourceFile != null) {
				form.put("source_name", sourceFile.name);
				form.put("source_content", sourceFile.getText_content());
			}
		}

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
				ent = defaultResultEntity(resultKey);
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
