package dragonfin.contest;

import dragonfin.contest.common.*;

import dragonfin.templates.Function;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.makeUserKey;

public class IssueCredentialsServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(IssueCredentialsServlet.class.getName());

	String getTemplate()
	{
		return "issue_credentials.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		renderTemplate(req, resp, getTemplate());
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		ctx.put("generate_password", new Function() {
			public Object invoke(Bindings args) {
				return generatePassword(5);
			}});
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:cancel") != null) {
			doCancel(req, resp);
		}
		else {
			doSetPasswords(req, resp);
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "users", null);
		}
		resp.sendRedirect(u);
	}

	void doSetPasswords(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		for (Enumeration<String> e = req.getParameterNames(); e.hasMoreElements(); ) {
			String pname = e.nextElement();
			if (pname.startsWith("reset:")) {
				String username = pname.substring(6);
				String pass = req.getParameter("password:"+username);
				doSetOnePassword(ds, contestId, username, pass);
			}
		}

	}

	void doSetOnePassword(DatastoreService ds, String contestId, String username, String pass)
		throws ServletException
	{
		log.info("resetting password for "+username);

		Key userKey = makeUserKey(contestId, username);

		Transaction txn = ds.beginTransaction();

		try {

			Entity ent = ds.get(userKey);
			ent.setProperty("password", pass);
			ds.put(ent);
			txn.commit();
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Entity unexpectedly not found in datastore.");
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	static final String PASSWORD_CHARS = "2346789bcdfghjkmnpqrtvwxyzBCDFGHJKLMNPQRTVWXYZ";
	static String generatePassword(int len)
	{
		char [] candidates = PASSWORD_CHARS.toCharArray();
		char [] pw = new char[len];

		assert len <= candidates.length;

		for (int i = 0; i < len; i++) {
			int j = (int)Math.floor(Math.random()*(candidates.length-i));
			pw[i] = candidates[j];
			candidates[j] = candidates[candidates.length-1-i];
		}
		return new String(pw);
	}
}
