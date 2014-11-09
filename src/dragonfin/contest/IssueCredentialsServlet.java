package dragonfin.contest;

import dragonfin.contest.common.*;

import dragonfin.templates.Function;

import java.io.*;
import java.util.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class IssueCredentialsServlet extends CoreServlet
{
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
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
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
