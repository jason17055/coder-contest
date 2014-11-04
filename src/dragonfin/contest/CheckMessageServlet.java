package dragonfin.contest;

import java.io.IOException;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.fasterxml.jackson.core.*;

import static dragonfin.contest.TemplateVariables.makeUserKey;
import static dragonfin.contest.TemplateVariables.makeTestResultId;
import static dragonfin.contest.TemplateVariables.parseTestResultId;

public class CheckMessageServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		CheckMessage m = new CheckMessage();
		m.req = req;
		m.resp = resp;
		m.timeout = req.getParameter("timeout");
		m.type = req.getParameter("type");
		m.after = req.getParameter("after");

		m.userKey = null;
		HttpSession sess = req.getSession(false);
		if (sess != null) {
			m.contestId = (String) sess.getAttribute("contest");
			String username = (String) sess.getAttribute("username");
			if (m.contestId != null && username != null) {
				m.userKey = makeUserKey(m.contestId, username);
				m.pokeUser();
			}
		}

		String [] job_ids = req.getParameterValues("jobcompletion");
		if (job_ids != null) {
			for (String jobId : job_ids) {
				m.addJobCompletionCheck(jobId);
			}
		}

		String [] test_ids = req.getParameterValues("testresultcompletion");
		if (test_ids != null) {
			for (String testResultId : test_ids) {
				m.addTestResultCompletionCheck(testResultId);
			}
		}

		String [] test_status = req.getParameterValues("testresultstatus");
		if (test_status != null) {
			for (String status_info : test_status) {
				int sep = status_info.indexOf("//");
				if (sep == -1) { continue; }
				String testResultId = status_info.substring(0,sep);
				String statusStr = status_info.substring(sep+2);
				m.addTestResultStatusCheck(testResultId, statusStr);
			}
		}

		if (m.checkMultiple()) {
			return;
		}

		String dismissMessage = req.getParameter("dismiss_message");
		if (dismissMessage != null) {
			m.dismissMessage(dismissMessage);
		}

		if (m.checkForMessage(req, resp)) {
			return;
		}

		resp.setContentType("text/json;charset=UTF-8");
		JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
		out.writeStartObject();
		out.writeEndObject();
		out.close();
	}

	class CheckMessage
	{
		HttpServletRequest req;
		HttpServletResponse resp;
		String contestId;
		Key userKey;
		DatastoreService ds;

		String timeout;
		String type;
		String after;

		ArrayList<Check> checks = new ArrayList<Check>();

		CheckMessage()
		{
			this.ds = DatastoreServiceFactory.getDatastoreService();
		}

		void pokeUser()
		{
			Transaction txn = ds.beginTransaction();
			try {
				Entity ent = ds.get(userKey);
				Date lastAccess = (Date) ent.getProperty("last_access");
				ent.setProperty("last_access", new Date());
				ds.put(ent);
				txn.commit();
			}
			catch (EntityNotFoundException e) {
				// just ignore
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}

		boolean checkMultiple()
			throws IOException
		{
			for (Check ch : checks) {
				boolean rv = ch.doCheck();
				if (rv) {
					
					resp.setContentType("text/json;charset=UTF-8");
					JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
					ch.emitMessage(out);
					out.close();
					return true;
				}
			}
			return false;
		}

		void addJobCompletionCheck(String jobId)
		{
			checks.add(new JobCompletionCheck(ds, jobId));
		}

		void addTestResultCompletionCheck(String testResultId)
		{
			if (contestId == null) { return; }
			checks.add(new TestResultStatusCheck(ds, contestId, testResultId, null));
		}

		void addTestResultStatusCheck(String testResultId, String status)
		{
			if (contestId == null) { return; }
			checks.add(new TestResultStatusCheck(ds, contestId, testResultId, status));
		}

		void dismissMessage(String messageId)
		{
			Key messageKey = KeyFactory.createKey(userKey, "Message", Long.parseLong(messageId));
			ds.delete(messageKey);
		}

		boolean checkForMessage(HttpServletRequest req, HttpServletResponse resp)
			throws IOException
		{
			if (userKey == null) {
				return false;
			}

			Query q = new Query("Message");
			q.setAncestor(userKey);
			q.addSort("created");

			PreparedQuery pq = ds.prepare(q);
			for (Entity ent : pq.asIterable()) {
				emitMessageDetails(ent);
				return true;
			}

			return false;
		}

		void emitMessageDetails(Entity ent)
			throws IOException
		{
			resp.setContentType("text/json;charset=UTF-8");
			JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
			out.writeStartObject();
			out.writeStringField("message_id", Long.toString(ent.getKey().getId()));
			out.writeStringField("message", (String)ent.getProperty("message"));
			out.writeStringField("url", (String)ent.getProperty("url"));
			out.writeStringField("messagetype", "N");
			out.writeEndObject();
			out.close();
		}
	}

	static abstract class Check
	{
		public abstract boolean doCheck();
		public abstract void emitMessage(JsonGenerator out)
			throws IOException;
	}

	static class JobCompletionCheck extends Check
	{
		final DatastoreService ds;
		final String jobId;
		Entity ent;

		JobCompletionCheck(DatastoreService ds, String jobId)
		{
			this.ds = ds;
			this.jobId = jobId;
		}

		@Override
		public boolean doCheck()
		{
			Key jobKey = KeyFactory.createKey("TestJob", Long.parseLong(jobId));
			try {

				this.ent = ds.get(jobKey);
				Boolean b = (Boolean) ent.getProperty("finished");
				if (b != null && b.booleanValue()) {
					return true;
				}
			}
			catch (EntityNotFoundException e) {
			}
			return false;
		}

		@Override
		public void emitMessage(JsonGenerator out) throws IOException
		{
			out.writeStartObject();
			out.writeStringField("class", "job_completed");
			out.writeStringField("job_id", Long.toString(ent.getKey().getId()));
			out.writeEndObject();
		}
	}

	static class TestResultStatusCheck extends Check
	{
		final DatastoreService ds;
		final Key testResultKey;
		final String expectedStatus;
		Entity ent;

		TestResultStatusCheck(DatastoreService ds, String contestId, String testResultId, String expectedStatus)
		{
			this.ds = ds;
			this.testResultKey = parseTestResultId(contestId, testResultId);
			this.expectedStatus = expectedStatus;
		}

		@Override
		public boolean doCheck()
		{
			String foundStatus;
			try {

				this.ent = ds.get(testResultKey);
				foundStatus = (String) ent.getProperty("result_status");
			}
			catch (EntityNotFoundException e) {

				foundStatus = null;
			}

			if (foundStatus == expectedStatus) {
				return false;
			}
			else if (foundStatus == null) {
				return true;
			}
			else {
				return !foundStatus.equals(expectedStatus);
			}
		}

		@Override
		public void emitMessage(JsonGenerator out) throws IOException
		{
			out.writeStartObject();
			out.writeStringField("class", "test_result_completed");
			out.writeStringField("test_result_id", makeTestResultId(testResultKey));
			out.writeEndObject();
		}
	}
}
