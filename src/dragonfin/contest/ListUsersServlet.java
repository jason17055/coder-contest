package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ListUsersServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		String contestId = req.getParameter("contest");
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Query q = new Query("User")
		//	.setFilter(
		//	new Query.FilterPredicate(
		//		"contest", Query.FilterOperator.EQUAL,
		//		contestId)
			;
		PreparedQuery pq = ds.prepare(q);

		ArrayList<UserInfo> list = new ArrayList<UserInfo>();
		for (Entity ent : pq.asIterable()) {
			UserInfo p = DataHelper.userFromEntity(ent);
			p.edit_url = makeContestUrl(contestId, "user", "id="+p.username);
			list.add(p);
		}

		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_users", list);
		renderTemplate(req, resp, "list_users.tt", args);
	}
}
