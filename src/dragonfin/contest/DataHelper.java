package dragonfin.contest;

import com.google.appengine.api.datastore.*;
import dragonfin.contest.model.*;

public class DataHelper
{
	static File addFileMetadata(DatastoreService ds, File f)
	{
		if (f == null || f.id == null) { return null; }

		try {

		if (f.name == null) {
			Key fKey = KeyFactory.createKey("File", f.id);
			Entity ent = ds.get(fKey);
			f.name = (String) ent.getProperty("given_name");
		}

		return f;

		}
		catch (EntityNotFoundException e) {
			return null;
		}
	}

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

	static File key2file(Key k)
	{
		File f = new File();
		f.id = k.getName();
		return f;
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
		Key userKey = KeyFactory.createKey("User",
				contestId+"/"+username);

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
