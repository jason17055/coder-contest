package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ProblemDescriptionServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }

		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("problem");

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		try {
			Entity ent = ds.get(problemKey);
			ProblemInfo p = DataHelper.problemFromEntity(ent);

			p.spec = checkFileUrl(DataHelper.addFileMetadata(ds, p.spec));
			p.url = makeContestUrl(contestId, "problem."+problemId+"/");

			HashMap<String,Object> args = new HashMap<String,Object>();
			args.put("problem", p);
			renderTemplate(req, resp, "problem_description.tt", args);
		}
		catch (EntityNotFoundException e){
			resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}
}
