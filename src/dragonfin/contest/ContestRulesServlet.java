package dragonfin.contest;

import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

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

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "", null);
		}
		resp.sendRedirect(u);
	}

	void doUpdateRules(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			String contestId = req.getParameter("contest");
			Key contestKey = KeyFactory.createKey("Contest", contestId);

			Entity ent = ds.get(contestKey);
			ent.setProperty("title", req.getParameter("title"));
			ent.setProperty("subtitle", req.getParameter("subtitle"));
			ent.setProperty("logo", req.getParameter("logo"));

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
	}
}
