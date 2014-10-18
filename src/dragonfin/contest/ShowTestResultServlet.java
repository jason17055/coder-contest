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
	String getTemplate()
	{
		return "show_test_result.tt";
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
		String contestId = tv.req.getParameter("contest");
		String testResultId = tv.req.getParameter("id");
		Key key = parseTestResultId(contestId, testResultId);
		TemplateVariables.TestResult tr = tv.fetchTestResult(key);
		ctx.put("test_result", tr);
	}
}
