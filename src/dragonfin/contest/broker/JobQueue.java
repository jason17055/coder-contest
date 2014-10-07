package dragonfin.contest.broker;

import java.util.*;
import com.google.appengine.api.datastore.*;

public class JobQueue
{
	ArrayList<Entity> pendingJobs = new ArrayList<Entity>();
	HashSet<Key> knownJobs = new HashSet<Key>();
	HashMap<Key,Entity> activeJobs = new HashMap<Key,Entity>();

	long lastScan = 0;
	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

	static final long SCAN_CYCLE = 30000;

	synchronized Entity claim(String contestId, Set<String> languages, Key workerKey, long timeout)
	{
		long endTime = System.currentTimeMillis() + timeout;
		for (;;) {

			for (Iterator<Entity> it = pendingJobs.iterator(); it.hasNext(); ) {
				Entity ent = it.next();

				if (isEligible(ent, contestId, languages)) {

					it.remove();
					if (tryClaim(workerKey, ent.getKey())) {

						// successful claim
						activeJobs.put(ent.getKey(), ent);
						return ent;
					}
				}
			}

			long curTime = System.currentTimeMillis();
			if (curTime - lastScan > SCAN_CYCLE) {

				scanNow();
				curTime = System.currentTimeMillis();
			}

			if (curTime >= endTime) {
	
				return null;
			}

			try {
				wait(endTime-curTime);
			}
			catch (InterruptedException e) {
				// shutdown the thread
				return null;
			}
		}
	}

	private void scanNow()
	{
		Query q = new Query("TestJob");
		q.setFilter(
			Query.FilterOperator.EQUAL.of("claimed", Boolean.FALSE)
			);
		q.addSort("created");

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {
			if (!knownJobs.contains(ent.getKey())) {

				newJob(ent);
			}
		}
	}

	private void newJob(Entity ent)
	{
		pendingJobs.add(ent);
		knownJobs.add(ent.getKey());
	}

	private boolean isEligible(Entity ent, String contestId, Set<String> languages)
	{
		String jobContest = (String) ent.getProperty("contest");
		if (jobContest == null || !jobContest.equals(contestId)) {
			return false;
		}

		String fileType = (String) ent.getProperty("source_extension");
		return (languages.contains(fileType));
	}

	private boolean tryClaim(Key workerKey, Key jobKey)
	{
		Transaction txn = ds.beginTransaction();
		try {
			Entity ent;
			try {
				ent = ds.get(jobKey);
			}
			catch (EntityNotFoundException e) {
				return false;
			}

			Boolean isClaimed = (Boolean) ent.getProperty("claimed");
			if (isClaimed != null && isClaimed.booleanValue()) {
				return false;
			}

			ent.setProperty("claimed", Boolean.TRUE);
			ent.setProperty("owner", workerKey);
			ent.setProperty("last_touched", new Date());
			ds.put(ent);

			txn.commit();

			return true;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
