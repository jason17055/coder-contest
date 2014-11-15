package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.makeSubmissionId;

public class EditClarificationServlet extends CoreServlet
{
	String getTemplate()
	{
		return "edit_clarification.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireJudge(req, resp)) { return; }

		//TODO- check that the judge can actually access this specific
		// submission

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
			doUpdateClarification(req, resp);
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
		if (requireJudge(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));
		Key userKey = getLoggedInUserKey(req);

		//
		// check form parameters
		//
		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData)
			req.getAttribute("POST");

		// check permission to create a clarification
		try {
		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.User judge = tv.fetchUser(userKey);
		TemplateVariables.Problem p = tv.fetchProblem(problemKey);
		if (!p.canJudge(judge)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
					"Not allowed to judge this problem.");
			return;
		}
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Entity unexpectedly missing from datastore", e);
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

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

	void doUpdateClarification(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireJudge(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String id = req.getParameter("id");
		if (contestId == null || id == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key submissionKey = TemplateVariables.parseSubmissionId(contestId, id);
		Key userKey = getLoggedInUserKey(req);

		//
		// TODO- check form parameters
		//
		FileUploadFormHelper.FormData POST = (FileUploadFormHelper.FormData)
			req.getAttribute("POST");

		// check permission to update a clarification
		try {
		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.User judge = tv.fetchUser(userKey);
		TemplateVariables.Submission s = tv.fetchSubmission(submissionKey);
		if (s.getJudge() != judge) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
					"Someone else is responding to this submission.");
			return;
		}
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Entity unexpectedly missing from datastore", e);
		}

		boolean answerChanged;
		boolean firstAnswer;
		boolean hadAnswer;

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		Entity ent;

		try {

			ent = ds.get(submissionKey);

			String oldAnswer = (String) ent.getProperty("answer");
			oldAnswer = oldAnswer != null ? oldAnswer : "";
			String newAnswer = POST.get("answer");
			answerChanged = !oldAnswer.equals(newAnswer);

			String oldAnswerType = (String) ent.getProperty("answer_type");
			oldAnswerType = oldAnswerType != null ? oldAnswerType : "";
			hadAnswer = oldAnswerType.length() != 0;
			String newAnswerType = POST.get("answer_type");
			firstAnswer = !oldAnswerType.equals(newAnswerType);

			ent.setProperty("question", POST.get("question"));
			ent.setProperty("answer", newAnswer);
			ent.setProperty("answer_type", newAnswerType);

			ds.put(ent);
			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (firstAnswer || (answerChanged && hadAnswer)) {
			notifyJudgment(ds, ent, firstAnswer);
		}

		doCancel(req, resp);
	}

	void notifyJudgment(DatastoreService ds, Entity subEnt, boolean firstJudgment)
	{
		Key userKey = subEnt.getKey().getParent();
		String contestId = (String) subEnt.getProperty("contest");
		Key problemKey = (Key) subEnt.getProperty("problem");

		String problemName;
		try {
			Entity problemEnt = ds.get(problemKey);
			problemName = (String) problemEnt.getProperty("name");
		}
		catch (EntityNotFoundException e) {
			problemName = "Problem "+Long.toString(problemKey.getId());
		}

		String message = firstJudgment ?
			String.format("Your question about %s has been answered.", problemName) :
			String.format("Your question about %s has a new answer.", problemName);

		String url = makeContestUrl(
			contestId, "clarification",
			String.format("id=%s", makeSubmissionId(subEnt.getKey()))
			);

		Entity messageEnt = new Entity("Message", userKey);
		messageEnt.setProperty("created", new Date());
		messageEnt.setProperty("message", message);
		messageEnt.setProperty("url", url);
		ds.put(messageEnt);
	}
}
