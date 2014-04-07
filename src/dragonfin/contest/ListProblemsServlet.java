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
			.setAncestor(contestKey)
		//	.addSort("ordinal")
			.addSort("name")
			;
		PreparedQuery pq = ds.prepare(q);

		ArrayList<ProblemInfo> list = new ArrayList<ProblemInfo>();
		for (Entity ent : pq.asIterable()) {
			String problemId = Long.toString(ent.getKey().getId());
			ProblemInfo p = new ProblemInfo();
			p.id = problemId;
			p.name = (String) ent.getProperty("name");
			if (p.name == null || p.name.length() == 0) {
				p.name = "(unnamed)";
			}
			p.contestId = contestId;
			p.edit_url = makeContestUrl(contestId, "problem", "id="+p.id);
			list.add(p);
		}

		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_problems", list);
		renderTemplate(req, resp, "list_problems.tt", args);
	}
}
