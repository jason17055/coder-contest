package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ListUsersServlet extends CoreServlet
{
	String getTemplate() {
		return "list_users.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:bulk_create_users") != null) {
			doBulkCreate(req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	void doBulkCreate(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String userType = req.getParameter("bulk_type");

		if (contestId == null || userType == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Matcher m = Pattern.compile("^([cj]*):(.+)$").matcher(userType);
		if (!m.matches()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}
		boolean makeContestant = m.group(1).indexOf('c') != -1;
		boolean makeJudge = m.group(1).indexOf('j') != -1;
		String prefix = m.group(2);

		int bulkStart = Integer.parseInt(req.getParameter("bulk_start"));
		int bulkEnd = Integer.parseInt(req.getParameter("bulk_end"));
		if (bulkEnd-bulkStart > 30) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Cannot create more than 30 at a time.");
			return;
		}

		for (int i = bulkStart; i <= bulkEnd; i++) {
			doCreateOneUser(contestId, prefix+i, makeContestant, makeJudge);
		}

		String url = getMyUrl(req);
		resp.sendRedirect(url);
	}

	void doCreateOneUser(String contestId, String username, boolean makeContestant, boolean makeJudge)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key userKey = KeyFactory.createKey("User", contestId+"/"+username);

		Transaction txn = ds.beginTransaction();
		try {
			Entity ent;
			try {
				ent = ds.get(userKey);
				return; /* the user already exists */
			}
			catch (EntityNotFoundException e) {
				// ok to continue
			}

			ent = new Entity(userKey);
			ent.setProperty("created", new Date());
			ent.setProperty("contest", contestId);
			ent.setProperty("username", username);
			ent.setProperty("name", username);
			ent.setProperty("description", "");
			ent.setProperty("is_director", Boolean.FALSE);
			ent.setProperty("is_judge", Boolean.valueOf(makeJudge));
			ent.setProperty("is_contestant", Boolean.valueOf(makeContestant));
			ent.setProperty("visible", Boolean.TRUE);

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
