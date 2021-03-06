package dragonfin.contest;

import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.HtmlDateFormat.*;

public class ContestRulesServlet extends CoreServlet
{
	String getTemplate() {
		return "rules.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		TemplateVariables.Contest c = tv.fetchContest(contestId);

		Map<String,Object> form = new HashMap<String,Object>();
		form.put("title", c.title);
		form.put("subtitle", c.subtitle);
		form.put("logo", c.logo);
		form.put("contestants_can_change_name", c.contestants_can_change_name);
		form.put("contestants_can_change_description", c.contestants_can_change_description);
		form.put("contestants_can_change_password", c.contestants_can_change_password);
		form.put("contestants_can_write_code", c.contestants_can_write_code);
		form.put("judges_can_change_name", c.judges_can_change_name);
		form.put("judges_can_change_password", c.judges_can_change_password);
		form.put("collaboration", c.collaboration);
		form.put("score_system", c.score_system);
		form.put("scoreboard", c.scoreboard);
		form.put("scoreboard_images", c.scoreboard_images);
		form.put("scoreboard_popups", c.scoreboard_popups);
		form.put("scoreboard_order", c.scoreboard_order);
		form.put("scoreboard_fanfare", c.scoreboard_fanfare);
		form.put("phase1_name", c.phase1_name);
		form.put("phase2_name", c.phase2_name);
		form.put("phase3_name", c.phase3_name);
		form.put("phase4_name", c.phase4_name);
		form.put("phase1_ends", formatDateTime(c.phase1_ends, c.time_zone));
		form.put("phase2_ends", formatDateTime(c.phase2_ends, c.time_zone));
		form.put("phase3_ends", formatDateTime(c.phase3_ends, c.time_zone));
		form.put("phase4_ends", formatDateTime(c.phase4_ends, c.time_zone));
		form.put("started", formatDateTime(c.started, c.time_zone));
		form.put("time_zone", c.time_zone);

		{
			StringBuilder sb = new StringBuilder();
			for (String s : c.no_responses) {
				if (sb.length() != 0) { sb.append("\n"); }
				sb.append(s);
			}
			form.put("no_responses", sb.toString());
		}

		ctx.put("f", form);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:cancel") != null) {
			doCancel(req, resp);
		}
		else {
			doUpdateRules(req, resp);
		}
	}

	String getNextUrl(HttpServletRequest req)
	{
		String u = req.getParameter("next");
		if (u != null) {
			return u;
		}
		else {
			return makeContestUrl(req.getParameter("contest"), "controller", null);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		resp.sendRedirect(getNextUrl(req));
	}

	void doUpdateRules(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		if (requireDirector(req, resp)) { return; }

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		boolean phaseChanged = false;

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);

			Entity ent = ds.get(contestKey);
			String tz = (String) ent.getProperty("time_zone");
			if (tz == null || tz.equals("")) {
				tz = "UTC";
			}

			ent.setProperty("title", req.getParameter("title"));
			ent.setProperty("subtitle", req.getParameter("subtitle"));
			ent.setProperty("logo", req.getParameter("logo"));
			ent.setProperty("scoreboard", req.getParameter("scoreboard"));
			ent.setProperty("scoreboard_order", req.getParameter("scoreboard_order"));

			ent.setProperty("started", asDate(req, "started", tz));
			if (req.getParameter("started_set_to_now") != null) {
				ent.setProperty("started", new Date());
			}

			ent.setProperty("phase1_name", req.getParameter("phase1_name"));
			ent.setProperty("phase2_name", req.getParameter("phase2_name"));
			ent.setProperty("phase3_name", req.getParameter("phase3_name"));
			ent.setProperty("phase4_name", req.getParameter("phase4_name"));

			ent.setProperty("phase1_ends", asDate(req, "phase1_ends", tz));
			ent.setProperty("phase2_ends", asDate(req, "phase2_ends", tz));
			ent.setProperty("phase3_ends", asDate(req, "phase3_ends", tz));
			ent.setProperty("phase4_ends", asDate(req, "phase4_ends", tz));

			ent.setProperty("no_responses", asStringList(req.getParameter("no_responses")));

			int oldPhase = ent.hasProperty("current_phase") ?
				(int)((Long)ent.getProperty("current_phase")).longValue() :
				1;
			int newPhase = Integer.parseInt(req.getParameter("current_phase"));
			phaseChanged = oldPhase != newPhase;
			ent.setProperty("current_phase", newPhase);

			updateFromForm_boolean(ent, req, "contestants_can_change_name");
			updateFromForm_boolean(ent, req, "contestants_can_change_description");
			updateFromForm_boolean(ent, req, "contestants_can_change_password");
			updateFromForm_boolean(ent, req, "contestants_can_write_code");
			updateFromForm_boolean(ent, req, "judges_can_change_name");
			updateFromForm_boolean(ent, req, "judges_can_change_password");

			//TODO- the remaining parameters on this form.

			ds.put(ent);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (phaseChanged) {
			// recalculate all teams' scores
			UpdateScoreTask.enqueueTask_all(contestId);
		}

		doCancel(req, resp);
	}

	void updateFromForm_boolean(Entity ent, HttpServletRequest req, String propName)
	{
		ent.setProperty(propName, Boolean.valueOf(req.getParameter(propName) != null));
	}

	static Date asDate(HttpServletRequest req, String key, String timeZone)
	{
		return parseDateTime(req.getParameter(key), timeZone);
	}

	static List<String> asStringList(String s)
	{
		ArrayList<String> list = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new StringReader(s));
		try {
			String tmp;
			while ( (tmp = in.readLine()) != null )
			{
				tmp = tmp.trim();
				if (tmp.length() != 0) {
					list.add(tmp);
				}
			}
		}
		catch (IOException e) {
			// can't really happen with BufferedReader-StringReader
			throw new Error("Unexpected: "+e, e);
		}
		return list;
	}
}
