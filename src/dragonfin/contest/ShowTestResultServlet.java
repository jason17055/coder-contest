package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static dragonfin.contest.TemplateVariables.parseTestResultId;

public class ShowTestResultServlet extends CoreServlet
{
	boolean checkAccess(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String contestId = req.getParameter("contest");
		String testResultId = req.getParameter("id");
		Key key = parseTestResultId(contestId, testResultId);

		try {
			TemplateVariables tv = makeTemplateVariables(req);
			TemplateVariables.TestResult tr = tv.fetchTestResult(key);
			TemplateVariables.Submission s = tr.getSubmission();
			if (s.getCan_judge()) {
				// access ok
				return false;
			}
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return true;
		}

		resp.sendError(HttpServletResponse.SC_FORBIDDEN,
			"You do not have access to this submission."
			);
		return true;
	}

	String getTemplate()
	{
		return "show_test_result.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		if (checkAccess(req, resp)) { return; }

		renderTemplate(req, resp, getTemplate());
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		String testResultId = tv.req.getParameter("id");
		Key key = parseTestResultId(contestId, testResultId);
		TemplateVariables.TestResult tr = tv.fetchTestResult(key);
		ctx.put("test_result", tr);
	}
}
