package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class DefineSystemTestServlet extends CoreServlet
{
	public String getTemplate()
	{
		return "define_system_test.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws Exception
	{
		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("problem");
		String testNumber = tv.req.getParameter("number");

		if (testNumber != null) {

			// editing an existing record

			TemplateVariables.SystemTest st = tv.fetchSystemTest(contestId, problemId, testNumber);
			ctx.put("system_test", st);
			ctx.put("problem", st.getProblem());

			Map<String,Object> form = new HashMap<String,Object>();
			form.put("input", st.getInput());
			form.put("expected", st.getExpected());
			form.put("sample", st.sample);
			form.put("auto_judge", st.auto_judge);
			ctx.put("f", form);
		}
		else {

			// creating a new record

			TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
			ctx.put("problem", p);

			Map<String,Object> form = new HashMap<String,Object>();
			form.put("problem", p);
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
		else if (POST.containsKey("action:create_system_test") ||
			POST.containsKey("action:create_system_test_and_repeat")) {
			doCreateSystemTest(req, resp);
		}
		else if (POST.containsKey("action:delete_system_test")) {
			doDeleteSystemTest(req, resp);
		}
		else {
			doUpdateSystemTest(req, resp);
		}
	}

	String getNextUrl(HttpServletRequest req)
	{
		String u = req.getParameter("next");
		if (u != null) {
			return u;
		}

		String t = req.getParameter("problem");
		if (t != null) {
			return makeContestUrl(req.getParameter("contest"),
				"problem", "id="+escapeUrl(t)
				);
		}

		return makeContestUrl(req.getParameter("contest"), "problems", null);
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		resp.sendRedirect(getNextUrl(req));
	}

	void doDeleteSystemTest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		String testNumber = req.getParameter("number");
		if (contestId == null || problemId == null || testNumber == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key prbKey = KeyFactory.createKey(contestKey,
			"Problem", Long.parseLong(problemId));
		Key testKey = KeyFactory.createKey(prbKey,
			"SystemTest", Long.parseLong(testNumber));

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		//TODO- delete TestResults that reference this system test

		ds.delete(testKey);
		doCancel(req, resp);
	}

	void doCreateSystemTest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));
			Entity problemEnt = ds.get(problemKey);

			long testId = problemEnt.hasProperty("last_system_test_id") ?
				((Long)problemEnt.getProperty("last_system_test_id")).longValue() : 0;
			testId++;
			problemEnt.setProperty("last_system_test_id", new Long(testId));
			ds.put(problemEnt);

			Key systestKey = KeyFactory.createKey(problemKey,
				"SystemTest", testId);
			Entity ent = new Entity(systestKey);
			ent.setProperty("created", new Date());
			updateFromForm(ent, POST);
			ds.put(ent);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (POST.containsKey("action:create_system_test_and_repeat")) {
			resp.sendRedirect(getMyUrl(req));
			return;
		}

		doCancel(req, resp);
	}

	void updateFromForm(Entity ent, Map<String,String> POST)
	{
		boolean isSample = POST.containsKey("sample");
		boolean isAutoJudge = POST.containsKey("auto_judge");

		ent.setProperty("sample", new Boolean(isSample));
		ent.setProperty("auto_judge", new Boolean(isAutoJudge));

		updateFromForm_file(ent, POST, "input");
		updateFromForm_file(ent, POST, "expected");
	}

	void doUpdateSystemTest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		String testNumber = req.getParameter("number");
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", Long.parseLong(problemId));
			Key testKey = KeyFactory.createKey(prbKey,
				"SystemTest", Long.parseLong(testNumber));

			Entity ent1 = ds.get(testKey);
			updateFromForm(ent1, POST);
			ds.put(ent1);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
				"Invalid System Test.");
			return;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		doCancel(req, resp);
	}
}
