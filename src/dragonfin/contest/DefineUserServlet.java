package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class DefineUserServlet extends CoreServlet
{
	String getTemplate() {
		return "define_user.tt";
	}

	boolean checkAuthorized(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String frmUsername = req.getParameter("id");
		String sesUsername = (String) req.getSession(false).getAttribute("username");

		if (isDirector(req, resp) || (
			frmUsername != null && sesUsername != null && frmUsername.equals(sesUsername))
		) {
			return false;
		}
		else {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Only the contest director can edit another user's properties");
			return true;
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		if (checkAuthorized(req, resp)) { return; }

		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException,ServletException
	{
		String contestId = tv.req.getParameter("contest");
		String username = tv.req.getParameter("id");

		if (username != null) {

			// editing an existing record

			TemplateVariables.User u = tv.fetchUser(contestId, username);
			ctx.put("user", u);

			Map<String,Object> form = new HashMap<String,Object>();
			form.put("username", u.username);
			form.put("name", u.name);
			form.put("description", u.description);
			form.put("ordinal", u.ordinal);

			form.put("is_director", u.is_director);
			form.put("is_judge", u.is_judge);
			form.put("is_contestant", u.is_contestant);
			form.put("visible", u.visible);

			ctx.put("f", form);
		}
		else {

			// creating a new record

			Map<String,Object> form = new HashMap<String,Object>();
			form.put("ordinal", "0");
			form.put("visible", Boolean.TRUE);
			form.put("is_contestant", Boolean.TRUE);

			ctx.put("f", form);
		}

		FormPermissions p = getFormPermissions(tv.req);
		ctx.put("can_change_name", p.can_change_name);
		ctx.put("can_change_description", p.can_change_description);
		ctx.put("can_change_password", p.can_change_password);
	}

	static class FormPermissions
	{
		public boolean can_change_name;
		public boolean can_change_description;
		public boolean can_change_password;
		public boolean can_change_ordinal;
		public boolean can_change_flags;
	}

	FormPermissions getFormPermissions(HttpServletRequest req)
		throws ServletException
	{
		FormPermissions p = new FormPermissions();

		String username = (String)req.getSession(false).getAttribute("username");
		if (username == null) {
			return p;
		}

		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Contest c;
		TemplateVariables.User u;
		try {
			c = tv.getContest();
			u = tv.fetchUser(c.id, username);
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("invalid contest or session");
		}

		if (u.is_director) {
			p.can_change_name = true;
			p.can_change_description = true;
			p.can_change_password = true;
			p.can_change_ordinal = true;
			p.can_change_flags = true;
			return p;
		}

		if (u.is_contestant) {
			p.can_change_name = p.can_change_name || c.contestants_can_change_name;
			p.can_change_description = p.can_change_description || c.contestants_can_change_description;
			p.can_change_password = p.can_change_password || c.contestants_can_change_password;
		}
		if (u.is_judge) {
			p.can_change_name = p.can_change_name || c.judges_can_change_name;
			p.can_change_password = p.can_change_password || c.judges_can_change_password;
		}
		return p;
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
		else if (req.getParameter("action:delete_user") != null) {
			doDeleteUser(req, resp);
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

	void doDeleteUser(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
	}

	void doUpdateUser(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		if (checkAuthorized(req, resp)) { return; }

		FormPermissions p = getFormPermissions(req);

		String contestId = req.getParameter("contest");
		String username = req.getParameter("id");

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
			updateFromForm(ent1, params, p);
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

		FormPermissions p = getFormPermissions(req);

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
			updateFromForm(userEnt, params, p);
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

	void updateFromForm(Entity ent1, Map<String,String> POST, FormPermissions p)
	{
		if (p.can_change_name) {
			ent1.setProperty("name", POST.get("name"));
		}
		if (p.can_change_description) {
			ent1.setProperty("description", POST.get("description"));
		}

		String tmpPass = POST.get("password");
		if (tmpPass != null && tmpPass.length() != 0 && p.can_change_password) {
			ent1.setProperty("password", tmpPass);
		}

		// integers
		if (p.can_change_ordinal) {
			updateFromForm_int(ent1, POST, "ordinal");
		}

		// booleans
		if (p.can_change_flags) {
			ent1.setProperty("is_director", Boolean.valueOf(POST.containsKey("is_director")));
			ent1.setProperty("is_judge", Boolean.valueOf(POST.containsKey("is_judge")));
			ent1.setProperty("is_contestant", Boolean.valueOf(POST.containsKey("is_contestant")));
			ent1.setProperty("visible", Boolean.valueOf(POST.get("visible")));
		}

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
