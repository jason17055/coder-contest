package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public abstract class ProblemCoreServlet extends CoreServlet
{
	public final void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	protected void doFormError(HttpServletRequest req, HttpServletResponse resp, String errorMessage)
		throws IOException, ServletException
	{
		ArrayList<String> messages = new ArrayList<String>();
		messages.add(errorMessage);
		Map<String,Object> args = new HashMap<String,Object>();
		args.put("messages", messages);
		renderTemplate(req, resp, getTemplate(), args);
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException, IOException
	{
		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("problem");
		Key userKey = getLoggedInUserKey(tv.req);
		Key resultKey = KeyFactory.createKey(userKey, "Result", Long.parseLong(problemId));

		TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
		ctx.put("problem", p);

		try {
			TemplateVariables.Result r = tv.fetchResult(resultKey);
			ctx.put("result", r);
		}
		catch (EntityNotFoundException e) {
			// safe to ignore
			ctx.put("result", null);
		}
	}

	public abstract String getTemplate();
}
