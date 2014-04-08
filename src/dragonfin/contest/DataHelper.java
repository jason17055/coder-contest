package dragonfin.contest;

import com.google.appengine.api.datastore.*;
import dragonfin.contest.model.*;

public class DataHelper
{
	public static ContestInfo loadContest(String contestId)
		throws NotFound
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);

		try {
		Entity ent = ds.get(contestKey);
		ContestInfo rv = new ContestInfo();

		rv.id = contestId;
		rv.title = ent.hasProperty("title") ?
			(String)ent.getProperty("title") :
			contestId;
		rv.created_by = (String)ent.getProperty("created_by");

		return rv;

		}
		catch (EntityNotFoundException e) {
			throw new NotFound(e);
		}
	}

	static ProblemInfo problemFromEntity(Entity ent)
	{
		String problemId = Long.toString(ent.getKey().getId());
		String contestId = ent.getKey().getParent().getName();

		ProblemInfo rv = new ProblemInfo();

		rv.id = problemId;
		rv.contestId = contestId;
		rv.name = (String) ent.getProperty("name");
		if (rv.name == null || rv.name.length() == 0) {
			rv.name = "(unnamed)";
		}

		rv.judged_by = (String) ent.getProperty("judged_by");
		rv.scoreboard_image = (String) ent.getProperty("scoreboard_image");

		if (ent.hasProperty("visible")) {
			rv.visible = ((Boolean) ent.getProperty("visible")).booleanValue();
		}
		if (ent.hasProperty("allow_submissions")) {
			rv.allow_submissions = ((Boolean) ent.getProperty("allow_submissions")).booleanValue();
		}
		if (ent.hasProperty("score_by_access_time")) {
			rv.score_by_access_time = ((Boolean) ent.getProperty("score_by_access_time")).booleanValue();
		}

		if (ent.hasProperty("ordinal")) {
			rv.ordinal = (int)
			((Long) ent.getProperty("ordinal")).longValue();
		}
		if (ent.hasProperty("difficulty")) {
			rv.difficulty = (int)
			((Long) ent.getProperty("difficulty")).longValue();
		}
		if (ent.hasProperty("allocated_minutes")) {
			rv.allocated_minutes = (int)
			((Long) ent.getProperty("allocated_minutes")).longValue();
		}
		if (ent.hasProperty("runtime_limit")) {
			rv.runtime_limit = (int)
			((Long) ent.getProperty("runtime_limit")).longValue();
		}

		return rv;
	}

	static File handleFileProperty(Entity ent, String propName)
	{
		Key fileKey = (Key) ent.getProperty(propName);
		if (fileKey == null)
			return null;

		try {
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity fileEnt = ds.get(fileKey);

		File f = new File();
		f.id = fileKey.getName();
		f.name = (String) fileEnt.getProperty("given_name");
		return f;

		}
		catch (EntityNotFoundException e){
			File f = new File();
			f.id = "missing";
			f.name = "missing";
			return f;
		}
	}

	public static ProblemInfo loadProblem(String contestId, String id)
		throws NotFound
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", Long.parseLong(id));

		try {

		Entity ent = ds.get(prbKey);
		ProblemInfo rv = problemFromEntity(ent);

		rv.spec = handleFileProperty(ent, "spec");
		rv.solution = handleFileProperty(ent, "solution");

		return rv;

		}
		catch (EntityNotFoundException e) {
			throw new NotFound(e);
		}
	}

	static UserInfo userFromEntity(Entity ent)
	{
		UserInfo rv = new UserInfo();

		String uid = ent.getKey().getName();
		if (uid.indexOf('/') != -1) {
			rv.contest = uid.substring(0,uid.indexOf('/'));
			rv.username = uid.substring(uid.indexOf('/')+1);
		}
		else {
			rv.username = uid;
		}

		rv.name = (String) ent.getProperty("name");
		rv.description = (String) ent.getProperty("description");

		rv.ordinal = ent.hasProperty("ordinal") ?
			(int)((Long) ent.getProperty("ordinal")).longValue() :
			0;
		rv.is_director = ent.hasProperty("is_director") ?
			((Boolean) ent.getProperty("is_director")).booleanValue() :
			false;
		rv.is_judge = ent.hasProperty("is_judge") ?
			((Boolean) ent.getProperty("is_judge")).booleanValue() :
			false;
		rv.is_contestant = ent.hasProperty("is_contestant") ?
			((Boolean) ent.getProperty("is_contestant")).booleanValue() :
			false;

		return rv;
	}

	public static UserInfo loadUser(String contestId, String username)
		throws NotFound
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key userKey = KeyFactory.createKey(contestKey,
				"User", username);

		try {

		Entity ent = ds.get(userKey);
		UserInfo rv = userFromEntity(ent);

		return rv;

		}
		catch (EntityNotFoundException e) {
			throw new NotFound(e);
		}
	}

	static class NotFound extends Exception
	{
		public NotFound(Throwable cause) {
			super("Entity Not Found", cause);
		}
	}
}
