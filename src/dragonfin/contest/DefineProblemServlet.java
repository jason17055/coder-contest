package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import dragonfin.contest.model.File;
import com.google.appengine.api.datastore.*;

public class DefineProblemServlet extends CoreServlet
{
	static final String TEMPLATE = "define_problem.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("id");

		if (requireDirector(req, resp)) { return; }

		Map<String,Object> form = new HashMap<String,Object>();
		if (problemId != null) {
			try {
			ProblemInfo prb = DataHelper.loadProblem(contestId, problemId);
			form.put("name", prb.name);
			form.put("visible", prb.visible ? "1" : "");
			form.put("allow_submissions", prb.allow_submissions ? "1" : "");
			form.put("judged_by", prb.judged_by);
			form.put("difficulty", Integer.toString(prb.difficulty));
			form.put("allocated_minutes", Integer.toString(prb.allocated_minutes));
			form.put("runtime_limit", Integer.toString(prb.runtime_limit));
			form.put("spec", prb.spec);
			form.put("solution", prb.solution);
			form.put("scoreboard_image", prb.scoreboard_image);
			form.put("score_by_access_time", prb.score_by_access_time ? "1" : "");
			form.put("start_time", prb.start_time);

			} catch (DataHelper.NotFound e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		Map<String,Object> args = new HashMap<String,Object>();
		args.put("f", form);
		renderTemplate(req, resp, TEMPLATE, args);
	}

	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);

		if (POST.containsKey("action:cancel")) {
			doCancel(req, resp);
		}
		else if (POST.containsKey("action:create_problem")) {
			doCreateProblem(req, resp);
		}
		else {
			doUpdateProblem(req, resp);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "problems", null);
		}
		resp.sendRedirect(u);
	}

	void doCreateProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");
		String problemName = POST.get("name");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity contestEnt = ds.get(contestKey);

			long problemId = contestEnt.hasProperty("last_problem_id") ?
				((Long)contestEnt.getProperty("last_problem_id")).longValue() : 0;
			problemId++;
			contestEnt.setProperty("last_problem_id", new Long(problemId));
			ds.put(contestEnt);

			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", problemId);
			Entity ent1 = new Entity(prbKey);
			ent1.setProperty("created", new Date());
			updateFromForm(ent1, POST);
			ds.put(ent1);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Invalid contest", e);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		doCancel(req, resp);
	}

	void updateFromFormInt(Entity ent1, Map<String,String> POST, String propName)
	{
		if (POST.containsKey(propName)) {
			try {
			long x = Long.parseLong(POST.get(propName));
			ent1.setProperty(propName, new Long(x));
			}
			catch (NumberFormatException e) {
				ent1.removeProperty(propName);
			}
		}
	}

	void updateFromForm(Entity ent1, Map<String,String> POST)
	{
		ent1.setProperty("name", POST.get("name"));
		ent1.setProperty("visible", POST.containsKey("visible") ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("allow_submissions", POST.containsKey("allow_submissions") ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("score_by_access_time", "Y".equals(POST.get("score_by_access_time")) ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("judged_by", POST.get("judged_by"));

		updateFromFormInt(ent1, POST, "difficulty");
		updateFromFormInt(ent1, POST, "allocated_minutes");
		updateFromFormInt(ent1, POST, "runtime_limit");
	}

	void doUpdateProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("id");
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");
		String problemName = POST.get("name");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", Long.parseLong(problemId));
			Entity ent1 = ds.get(prbKey);
			updateFromForm(ent1, POST);
			ds.put(ent1);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Invalid contest", e);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		doCancel(req, resp);
	}
}
