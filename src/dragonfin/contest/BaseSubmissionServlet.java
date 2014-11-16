package dragonfin.contest;

import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.parseSubmissionId;

public abstract class BaseSubmissionServlet extends CoreServlet
{
	boolean checkAccess(HttpServletRequest req, HttpServletResponse resp, boolean modifyAccess)
		throws IOException
	{
		try {

		String contestId = req.getParameter("contest");
		String id = req.getParameter("id");
		if (contestId == null || id == null) {

			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return true;
		}

		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Submission s = tv.fetchSubmission(contestId, id);
		TemplateVariables.User user = tv.fetchUser(getLoggedInUserKey(req));

		if (modifyAccess) {
			// to modify, the user must be the "judge" of this submission
			if (s.getJudge() == user) {
				return false;
			}

			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
				"Someone else is responding to this submission.");
			return true;
		}

		// check submitter access
		if (s.getSubmitterKey().equals(getLoggedInUserKey(req))) {

			// ok, the submitter can see their own submission
			return false;
		}

		// check judge access
		if (s.getCan_judge()) {

			return false;
		}

		resp.sendError(HttpServletResponse.SC_FORBIDDEN,
			"You do not have access to this submission."
			);
		return true;

		} //end try
		catch (EntityNotFoundException e) {


			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not Found");
			return true;
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		maybeReleaseSubmission(req);

		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "submissions", null);
		}
		resp.sendRedirect(u);
	}

	void maybeReleaseSubmission(HttpServletRequest req)
		throws ServletException
	{
		String contestId = req.getParameter("contest");
		String id = req.getParameter("id");

		Key userKey = getLoggedInUserKey(req);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		try {

			Entity ent = ds.get(parseSubmissionId(contestId, id));
			String answerType = (String) ent.getProperty("answer_type");
			Key judgeKey = (Key) ent.getProperty("judge");

			if ((answerType == null || answerType.equals(""))
				&&
				(judgeKey != null && judgeKey.equals(userKey)))
			{
				ent.setProperty("judge", null);
				ds.put(ent);
			}

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Unexpectedly missing some entities in datastore", e);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
