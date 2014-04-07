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

	public static ProblemInfo loadProblem(String contestId, String id)
		throws NotFound
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", Long.parseLong(id));

		try {

		Entity ent = ds.get(prbKey);
		ProblemInfo rv = new ProblemInfo();

		rv.id = id;
		rv.contestId = contestId;
		rv.name = (String) ent.getProperty("name");
		rv.ordinal = ent.hasProperty("ordinal") ?
			((Integer) ent.getProperty("ordinal")).intValue() :
			0;

		return rv;

		}
		catch (EntityNotFoundException e) {
			throw new NotFound(e);
		}
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
		UserInfo rv = new UserInfo();

		rv.id = username;
		rv.contestId = contestId;
		rv.name = (String) ent.getProperty("name");
		rv.ordinal = ent.hasProperty("ordinal") ?
			((Integer) ent.getProperty("ordinal")).intValue() :
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
