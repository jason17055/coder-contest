package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class EditClarificationServlet extends CoreServlet
{
	String getTemplate()
	{
		return "edit_clarification.tt";
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
			form.put("problem", s.getProblem());
			form.put("question", s.question);
			form.put("answer", s.answer);
			form.put("answer_type", s.answer_type);
			ctx.put("f", form);
		}
		else {
			String problemId = tv.req.getParameter("problem");

			HashMap<String,Object> form = new HashMap<String,Object>();
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
		else if (req.getParameter("id") != null) {
			// not implemented
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
		else {
			// new clarification
			doCreateClarification(req, resp);
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

	void doCreateClarification(HttpServletRequest req, HttpServletResponse resp)
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

		// TODO- check permission to submit question

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

			ent.setProperty("question", POST.get("question"));
			ent.setProperty("answer", POST.get("answer"));
			ent.setProperty("answer_type", POST.get("answer_type"));

			submissionKey = ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		doCancel(req, resp);
	}
}
