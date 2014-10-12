package dragonfin.contest;

import dragonfin.contest.common.*;
import static dragonfin.contest.common.CommonFunctions.escapeUrl;

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

	Contest getContest()
		throws EntityNotFoundException
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}

		return fetchContest(contestId);
	}

	ArrayList<Worker> enumerateWorkers(String contestId)
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Query q = new Query("Worker")
			.setAncestor(contestKey);

		PreparedQuery pq = ds.prepare(q);
		ArrayList<Worker> list = new ArrayList<Worker>();
		for (Entity ent : pq.asIterable()) {
			Worker w = handleWorker(ent.getKey(), ent);
			list.add(w);
		}
		return list;
	}

	ArrayList<Problem> getAll_problems()
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}
		return enumerateProblems(contestId);
	}

	ArrayList<Problem> enumerateProblems(String contestId)
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Query q = new Query("Problem")
			.setAncestor(contestKey)
		//	.addSort("ordinal")
			.addSort("name")
			;
		PreparedQuery pq = ds.prepare(q);

		ArrayList<Problem> list = new ArrayList<Problem>();
		for (Entity ent : pq.asIterable()) {
			Problem p = handleProblem(ent.getKey(), ent);
			list.add(p);
		}
		return list;
	}

	ArrayList<Submission> enumerateSubmissions(String contestId)
	{
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

	ArrayList<User> getAll_contestants()
	{
		String contestId = req.getParameter("contest");
		if (contestId == null) {
			return null;
		}

		Query q = new Query("User");
		q.setFilter(Query.CompositeFilterOperator.and(
			Query.FilterOperator.EQUAL.of("contest", contestId),
			Query.FilterOperator.EQUAL.of("is_contestant", Boolean.TRUE)
			));

		PreparedQuery pq = ds.prepare(q);
		ArrayList<User> list = new ArrayList<User>();
		for (Entity ent : pq.asIterable()) {
			User u = handleUser(ent.getKey(), ent);
			list.add(u);
		}
		return list;
	}

	ArrayList<Contest> getAll_contests()
	{
		Query q = new Query("Contest");
		PreparedQuery pq = ds.prepare(q);

		ArrayList<Contest> list = new ArrayList<Contest>();
		for (Entity ent : pq.asIterable()) {
			Contest c = handleContest(ent.getKey(), ent);
			list.add(c);
		}
		return list;
	}

	ArrayList<User> enumerateUsers(String contestId)
	{
		Query q = new Query("User");
		q.setFilter(Query.FilterOperator.EQUAL.of("contest", contestId));

		PreparedQuery pq = ds.prepare(q);
		ArrayList<User> list = new ArrayList<User>();
		for (Entity ent : pq.asIterable()) {
			User u = handleUser(ent.getKey(), ent);
			list.add(u);
		}
		return list;
	}

	public class Contest
	{
		public final Key dsKey;
		public String id;
		public String title;
		public String subtitle;
		public String logo;
		public String created_by;
		public String collaboration;
		public String score_system;
		public String scoreboard;
		public String scoreboard_order;
		public String current_phase_name = "CODING";
		public String current_phase_timeleft = "foo";
		public String [] status_choices;
		public boolean contestants_can_change_name;
		public boolean contestants_can_change_description;
		public boolean contestants_can_change_password;
		public boolean contestants_can_write_code;
		public boolean judges_can_change_name;
		public boolean judges_can_change_password;
		public boolean scoreboard_images;
		public boolean scoreboard_popups;
		public boolean scoreboard_fanfare;

		Contest(Key dsKey) {
			this.dsKey = dsKey;
			this.id = dsKey.getName();

			status_choices = new String[] {
				"Accepted",
				"Correct",
				"Wrong Answer",
				"Output Format Error",
				"Excessive Output",
				"Compilation Error",
				"Run-Time Error",
				"Time-Limit Exceeded"
				};
		}

		public String getConfig_url()
		{
			return req.getContextPath()+"/_admin/define_contest?contest="+escapeUrl(id);
		}

		ArrayList<Worker> workersCached;
		public ArrayList<Worker> getWorkers()
		{
			if (workersCached != null) {
				return workersCached;
			}

			workersCached = enumerateWorkers(id);
			return workersCached;
		}

		ArrayList<Submission> submissionsCached;
		public ArrayList<Submission> getSubmissions()
		{
			if (submissionsCached == null) {
				submissionsCached = enumerateSubmissions(id);
			}
			return submissionsCached;
		}

		ArrayList<User> usersCached;
		public ArrayList<User> getUsers()
		{
			if (usersCached == null) {
				usersCached = enumerateUsers(id);
			}
			return usersCached;
		}

		ArrayList<Problem> problemsCached;
		public ArrayList<Problem> getProblems()
		{
			if (problemsCached == null) {
				problemsCached = enumerateProblems(id);
			}
			return problemsCached;
		}
	}

	public class Problem
	{
		public final Key dsKey;
		public String id;
		public String name;
		public String judged_by;
		public String scoreboard_image;
		public int difficulty;
		public int allocated_minutes;
		public int runtime_limit;
		public Date start_time;
		public boolean visible;
		public boolean allow_submissions;
		public boolean score_by_access_time;
		Key specFileKey;
		Key solutionFileKey;

		Problem(Key dsKey) {
			this.dsKey = dsKey;
			this.id = Long.toString(dsKey.getId());
		}

		public String getEdit_url()
		{
			return makeUrl("problem?id="+id);
		}

		public String getNew_system_test_url()
		{
			return makeUrl("system_test?problem="+id);
		}

		public String getNew_clarification_url()
		{
			return makeUrl("clarification?problem="+id);
		}

		public String getUrl()
		{
			return makeUrl("problem."+id+"/");
		}

		public File getSpec()
			throws EntityNotFoundException
		{
			return specFileKey != null ? fetchFile(specFileKey) : null;
		}

		public File getSolution()
			throws EntityNotFoundException
		{
			return solutionFileKey != null ? fetchFile(solutionFileKey) : null;
		}

		public ArrayList<SystemTest> getSample_inputs()
		{
			Query q = new Query("SystemTest");
			q.setAncestor(dsKey);
			q.setFilter(Query.FilterOperator.EQUAL.of("sample", Boolean.TRUE));
			q.addSort("created");

			PreparedQuery pq = ds.prepare(q);
			ArrayList<SystemTest> list = new ArrayList<SystemTest>();
			int count = 0;
			for (Entity ent : pq.asIterable()) {
				count++;
				SystemTest st = handleSystemTest(ent.getKey(), ent);
				st.sample_name = name + "_" + count;
				list.add(st);
			}
			return list;
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
		String contestId = req.getParameter("contest");
		return req.getContextPath() + "/" + contestId +
			"/" + path;
	}

	public class Worker
	{
		public final Key dsKey;
		public String id;
		public Date created;
		public String accepted_languages;
		public String name;
		public String description;
		public String system;

		Worker(Key dsKey) {
			this.dsKey = dsKey;
			this.id = Long.toString(dsKey.getId());
		}

		Entity statusEnt;
		void fetchStatus()
		{
			if (statusEnt != null) { return; }

			Key statusKey = KeyFactory.createKey(dsKey, "WorkerStatus", 1);
			try {
				statusEnt = ds.get(statusKey);
			}
			catch (EntityNotFoundException e) {
				statusEnt = new Entity(statusKey);
			}
		}

		public String getWorker_status()
		{
			fetchStatus();
			return (String) statusEnt.getProperty("worker_status");
		}

		public boolean getBusy()
		{
			fetchStatus();
			Boolean b = (Boolean) statusEnt.getProperty("busy");
			return (b != null && b.booleanValue());
		}
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
		public boolean online;
		public boolean visible;
		public String score_html;
		public Map<String,Result> result_by_problem;

		User(Key dsKey) {
			this.dsKey = dsKey;

			String [] idParts = dsKey.getName().split("/");
			this.username = idParts[1];
		}

		public String getEdit_url()
		{
			return makeUrl("user?id="+username);
		}
	}

	public class Result
	{
		public final Key dsKey;
		public Date opened;
		Key sourceFileKey;

		Result(Key dsKey) {
			this.dsKey = dsKey;
		}

		public File getSource()
			throws EntityNotFoundException
		{
			return sourceFileKey != null ? fetchFile(sourceFileKey) : null;
		}
	}

	static String getContestFromUserKey(Key userKey)
	{
		String id = userKey.getName();
		String [] parts = id.split("/");
		return parts[0];
	}

	static String getUsernameFromUserKey(Key userKey)
	{
		String id = userKey.getName();
		String [] parts = id.split("/");
		return parts[1];
	}

	static String makeSubmissionId(Key submissionKey)
	{
		Key userKey = submissionKey.getParent();
		String username = getUsernameFromUserKey(userKey);
		return username + "/" + Long.toString(submissionKey.getId());
	}

	static Key makeUserKey(String contestId, String username)
	{
		return KeyFactory.createKey("User",
			String.format("%s/%s", contestId, username)
			);
	}

	public class Submission
	{
		public final Key dsKey;
		public String id;
		public String status;
		public Date created;
		public String type;
		public int minutes;

		Submission(Key dsKey)
		{
			this.dsKey = dsKey;
			this.id = makeSubmissionId(dsKey);
		}

		public String getEdit_url()
		{
			if ("clarification".equals(type)) {
				return "clarification?id="+id;
			}
			else {
				return "submission?id="+id;
			}
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

		public ArrayList<TestResult> getTest_results()
		{
			return null;
		}
	}

	public class TestResult
	{
	//TODO
	}

	public class SystemTest
	{
		public final Key dsKey;
		public boolean sample;
		public boolean auto_judge;
		public String sample_name;
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

	public class TestJob
	{
		final Key dsKey;
		public String id;
		public String type;
		public String result_status;
		public boolean claimed;
		public boolean finished;
		Key sourceFileKey;
		Key inputFileKey;
		Key outputFileKey;
		Key result_detailFileKey;

		TestJob(Key dsKey) {
			this.dsKey = dsKey;
			this.id = Long.toString(dsKey.getId());
		}

		public File getSource()
			throws EntityNotFoundException
		{
			return sourceFileKey != null ? fetchFile(sourceFileKey) : null;
		}

		public File getInput()
			throws EntityNotFoundException
		{
			return inputFileKey != null ? fetchFile(inputFileKey) : null;
		}

		public File getOutput()
			throws EntityNotFoundException
		{
			return outputFileKey != null ? fetchFile(outputFileKey) : null;
		}

		public File getResult_detail()
			throws EntityNotFoundException
		{
			return result_detailFileKey != null ? fetchFile(result_detailFileKey) : null;
		}
	}

	public class Clarification
	{
	}

	Contest fetchContest(String contestId)
		throws EntityNotFoundException
	{
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Entity ent = ds.get(contestKey);
		return handleContest(contestKey, ent);
	}

	Contest handleContest(Key key, Entity ent)
	{
		Contest c = new Contest(key);

		// strings
		c.title = ent.hasProperty("title") ?
			(String)ent.getProperty("title") :
			c.id;
		c.subtitle = (String)ent.getProperty("subtitle");
		c.logo = (String)ent.getProperty("logo");
		c.created_by = (String)ent.getProperty("created_by");
		c.collaboration = ent.hasProperty("collaboration") ?
			(String)ent.getProperty("collaboration") :
			"N";
		c.score_system = ent.hasProperty("score_system") ?
			(String)ent.getProperty("score_system") :
			"A";
		c.scoreboard = ent.hasProperty("scoreboard") ?
			(String)ent.getProperty("scoreboard") :
			"Y";
		c.scoreboard_order = ent.hasProperty("scoreboard_order") ?
			(String)ent.getProperty("scoreboard_order") :
			"s";

		// booleans
		c.contestants_can_change_name = ent.hasProperty("contestants_can_change_name") ?
			((Boolean)ent.getProperty("contestants_can_change_name")).booleanValue() :
			true;
		c.contestants_can_change_description = ent.hasProperty("contestants_can_change_description") ?
			((Boolean)ent.getProperty("contestants_can_change_description")).booleanValue() :
			true;
		c.contestants_can_change_password = ent.hasProperty("contestants_can_change_password") ?
			((Boolean)ent.getProperty("contestants_can_change_password")).booleanValue() :
			true;
		c.contestants_can_write_code = ent.hasProperty("contestants_can_write_code") ?
			((Boolean)ent.getProperty("contestants_can_write_code")).booleanValue() :
			true;
		c.judges_can_change_name = ent.hasProperty("judges_can_change_name") ?
			((Boolean)ent.getProperty("judges_can_change_name")).booleanValue() :
			true;
		c.judges_can_change_password = ent.hasProperty("judges_can_change_password") ?
			((Boolean)ent.getProperty("judges_can_change_password")).booleanValue() :
			true;
		c.scoreboard_images = ent.hasProperty("scoreboard_images") ?
			((Boolean)ent.getProperty("scoreboard_images")).booleanValue() :
			true;
		c.scoreboard_popups = ent.hasProperty("scoreboard_popups") ?
			((Boolean)ent.getProperty("scoreboard_popups")).booleanValue() :
			true;
		c.scoreboard_fanfare = ent.hasProperty("scoreboard_fanfare") ?
			((Boolean)ent.getProperty("scoreboard_fanfare")).booleanValue() :
			true;

		return c;
	}

	TestJob fetchTestJob(Key jobKey)
		throws EntityNotFoundException
	{
		Entity ent = ds.get(jobKey);
		return handleTestJob(jobKey, ent);
	}

	TestJob fetchTestJob(String jobId)
		throws EntityNotFoundException
	{
		Key jobKey = KeyFactory.createKey("TestJob", Long.parseLong(jobId));
		return fetchTestJob(jobKey);
	}

	TestJob handleTestJob(Key key, Entity ent)
	{
		TestJob j = new TestJob(key);

		//strings/dates
		j.type = (String) ent.getProperty("type");		
		j.result_status = (String) ent.getProperty("result_status");

		//files
		j.sourceFileKey = (Key) ent.getProperty("source");
		j.inputFileKey = (Key) ent.getProperty("input");
		j.outputFileKey = (Key) ent.getProperty("output");
		j.result_detailFileKey = (Key) ent.getProperty("result_detail");

		//booleans
		j.claimed = ent.hasProperty("claimed") ?
			((Boolean) ent.getProperty("claimed")).booleanValue() :
			false;
		j.finished = ent.hasProperty("finished") ?
			((Boolean) ent.getProperty("finished")).booleanValue() :
			false;

		return j;
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

		//strings/dates
		p.name = (String) ent.getProperty("name");		
		p.judged_by = (String) ent.getProperty("judged_by");
		p.scoreboard_image = (String) ent.getProperty("scoreboard_image");
		p.start_time = (Date) ent.getProperty("start_time");

		//files
		p.specFileKey = (Key) ent.getProperty("spec");
		p.solutionFileKey = (Key) ent.getProperty("solution");

		//booleans
		p.visible = ent.hasProperty("visible") ?
			((Boolean) ent.getProperty("visible")).booleanValue() :
			false;
		p.allow_submissions = ent.hasProperty("allow_submissions") ?
			((Boolean) ent.getProperty("allow_submissions")).booleanValue() :
			false;
		p.score_by_access_time = ent.hasProperty("score_by_access_time") ?
			((Boolean) ent.getProperty("score_by_access_time")).booleanValue() :
			false;

		// integers
		p.difficulty = ent.hasProperty("difficulty") ?
			(int)((Long)ent.getProperty("difficulty")).longValue() :
			0;
		p.allocated_minutes = ent.hasProperty("allocated_minutes") ?
			(int)((Long)ent.getProperty("allocated_minutes")).longValue() :
			0;
		p.runtime_limit = ent.hasProperty("runtime_limit") ?
			(int)((Long)ent.getProperty("runtime_limit")).longValue() :
			0;
		return p;
	}

	Worker handleWorker(Key key, Entity ent)
	{
		Worker w = new Worker(key);

		w.created = (Date) ent.getProperty("created");
		w.accepted_languages = (String) ent.getProperty("accepted_languages");
		w.name = (String) ent.getProperty("name");
		w.description = (String) ent.getProperty("description");
		w.system = (String) ent.getProperty("system");

		return w;
	}

	HashMap<Key,Result> cachedResults = new HashMap<Key,Result>();
	Result fetchResult(Key resultKey)
		throws EntityNotFoundException
	{
		if (cachedResults.containsKey(resultKey)) {
			return cachedResults.get(resultKey);
		}

		Entity ent = ds.get(resultKey);
		Result r = handleResult(resultKey, ent);
		cachedResults.put(resultKey, r);
		return r;
	}

	Result handleResult(Key key, Entity ent)
	{
		Result r = new Result(key);
		r.sourceFileKey = (Key) ent.getProperty("source");
		r.opened = (Date) ent.getProperty("opened");
		return r;
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

	User fetchUser(String contestId, String username)
		throws EntityNotFoundException
	{
		Key userKey = KeyFactory.createKey("User", contestId+"/"+username);
		return fetchUser(userKey);
	}

	User handleUser(Key key, Entity ent)
	{
		User u = new User(key);
		u.is_director = ent.hasProperty("is_director") ?
			((Boolean) ent.getProperty("is_director")).booleanValue() :
			false;
		u.is_judge = ent.hasProperty("is_judge") ?
			((Boolean) ent.getProperty("is_judge")).booleanValue() :
			false;
		u.is_contestant = ent.hasProperty("is_contestant") ?
			((Boolean) ent.getProperty("is_contestant")).booleanValue() :
			false;
		u.name = (String) ent.getProperty("name");
		u.description = (String) ent.getProperty("description");
		u.ordinal = ent.hasProperty("ordinal") ?
			(int)((Long)ent.getProperty("ordinal")).longValue() :
			0;
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
		s.type = (String) ent.getProperty("type");
		return s;
	}

	File handleFile(Key key, Entity ent)
	{
		File f = new File();
		f.id = key.getName();
		f.name = (String) ent.getProperty("given_name");
		f.url = makeFileUrl(req, f.id, f.name);
		f.inline_text_url = makeFileUrl(req, f.id, f.name)+"?type=text";
		return f;
	}

	public static String makeFileUrl(HttpServletRequest req, String id, String name)
	{
		return req.getContextPath()+"/_f/"+id+"/"+name;
	}

	public static Entity defaultResultEntity(Key resultKey)
	{
		Entity ent = new Entity(resultKey);
		ent.setProperty("opened", new Date());
		return ent;
	}
}
