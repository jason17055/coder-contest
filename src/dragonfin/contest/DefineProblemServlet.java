package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class DefineProblemServlet extends CoreServlet
{
	static final String TEMPLATE = "define_problem.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		renderTemplate(req, resp, TEMPLATE);
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:create_problem") != null) {
			doCreateProblem(req, resp);
		}
		else {
			throw new ServletException("invalid POST");
		}
	}

	void doCreateProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String directorName = "director";
		String directorPassword = req.getParameter("password");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity ent1 = new Entity(contestKey);
			ent1.setProperty("created", new Date());
			ent1.setProperty("createdBy", "*anonymous");
			ds.put(ent1);

			Key userKey = KeyFactory.createKey(contestKey,
					"User", directorName);
			Entity ent2 = new Entity(userKey);
			ent2.setProperty("password", directorPassword);
			ent2.setProperty("isDirector", Boolean.TRUE);
			ds.put(ent2);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		String newUrl = makeContestUrl(contestId, "problems", null);
		resp.sendRedirect(newUrl);
	}
}
