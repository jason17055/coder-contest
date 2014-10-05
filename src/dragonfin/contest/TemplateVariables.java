package dragonfin.contest;

import dragonfin.contest.model.*;

import java.util.*;
import java.util.regex.*;
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
			Submission s = handleSubmission(ent.getKey(), ent);
			list.add(s);
		}
		return list;
	}

	public class Problem
	{
		public final Key dsKey;
		public String id;
		public String name;
		public String edit_url;
		public String new_system_test_url;
		public String new_clarification_url;
		Key specKey;

		Problem(Key dsKey) {
			this.dsKey = dsKey;
			this.id = Long.toString(dsKey.getId());
			this.edit_url = makeUrl("problem?id="+id);
			this.new_system_test_url = makeUrl("system_test?problem="+id);
			this.new_clarification_url = makeUrl("clarification?problem="+id);
		}

		public ArrayList<SystemTest> getSystem_tests()
		{
			Query q = new Query("SystemTest");
			q.setAncestor(dsKey);
			q.addSort("created");

			PreparedQuery pq = ds.prepare(q);
			ArrayList<SystemTest> list = new ArrayList<SystemTest>();
			for (Entity ent : pq.asIterable()) {
				SystemTest st = handleSystemTest(ent.getKey(), ent);
				list.add(st);
			}
			return list;
		}

		public ArrayList<Clarification> getClarifications()
		{
			return new ArrayList<Clarification>();
		}
	}

	String makeUrl(String path)
	{
		return path;
	}

	public class User
	{
		public final Key dsKey;
		public String username;
		public String name;
		public String description;
		public int ordinal;
		public boolean is_director;
		public boolean is_judge;
		public boolean is_contestant;

		User(Key dsKey) {
			this.dsKey = dsKey;

			String [] idParts = dsKey.getName().split("/");
			this.username = idParts[1];
		}
	}

	public class Submission
	{
		public final Key dsKey;
		public String id;
		public String status;
		public Date created;
		public String type = "submission";
		public String edit_url;
		public int minutes;

		Submission(Key dsKey)
		{
			this.dsKey = dsKey;

			String submitterId = dsKey.getParent().getName();
			String [] parts = submitterId.split("/");
			String username = parts[1];

			this.id = username + "/" + Long.toString(dsKey.getId());
			this.edit_url = "submission?id="+id;
		}

		Key problemKey;
		public Problem getProblem()
			throws EntityNotFoundException
		{
			return problemKey != null ? fetchProblem(problemKey) : null;
		}

		public User getSubmitter()
			throws EntityNotFoundException
		{
			return fetchUser(dsKey.getParent());
		}

		Key judgeKey;

		public User getJudge()
			throws EntityNotFoundException
		{
			return judgeKey != null ? fetchUser(judgeKey) : null;
		}

		Key sourceKey;
		public File getSource()
			throws EntityNotFoundException
		{
			return sourceKey != null ? fetchFile(sourceKey) : null;
		}
	}

	public class SystemTest
	{
		public final Key dsKey;
		public boolean sample;
		public boolean auto_judge;
		Key inputKey;
		Key expectedKey;

		SystemTest(Key key)
		{
			this.dsKey = key;
		}

		public File getInput()
			throws EntityNotFoundException
		{
			return inputKey != null ? fetchFile(inputKey) : null;
		}

		public File getExpected()
			throws EntityNotFoundException
		{
			return expectedKey != null ? fetchFile(expectedKey) : null;
		}

		public String getEdit_url()
		{
			String problemId = Long.toString(dsKey.getParent().getId());
			String num = Long.toString(dsKey.getId());
			return makeUrl("system_test?problem="+problemId+"&number="+num);
		}

		public long getTest_number()
		{
			return dsKey.getId();
		}

		public Problem getProblem()
			throws EntityNotFoundException
		{
			Key problemKey = dsKey.getParent();
			return fetchProblem(problemKey);
		}
	}

	public class Clarification
	{
	}

	HashMap<Key,Problem> cachedProblems = new HashMap<Key,Problem>();
	Problem fetchProblem(Key problemKey)
		throws EntityNotFoundException
	{
		if (cachedProblems.containsKey(problemKey)) {
			return cachedProblems.get(problemKey);
		}

		Entity ent = ds.get(problemKey);
		Problem p = handleProblem(problemKey, ent);
		cachedProblems.put(problemKey, p);
		return p;
	}

	Problem fetchProblem(String contestId, String id)
		throws EntityNotFoundException
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(id));
		return fetchProblem(problemKey);
	}

	Problem handleProblem(Key key, Entity ent)
	{
		Problem p = new Problem(key);
		p.name = (String) ent.getProperty("name");		
		p.specKey = (Key) ent.getProperty("spec");
		return p;
	}

	HashMap<Key,SystemTest> cachedSystemTests = new HashMap<Key,SystemTest>();
	SystemTest fetchSystemTest(Key testKey)
		throws EntityNotFoundException
	{
		if (cachedSystemTests.containsKey(testKey)) {
			return cachedSystemTests.get(testKey);
		}

		Entity ent = ds.get(testKey);
		SystemTest st = handleSystemTest(testKey, ent);
		cachedSystemTests.put(testKey, st);
		return st;
	}

	SystemTest fetchSystemTest(String contestId, String problemId, String testId)
		throws EntityNotFoundException
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));
		Key testKey = KeyFactory.createKey(problemKey, "SystemTest", Long.parseLong(testId));
		return fetchSystemTest(testKey);
	}

	SystemTest handleSystemTest(Key key, Entity ent)
	{
		SystemTest st = new SystemTest(key);
		st.inputKey = (Key) ent.getProperty("input");
		st.expectedKey = (Key) ent.getProperty("expected");
		st.sample = ent.hasProperty("sample") ?
			((Boolean) ent.getProperty("sample")).booleanValue() :
			false;
		st.auto_judge = ent.hasProperty("auto_judge") ?
			((Boolean) ent.getProperty("auto_judge")).booleanValue() :
			false;
		return st;
	}

	HashMap<Key,File> cachedFiles = new HashMap<Key,File>();
	File fetchFile(Key fileKey)
		throws EntityNotFoundException
	{
		if (cachedFiles.containsKey(fileKey)) {
			return cachedFiles.get(fileKey);
		}

		Entity ent = ds.get(fileKey);
		File f = handleFile(fileKey, ent);
		cachedFiles.put(fileKey, f);
		return f;
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

	HashMap<Key,Submission> cachedSubmissions = new HashMap<Key,Submission>();
	Submission fetchSubmission(Key subKey)
		throws EntityNotFoundException
	{
		if (cachedSubmissions.containsKey(subKey)) {
			return cachedSubmissions.get(subKey);
		}

		Entity ent = ds.get(subKey);
		Submission s = handleSubmission(subKey, ent);
		cachedSubmissions.put(subKey, s);
		return s;
	}

	static final Pattern SUBMISSION_ID_PATTERN = Pattern.compile("^([^/]+)/([0-9]+)$");
	static Key parseSubmissionId(String contestId, String submissionId)
	{
		Matcher m = SUBMISSION_ID_PATTERN.matcher(submissionId);
		if (!m.matches()) {
			throw new RuntimeException("Invalid submission id '"+submissionId+"'");
		}
		String username = m.group(1);
		long number = Long.parseLong(m.group(2));

		Key userKey = KeyFactory.createKey("User", contestId+"/"+username);
		Key submissionKey = KeyFactory.createKey(userKey, "Submission", number);
		return submissionKey;
	}

	Submission fetchSubmission(String contestId, String submissionId)
		throws EntityNotFoundException
	{
		return fetchSubmission(parseSubmissionId(contestId, submissionId));
	}

	Submission handleSubmission(Key key, Entity ent)
	{
		Submission s = new Submission(key);
		s.problemKey = (Key) ent.getProperty("problem");
		s.created = (Date) ent.getProperty("created");
		s.status = (String) ent.getProperty("status");
		s.judgeKey = (Key) ent.getProperty("judge");
		s.minutes = ent.hasProperty("minutes") ?
			(int)((Long) ent.getProperty("minutes")).longValue() :
			0;
		s.sourceKey = (Key) ent.getProperty("source");
		return s;
	}

	File handleFile(Key key, Entity ent)
	{
		File f = new File();
		f.id = key.getName();
		f.name = (String) ent.getProperty("given_name");
		f.url = req.getContextPath()+"/_f/"+f.id+"/"+f.name;
		f.inline_text_url = req.getContextPath()+"/_f/"+f.id+"/"+f.name+"?type=text";
		return f;
	}
}
