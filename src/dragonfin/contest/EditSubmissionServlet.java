package dragonfin.contest;

import dragonfin.contest.model.*;
import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class EditSubmissionServlet extends CoreServlet
{
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

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			String contestId = req.getParameter("contest");
			String id = req.getParameter("id");
			Key submissionKey = TemplateVariables.parseSubmissionId(contestId, id);
			Entity ent = ds.get(submissionKey);
			updateFromForm(ent, POST);
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

		doCancel(req, resp);
	}

	void updateFromForm(Entity ent, Map<String,String> POST)
	{
		ent.setProperty("status", POST.get("status"));
		updateFromFormInt(ent, POST, "minutes");
		updateFromForm_file(ent, POST, "source");
	}
}
