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
			s.submitterKey = (Key) ent.getProperty("submitter");
			s.judgeKey = (Key) ent.getProperty("judge");
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

	public class User
	{
		public final Key dsKey;
		public String name;
		public String description;
		public int ordinal;
		public boolean is_director;
		public boolean is_judge;
		public boolean is_contestant;

		User(Key dsKey) {
			this.dsKey = dsKey;
		}
	}

	public class Submission
	{
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

		Key submitterKey;
		Key judgeKey;
		public User getSubmitter()
			throws EntityNotFoundException
		{
			return submitterKey != null ? fetchUser(submitterKey) : null;
		}

		public User getJudge()
			throws EntityNotFoundException
		{
			return judgeKey != null ? fetchUser(judgeKey) : null;
		}
	}

	HashMap<Key,User> cachedUsers = new HashMap<Key,User>();
	User fetchUser(Key userKey)
		throws EntityNotFoundException
	{
		if (cachedUsers.containsKey(userKey)) {
			return cachedUsers.get(userKey);
		}

		Entity ent = ds.get(userKey);

		User u = new User(userKey);
		u.name = (String) ent.getProperty("name");
		u.description = (String) ent.getProperty("description");
		u.ordinal = ent.hasProperty("ordinal") ?
			(int)((Long) ent.getProperty("ordinal")).longValue() :
			0;
		u.is_director = ent.hasProperty("is_director") ?
			((Boolean) ent.getProperty("is_director")).booleanValue() :
			false;
		u.is_judge = ent.hasProperty("is_judge") ?
			((Boolean) ent.getProperty("is_judge")).booleanValue() :
			false;
		u.is_contestant = ent.hasProperty("is_contestant") ?
			((Boolean) ent.getProperty("is_contestant")).booleanValue() :
			false;

		cachedUsers.put(userKey, u);
		return u;
	}
}
