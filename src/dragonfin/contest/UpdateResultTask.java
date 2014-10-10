package dragonfin.contest;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;

import static dragonfin.contest.TemplateVariables.defaultResultEntity;
import static dragonfin.contest.TemplateVariables.getContestFromUserKey;

public class UpdateResultTask extends HttpServlet
{
	private static final Logger log = Logger.getLogger(UpdateResultTask.class.getName());

	static TaskOptions makeUrl()
	{
		return TaskOptions.Builder.withUrl("/_task/update_result");
	}

	public static boolean isStatusCorrect(String status)
	{
		return status.equals("Correct") || status.equals("Accepted");
	}

	public static int penaltyPerIncorrect = 20;

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String userId = req.getParameter("user");
		String problemId = req.getParameter("problem");

		log.info("want to update result for user "+userId+" problem "+problemId);

		Key userKey = KeyFactory.createKey("User", userId);
		Key resultKey = KeyFactory.createKey(userKey, "Result", Long.parseLong(problemId));

		String contestId = getContestFromUserKey(userKey);
		Key contestKey = KeyFactory.createKey("Contest", contestId);

		Key problemKey = KeyFactory.createKey(contestKey, "Problem", Long.parseLong(problemId));

		// search for all submissions for this user+problem

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("Submission")
			.setAncestor(userKey);
		q.setFilter(Query.CompositeFilterOperator.and(
			Query.FilterOperator.EQUAL.of("problem", problemKey),
			Query.FilterOperator.EQUAL.of("type", "submission")
			));

		boolean foundCorrect = false;
		int timeOfFirstCorrect = Integer.MAX_VALUE;
		int countIncorrect = 0;

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {

			String type = (String) ent.getProperty("type");
			assert type != null && type.equals("submission");

			String status = (String) ent.getProperty("status");
			if (status == null) {
				//unjudged
				continue;
			}

			int minutes = ent.hasProperty("minutes") ?
				(int)((Long)ent.getProperty("minutes")).longValue() :
				1;

			if (isStatusCorrect(status)) {

				foundCorrect = true;
				timeOfFirstCorrect = Math.min(timeOfFirstCorrect, minutes);
			}
			else {

				countIncorrect++;
			}
		}

		int new_minutes = foundCorrect ? timeOfFirstCorrect : 0;
		int new_incorrect_submissions = countIncorrect;
		int new_score = foundCorrect ? 1 : 0;
		int new_score_alt = foundCorrect ? -(countIncorrect*penaltyPerIncorrect + timeOfFirstCorrect) : 0;

		boolean anyChange = false;

		Transaction txn = ds.beginTransaction();
		try {

			Entity ent;
			try {
				ent = ds.get(resultKey);
			}
			catch (EntityNotFoundException e) {

				ent = defaultResultEntity(resultKey);
			}

			int old_minutes = ent.hasProperty("minutes") ?
				(int)((Long)ent.getProperty("minutes")).longValue() : 0;
			int old_incorrect_submissions = ent.hasProperty("incorrect_submissions") ?
				(int)((Long)ent.getProperty("incorrect_submissions")).longValue() : 0;
			int old_score = ent.hasProperty("score") ?
				(int)((Long)ent.getProperty("score")).longValue() : 0;
			int old_score_alt = ent.hasProperty("score_alt") ?
				(int)((Long)ent.getProperty("score_alt")).longValue() : 0;

			if (new_minutes != old_minutes) {
				ent.setProperty("minutes", new Long(new_minutes));
				anyChange = true;
			}
			if (new_incorrect_submissions != old_incorrect_submissions) {
				ent.setProperty("incorrect_submissions", new Long(new_incorrect_submissions));
				anyChange = true;
			}
			if (new_score != old_score) {
				ent.setProperty("score", new Long(new_score));
				anyChange = true;
			}
			if (new_score_alt != old_score_alt) {
				ent.setProperty("score_alt", new Long(new_score_alt));
				anyChange = true;
			}

			if (anyChange) {
				ds.put(ent);
			}

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		if (anyChange) {
			// enqueue a task for updating this contestant's overall score
			Queue taskQueue = QueueFactory.getDefaultQueue();
			taskQueue.add(UpdateScoreTask.makeUrl()
				.param("user", userKey.getName())
				);
		}

		resp.setStatus(HttpServletResponse.SC_OK);
	}
}
