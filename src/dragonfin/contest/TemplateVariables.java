package dragonfin.contest;

import dragonfin.contest.model.*;

import java.util.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class TemplateVariables
{
	final DatastoreService ds;
	final HttpServletRequest req;

	public TemplateVariables(HttpServletRequest req)
	{
		this.ds = DatastoreServiceFactory.getDatastoreService();
		this.req = req;
	}

	ContestInfo getContest()
		throws DataHelper.NotFound
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}

		return DataHelper.loadContest(contestId);
	}

	ArrayList<Submission> getAll_submissions()
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}

		Query q = new Query("Submission");
		q.setFilter(
			Query.FilterOperator.EQUAL.of("contest", contestId)
			);
		q.addSort("created");

		PreparedQuery pq = ds.prepare(q);
		ArrayList<Submission> list = new ArrayList<Submission>();
		for (Entity ent : pq.asIterable()) {
			Submission s = new Submission(
				Long.toString(ent.getKey().getId())
				);
			s.problemKey = (Key) ent.getProperty("problem");
			s.created = (Date) ent.getProperty("created");
			s.status = (String) ent.getProperty("status");
			list.add(s);
		}
		return list;
	}

	public class Problem
	{
		public final Key dsKey;
		public String id;
		public String name;
		Key specKey;

		Problem(Key dsKey) {
			this.dsKey = dsKey;
			this.id = Long.toString(dsKey.getId());
		}
	}

	public class Submission
	{
		public UserInfo getSubmitter() { return null; }
		public UserInfo getJudge() { return null; }

		public final String id;
		public String status;
		public Date created;
		public String type = "submission";
		public String edit_url;

		Submission(String id) {
			this.id = id;
			this.edit_url = "submission/"+id;
		}

		Problem problemCached;
		Key problemKey;
		public Problem getProblem()
			throws EntityNotFoundException
		{
			if (problemCached != null) {
				return problemCached;
			}

			if (problemKey == null) {
				return null;
			}

			Entity ent = ds.get(problemKey);

			problemCached = new Problem(problemKey);
			problemCached.name = (String) ent.getProperty("name");		
			problemCached.specKey = (Key) ent.getProperty("spec");
			return problemCached;
		}
	}
}
