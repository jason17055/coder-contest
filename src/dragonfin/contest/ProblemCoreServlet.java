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
		if (checkProblemAccess(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	boolean checkProblemAccess(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return true;
		}

		try {

		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Contest c = tv.fetchContest(contestId);
		int curPhase = c.current_phase_id;

		TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
		if (!p.onScoreboard(curPhase)) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return true;
		}

		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return true;
		}

		return false;
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
