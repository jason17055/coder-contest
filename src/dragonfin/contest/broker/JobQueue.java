package dragonfin.contest.broker;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import com.google.appengine.api.datastore.*;

public class JobQueue
{
	ArrayList<Entity> pendingJobs = new ArrayList<Entity>();
	HashSet<Key> knownJobs = new HashSet<Key>();

	long lastScan = 0;
	DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

	static final long SCAN_CYCLE = 180000; //three minutes
	private static final Logger log = Logger.getLogger(JobQueue.class.getName());

	synchronized Entity claim(String contestId, Set<String> languages, Key workerKey, long timeout)
	{
		long endTime = System.currentTimeMillis() + timeout;
		for (;;) {

			for (Iterator<Entity> it = pendingJobs.iterator(); it.hasNext(); ) {
				Entity ent = it.next();

				if (isEligible(ent, contestId, languages)) {

					knownJobs.remove(ent.getKey());
					it.remove();

					if (tryClaim(workerKey, ent.getKey())) {

						// successful claim
						return ent;
					}
				}
			}

			long curTime = System.currentTimeMillis();
			if (curTime - lastScan > SCAN_CYCLE) {

				log.info("scanning for new jobs");
				scanNow();
				curTime = System.currentTimeMillis();
				lastScan = curTime;
			}
			else if (curTime >= endTime) {

				// ran out of time
				return null;
			}
			else {

				try {
					wait(endTime-curTime);
				}
				catch (InterruptedException e) {
					// request for thread to shutdown?
					return null;
				}
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

	public synchronized boolean maybeNewJob(String jobId)
	{
		log.info("notified of job "+jobId);

		Key jobKey = KeyFactory.createKey("TestJob", Long.parseLong(jobId));
		if (knownJobs.contains(jobKey)) {

			return false;
		}

		try {
			Entity ent = ds.get(jobKey);
			newJob(ent);
			notifyAll();

			return true;
		}
		catch (EntityNotFoundException e) {
			log.warning("notified of job "+jobId+", but this job not found in datastore");
			//unexpected
			return true;
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
