package dragonfin.contest;

import dragonfin.contest.common.File;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
		form.put("phase0_name", c.phase0_name);
		form.put("phase1_name", c.phase1_name);
		form.put("phase2_name", c.phase2_name);
		form.put("phase3_name", c.phase3_name);
		form.put("phase4_name", c.phase4_name);
		form.put("phase0_ends", fromDate(c.phase0_ends));
		form.put("phase1_ends", fromDate(c.phase1_ends));
		form.put("phase2_ends", fromDate(c.phase2_ends));
		form.put("phase3_ends", fromDate(c.phase3_ends));
		form.put("phase4_ends", fromDate(c.phase4_ends));
		form.put("started", fromDate(c.started));

		form.put("yes_response", c.yes_response);
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

			ent.setProperty("started", asDate(req.getParameter("started")));

			ent.setProperty("phase0_name", req.getParameter("phase0_name"));
			ent.setProperty("phase1_name", req.getParameter("phase1_name"));
			ent.setProperty("phase2_name", req.getParameter("phase2_name"));
			ent.setProperty("phase3_name", req.getParameter("phase3_name"));
			ent.setProperty("phase4_name", req.getParameter("phase4_name"));

			ent.setProperty("phase0_ends", asDate(req.getParameter("phase0_ends")));
			ent.setProperty("phase1_ends", asDate(req.getParameter("phase1_ends")));
			ent.setProperty("phase2_ends", asDate(req.getParameter("phase2_ends")));
			ent.setProperty("phase3_ends", asDate(req.getParameter("phase3_ends")));
			ent.setProperty("phase4_ends", asDate(req.getParameter("phase4_ends")));

			ent.setProperty("yes_response", req.getParameter("yes_response"));
			ent.setProperty("no_responses", asStringList(req.getParameter("no_responses")));

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

		doCancel(req, resp);
	}

	static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static Date asDate(String s)
	{
		if (s == null || s.equals("")) {
			return null;
		}

		try {
			return DATE_FMT.parse(s);
		}
		catch (ParseException e) {
			return null;
		}
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

	static String fromDate(Date d)
	{
		if (d == null) {
			return null;
		}

		return DATE_FMT.format(d);
	}
}
