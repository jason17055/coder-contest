package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ListContestsServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		Query q = new Query("Contest");
		PreparedQuery pq = ds.prepare(q);

		ArrayList<ContestInfo> list = new ArrayList<ContestInfo>();
		for (Entity ent : pq.asIterable()) {
			String contestId = ent.getKey().getName();
			String creator = (String) ent.getProperty("createdBy");
			ContestInfo c = new ContestInfo();
			c.id = contestId;
			c.createdBy = creator;
			list.add(c);
		}

		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_contests", list);
		renderTemplate(req, resp, "admin/list_contest.tt", args);
	}
}
