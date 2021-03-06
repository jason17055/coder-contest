package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class DefineContestServlet extends AdminPageServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireAdmin(req, resp)) {
			return;
		}

		renderTemplate(req, resp, getTemplate());
	}

	String getTemplate() {
		return "admin/define_contest.tt";
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		if (contestId != null) {

			TemplateVariables.Contest c = tv.fetchContest(contestId);
			HashMap<String,String> form = new HashMap<String,String>();
			form.put("id", c.id);
			form.put("auth_external", c.auth_external);
			form.put("time_zone", c.time_zone);
			ctx.put("f", form);
		}
		else {

			// default form
			HashMap<String,String> form = new HashMap<String,String>();
			ctx.put("f", form);
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:create_contest") != null) {
			doCreateContest(req, resp);
		}
		else if (req.getParameter("action:update_contest") != null) {
			doUpdateContest(req, resp);
		}
		else if (req.getParameter("action:cancel") != null) {
			doCancel(req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "invalid POST");
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		resp.sendRedirect(getNextUrl(req));
	}

	void doCreateContest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String directorName = req.getParameter("director_username");
		String directorPassword = req.getParameter("password");
		String externalAuthStr = req.getParameter("auth_external");
		String timeZoneStr = req.getParameter("time_zone");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		TransactionOptions options = TransactionOptions.Builder.withXG(true);
		Transaction txn = ds.beginTransaction(options);

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity ent1 = new Entity(contestKey);
			ent1.setProperty("created", new Date());
			ent1.setProperty("created_by", req.getUserPrincipal().getName());
			ent1.setProperty("yes_response", "Yes");
			ent1.setProperty("no_responses", Arrays.asList(new String[] {
				"Wrong Answer",
				"Output Format Error",
				"Incomplete Output",
				"Excessive Output"
				}));
			ent1.setProperty("auth_external", externalAuthStr);
			ent1.setProperty("time_zone", timeZoneStr);
			ds.put(ent1);

			Key userKey = KeyFactory.createKey(
					"User", contestId+"/"+directorName);
			Entity ent2 = new Entity(userKey);
			ent2.setProperty("contest", contestId);
			ent2.setProperty("name", directorName);
			ent2.setProperty("password", directorPassword);
			ent2.setProperty("is_director", Boolean.TRUE);
			ent2.setProperty("created_by_method", "DefineContestServlet");
			ds.put(ent2);

			txn.commit();

			resp.sendRedirect(getNextUrl(req));
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	void doUpdateContest(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String directorName = req.getParameter("director_username");
		String directorPassword = req.getParameter("password");
		String externalAuthStr = req.getParameter("auth_external");
		String timeZoneStr = req.getParameter("time_zone");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		TransactionOptions options = TransactionOptions.Builder.withXG(true);
		Transaction txn = ds.beginTransaction(options);

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity ent1 = ds.get(contestKey);
			ent1.setProperty("auth_external", externalAuthStr);
			ent1.setProperty("time_zone", timeZoneStr);
			ds.put(ent1);

			if (directorName != null &&
				directorName.length() != 0 &&
				directorPassword != null &&
				directorPassword.length() != 0)
			{
				Key userKey = KeyFactory.createKey(
					"User", contestId+"/"+directorName);
				Entity ent2;
				try {
					ent2 = ds.get(userKey);
				}
				catch (EntityNotFoundException e) {
					ent2 = new Entity(userKey);
				}
				ent2.setProperty("contest", contestId);
				ent2.setProperty("name", directorName);
				ent2.setProperty("password", directorPassword);
				ent2.setProperty("is_director", Boolean.TRUE);
				ds.put(ent2);
			}

			txn.commit();

			resp.sendRedirect(getNextUrl(req));
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

	String getNextUrl(HttpServletRequest req)
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = req.getContextPath()+"/_admin/contests";
		}
		return u;
	}
}
