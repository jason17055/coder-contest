package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.MAX_PHASE_NUMBER;

public class DefineProblemServlet extends CoreServlet
{
	String getTemplate() {
		return "define_problem.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	protected void doFormError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
		throws IOException, ServletException
	{
		ArrayList<String> messages = new ArrayList<String>();
		messages.add(errorMessage);
		Map<String,Object> args = new HashMap<String,Object>();
		args.put("messages", messages);
		renderTemplate(req, resp, getTemplate(), args);
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws Exception
	{
		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("id");

		if (problemId != null) {

			// editing an existing record

			TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
			ctx.put("problem", p);

			Map<String,Object> form = new HashMap<String,Object>();
			form.put("name", p.name);
			form.put("judged_by", p.judged_by);
			form.put("difficulty", Integer.toString(p.difficulty));
			form.put("allocated_minutes", Integer.toString(p.allocated_minutes));
			form.put("runtime_limit", Integer.toString(p.runtime_limit));
			form.put("spec", p.getSpec());
			form.put("solution", p.getSolution());
			form.put("input_validator", p.getInput_validator());
			form.put("output_validator", p.getOutput_validator());
			form.put("scoreboard_image", p.scoreboard_image);
			form.put("input_is_text", p.input_is_text ? "1" : "");
			form.put("output_is_text", p.output_is_text ? "1" : "");
			form.put("score_by_access_time", p.score_by_access_time ? "1" : "");
			form.put("start_time", p.start_time);

			putPhaseOptions(form, "pp_scoreboard", p.pp_scoreboard);
			putPhaseOptions(form, "pp_read_problem", p.pp_read_problem);
			putPhaseOptions(form, "pp_submit", p.pp_submit);
			putPhaseOptions(form, "pp_read_opponent", p.pp_read_opponent);
			putPhaseOptions(form, "pp_challenge", p.pp_challenge);
			putPhaseOptions(form, "pp_read_solution", p.pp_read_solution);

			ctx.put("f", form);
		}
		else {

			// creating a new record

			Map<String,Object> form = new HashMap<String,Object>();

			form.put("input_is_text", "1");
			form.put("output_is_text", "1");
			form.put("pp_scoreboard_1", "on");
			form.put("pp_read_problem_1", "on");
			form.put("pp_submit_1", "on");

			ctx.put("f", form);
		}
	}

	void putPhaseOptions(Map<String,Object> form, String pp_name, boolean [] list)
	{
		if (list == null) {
			return;
		}

		for (int i = 0; i < list.length; i++) {
			if (list[i]) {
				form.put(pp_name+"_"+i, "on");
			}
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
		else if (POST.containsKey("action:create_problem")) {
			doCreateProblem(req, resp);
		}
		else if (POST.containsKey("action:delete_problem")) {
			doDeleteProblem(req, resp);
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

		//
		// check parameters
		//
		if (POST.get("name") == null || POST.get("name").length() == 0) {
			doFormError(req, resp, "Name is required.");
			return;
		}

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

	void updateFromForm(Entity ent1, Map<String,String> POST)
	{
		ent1.setProperty("name", POST.get("name"));
		ent1.setProperty("score_by_access_time", "Y".equals(POST.get("score_by_access_time")) ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("judged_by", POST.get("judged_by"));
		ent1.setProperty("input_is_text", new Boolean(POST.containsKey("input_is_text")));
		ent1.setProperty("output_is_text", new Boolean(POST.containsKey("output_is_text")));

		String t1 = POST.get("scoreboard_image");
		if (t1 != null && t1.length() != 0) {
			ent1.setProperty("scoreboard_image", t1);
		}
		else {
			ent1.setProperty("scoreboard_image", null);
		}

		updateFromFormInt(ent1, POST, "difficulty");
		updateFromFormInt(ent1, POST, "allocated_minutes");
		updateFromFormInt(ent1, POST, "runtime_limit");

		updateFromForm_file(ent1, POST, "spec");
		updateFromForm_file(ent1, POST, "solution");
		updateFromForm_file(ent1, POST, "input_validator");
		updateFromForm_file(ent1, POST, "output_validator");

		updateFromForm_phases(ent1, POST, "pp_scoreboard");
		updateFromForm_phases(ent1, POST, "pp_read_problem");
		updateFromForm_phases(ent1, POST, "pp_submit");
		updateFromForm_phases(ent1, POST, "pp_read_opponent");
		updateFromForm_phases(ent1, POST, "pp_challenge");
		updateFromForm_phases(ent1, POST, "pp_read_solution");
	}

	void updateFromForm_phases(Entity ent, Map<String,String> POST, String pp_option)
	{
		ArrayList<String> phases = new ArrayList<String>();
		for (int i = 0; i <= MAX_PHASE_NUMBER; i++) {
			String k = String.format("%s_%d", pp_option, i);
			if (POST.containsKey(k)) {
				phases.add(Integer.toString(i));
			}
		}
		ent.setProperty(pp_option, phases);
	}

	void doDeleteProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("id");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		//TODO- delete this problem's system tests, clarifications,
		// submissions

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key prbKey = KeyFactory.createKey(contestKey,
			"Problem", Long.parseLong(problemId));
		ds.delete(prbKey);

		doCancel(req, resp);
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

		//
		// check parameters
		//
		if (POST.get("name") == null || POST.get("name").length() == 0) {
			doFormError(req, resp, "Name is required.");
			return;
		}

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
