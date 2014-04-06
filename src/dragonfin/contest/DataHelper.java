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
		rv.createdBy = (String)ent.getProperty("createdBy");

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
