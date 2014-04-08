package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import dragonfin.contest.model.File;
import com.google.appengine.api.datastore.*;

public class DefineUserServlet extends CoreServlet
{
	static final String TEMPLATE = "define_user.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String userId = req.getParameter("id");

		if (requireDirector(req, resp)) { return; }

		Map<String,Object> form = new HashMap<String,Object>();
		if (userId != null) {
			try {
			UserInfo u = DataHelper.loadUser(contestId, userId);

			form.put("username", u.username);
			form.put("name", u.name);
			form.put("description", u.description);
			form.put("ordinal", u.ordinal);
			form.put("contest", u.contest);

			form.put("is_director", u.is_director);
			form.put("is_judge", u.is_judge);
			form.put("is_contestant", u.is_contestant);

			} catch (DataHelper.NotFound e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
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
		else if (req.getParameter("action:create_user") != null) {
			doCreateUser(req, resp);
		}
		else {
			doUpdateUser(req, resp);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "users", null);
		}
		resp.sendRedirect(u);
	}

	void doUpdateUser(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String username = req.getParameter("username");

		// TODO- check parameters

		HashMap<String,String> params = new HashMap<String,String>();
		for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements();)
		{
			String k = e.nextElement();
			params.put(k, req.getParameter(k));
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		try {

			Key userKey = KeyFactory.createKey("User",
				contestId+"/"+username);
			Entity ent1 = ds.get(userKey);
			updateFromForm(ent1, params);
			ds.put(ent1);

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

		doCancel(req, resp);
	}

	void doCreateUser(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String username = req.getParameter("username");

		// TODO- check parameters

		HashMap<String,String> params = new HashMap<String,String>();
		for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements();)
		{
			String k = e.nextElement();
			params.put(k, req.getParameter(k));
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		try {

			Key userKey = KeyFactory.createKey("User", contestId+"/"+username);
			Entity userEnt = new Entity(userKey);

			userEnt.setProperty("created", new Date());
			userEnt.setProperty("contest", contestId);
			userEnt.setProperty("username", username);
			updateFromForm(userEnt, params);
			ds.put(userEnt);

			txn.commit();
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
		ent1.setProperty("description", POST.get("description"));

		String tmpPass = POST.get("password");
		if (tmpPass != null && tmpPass.length() != 0) {
			ent1.setProperty("password", tmpPass);
		}

		// booleans
		ent1.setProperty("is_director", Boolean.valueOf(POST.containsKey("is_director")));
		ent1.setProperty("is_judge", Boolean.valueOf(POST.containsKey("is_judge")));
		ent1.setProperty("is_contestant", Boolean.valueOf(POST.containsKey("is_contestant")));

		updateFromForm_int(ent1, POST, "ordinal");
	}

	void updateFromForm_int(Entity ent1, Map<String,String> POST, String propName)
	{
		String s = POST.get(propName);
		if (s != null && s.length() != 0) {
			try {
			long x = Long.parseLong(s);
			ent1.setProperty(propName, new Long(x));
			}
			catch (NumberFormatException e) {
				ent1.removeProperty(propName);
			}
		}
		else {
			ent1.removeProperty(propName);
		}
	}
}
