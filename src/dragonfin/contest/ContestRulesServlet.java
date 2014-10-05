package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import dragonfin.contest.model.File;
import com.google.appengine.api.datastore.*;

public class ContestRulesServlet extends CoreServlet
{
	static final String TEMPLATE = "rules.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");

		Map<String,Object> form = new HashMap<String,Object>();
		{
			//TODO
		}

		Map<String,Object> args = new HashMap<String,Object>();
		args.put("f", form);
		renderTemplate(req, resp, TEMPLATE, args);
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
