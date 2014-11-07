package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ListSubmissionsServlet extends CoreServlet
{
	boolean requireJudge(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return true; }

		TemplateVariables tv = makeTemplateVariables(req);
		try {
			TemplateVariables.User u = tv.fetchUser(getLoggedInUserKey(req));
			if (u.is_director || u.is_judge) {
				return false;
			}
		}
		catch (EntityNotFoundException e) {
		}

		// not a judge or director
		resp.sendError(HttpServletResponse.SC_FORBIDDEN,
			"This page requires director or judge access.");
		return true;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireJudge(req, resp)) { return; }

		renderTemplate(req, resp, "list_submissions.tt");
	}
}
