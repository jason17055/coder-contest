package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.TemplateVariables.makeResultKey;
import static dragonfin.contest.TemplateVariables.defaultResultEntity;

public class StartProblemServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");
		if (contestId == null || problemId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String showMode;

		TemplateVariables tv = makeTemplateVariables(req);
		try {

			TemplateVariables.Contest c = tv.fetchContest(contestId);
			TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
			TemplateVariables.Result res = tv.fetchResultOptional(
				makeResultKey(getLoggedInUserKey(req), problemId)
				);

			if (res.opened == null) {
				openTheProblem(res.dsKey);
			}

			if (p.hasSpecFile() && !res.hasSourceFile()) {
				showMode = "description";
			}
			else {

				if (c.contestants_can_write_code) {
					showMode = "write";
				}
				else {
					showMode = "submit";
				}
			}

			String newUrl = makeContestUrl(contestId, "problem."+problemId+"/"+showMode);
			resp.sendRedirect(newUrl);
		}
		catch (EntityNotFoundException e) {

			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
	}

	private void openTheProblem(Key resultKey)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();
		try {
			Entity ent;
			try {
				ent = ds.get(resultKey);
			}
			catch (EntityNotFoundException e) {
				ent = defaultResultEntity(resultKey);
			}

			// default result entity should have 'opened' set
			assert ent.hasProperty("opened");

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
