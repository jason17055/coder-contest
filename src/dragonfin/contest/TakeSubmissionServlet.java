package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class TakeSubmissionServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireJudge(req, resp)) { return; }

		try {

		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.User judge = tv.fetchUser(getLoggedInUserKey(req));
		TemplateVariables.Submission s = tv.fetchSubmission(
				req.getParameter("contest"),
				req.getParameter("id")
				);

		// check whether this user can judge the problem
		TemplateVariables.Problem p = s.getProblem();
		if (!p.canJudge(judge)) {
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
					"Not allowed to judge this problem.");
			return;
		}

		// check whether the submission already has a judge
		TemplateVariables.User oldJudge = s.getJudge();
		if (oldJudge == null) {
			doOwnerChange(s, judge);
		}
		else if (req.getParameter("steal") != null && !oldJudge.equals(judge)) {
			doOwnerChange(s, judge);
		}

		String url = s.getEdit_url();
		if (req.getParameter("next") != null) {
			url += "&next=" + escapeUrl(req.getParameter("next"));
		}
		resp.sendRedirect(url);

		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
	}

	void doOwnerChange(TemplateVariables.Submission s, TemplateVariables.User j)
		throws EntityNotFoundException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Entity ent = ds.get(s.dsKey);

			ent.setProperty("judge", j.dsKey);

			ds.put(ent);
			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
