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

	static class NotFound extends Exception
	{
		public NotFound(Throwable cause) {
			super("Entity Not Found", cause);
		}
	}
}
