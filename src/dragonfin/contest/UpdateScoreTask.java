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

import static dragonfin.contest.TemplateVariables.getContestFromUserKey;

public class UpdateScoreTask extends HttpServlet
{
	private static final Logger log = Logger.getLogger(UpdateScoreTask.class.getName());

	public static void enqueueTask_all(String contestId)
	{
		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(
			TaskOptions.Builder.withUrl("/_task/update_score")
			.param("contest", contestId)
			);
	}

	public static void enqueueTask(Key userKey)
	{
		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(
			TaskOptions.Builder.withUrl("/_task/update_score")
			.param("user", userKey.getName())
			);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		try {
			new MyTask(req, resp).run();
		}
		catch (EntityNotFoundException e) {

			log.warning("got error: "+e);
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

	class MyTask
	{
		HttpServletRequest req;
		HttpServletResponse resp;
		TemplateVariables tv;
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		long sumScore;
		long sumScoreAlt;

		MyTask(HttpServletRequest req, HttpServletResponse resp) {
			this.req = req;
			this.resp = resp;
			this.tv = new TemplateVariables(req);
		}

		void updateAllScores()
			throws EntityNotFoundException
		{
			log.info("update score for ALL users");

			String contestId = req.getParameter("contest");
			Key contestKey = KeyFactory.createKey("Contest", contestId);
			TemplateVariables.Contest contest = tv.fetchContest(contestKey);

			for (TemplateVariables.User u : contest.getUsers()) {
				enqueueTask(u.dsKey);
			}
		}

		void run()
			throws EntityNotFoundException
		{
			if (req.getParameter("contest") != null && req.getParameter("user") == null) {
				updateAllScores();
				return;
			}

			String userId = req.getParameter("user");
			Key userKey = KeyFactory.createKey("User", userId);

			log.info("updating score for user "+userId);

			String contestId = getContestFromUserKey(userKey);
			Key contestKey = KeyFactory.createKey("Contest", contestId);
			TemplateVariables.Contest contest = tv.fetchContest(contestKey);

			Query q = new Query("Result");
			q.setAncestor(userKey);
			PreparedQuery pq = ds.prepare(q);
			for (Entity resultEnt : pq.asIterable()) {

				// get the problem associated with this result
				Key problemKey = KeyFactory.createKey(contestKey, "Problem", resultEnt.getKey().getId());
				TemplateVariables.Problem p = tv.fetchProblem(problemKey);

				if (!p.onScoreboard(contest.current_phase_id)) {
					// invisible problem; skip
					continue;
				}

				sumScore += intProperty(resultEnt, "score");
				sumScoreAlt += intProperty(resultEnt, "score_alt");

			}

			boolean anyChange = false;
			Transaction txn = ds.beginTransaction();
			try {

				Entity userEnt = ds.get(userKey);
				if (intProperty(userEnt, "score") != sumScore) {
					anyChange = true;
					userEnt.setProperty("score", sumScore);
				}
				if (intProperty(userEnt, "score_alt") != sumScoreAlt) {
					anyChange = true;
					userEnt.setProperty("score_alt", sumScoreAlt);
				}
				if (anyChange) {
					ds.put(userEnt);
				}

				txn.commit();
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}

			if (anyChange) {
				log.info("new score for "+userId+": "+sumScore+"."+sumScoreAlt);
				// do something?
			}

			resp.setStatus(HttpServletResponse.SC_OK);
		}
	}

	static boolean booleanProperty(Entity ent, String propName)
	{
		Object v = ent.getProperty(propName);
		if (v instanceof Boolean) {
			return ((Boolean)v).booleanValue();
		}
		else {
			return false;
		}
	}

	static long intProperty(Entity ent, String propName)
	{
		Object v = ent.getProperty(propName);
		if (v instanceof Long) {
			return ((Long)v).longValue();
		}
		else {
			return 0;
		}
	}
}
