package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ListProblemsServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		String contestId = req.getParameter("contest");
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Query q = new Query("Problem")
			.setAncestor(contestKey);
		PreparedQuery pq = ds.prepare(q);

		ArrayList<ProblemInfo> list = new ArrayList<ProblemInfo>();
		for (Entity ent : pq.asIterable()) {
			String problemId = ent.getKey().getName();
			String creator = (String) ent.getProperty("createdBy");
			ProblemInfo p = new ProblemInfo();
			p.id = problemId;
			p.contestId = contestId;
			list.add(p);
		}

		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_problems", list);
		renderTemplate(req, resp, "list_problems.tt", args);
	}
}
