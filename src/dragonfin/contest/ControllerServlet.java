package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ControllerServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_teams", makeVar_all_teams(contestId));
		args.put("all_problems", makeVar_all_problems(contestId));
		renderTemplate(req, resp, "controller.tt", args);
	}

	ArrayList<ProblemInfo> makeVar_all_problems(String contestId)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Query q = new Query("Problem")
			.setAncestor(contestKey)
			.addSort("name");
		PreparedQuery pq = ds.prepare(q);

		ArrayList<ProblemInfo> list = new ArrayList<ProblemInfo>();
		for (Entity ent : pq.asIterable()) {
			ProblemInfo p = DataHelper.problemFromEntity(ent);
			p.edit_url = makeContestUrl(contestId, "problem", "id="+p.id);
			p.url = makeContestUrl(contestId, "problem."+p.id+"/", null);
			list.add(p);
		}

		return list;
	}

	ArrayList<UserInfo> makeVar_all_teams(String contestId)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		Query q = new Query("User")
			.setFilter(
			Query.CompositeFilterOperator.and(
				Query.FilterOperator.EQUAL.of("contest", contestId),
				Query.FilterOperator.EQUAL.of("is_contestant", Boolean.TRUE)
			));
		PreparedQuery pq = ds.prepare(q);

		ArrayList<UserInfo> list = new ArrayList<UserInfo>();
		for (Entity ent : pq.asIterable()) {
			UserInfo p = DataHelper.userFromEntity(ent);
			p.edit_url = makeContestUrl(contestId, "user", "id="+p.username);
			list.add(p);
		}

		return list;
	}
}
