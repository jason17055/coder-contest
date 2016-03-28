package dragonfin.contest;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.memcache.*;
import com.fasterxml.jackson.core.*;

import dragonfin.contest.TemplateVariables.Contest;
import dragonfin.contest.TemplateVariables.Submission;

import static dragonfin.contest.TemplateVariables.makeContestKey;
import static dragonfin.contest.TemplateVariables.makeUserKey;
import static dragonfin.contest.TemplateVariables.makeTestResultId;
import static dragonfin.contest.TemplateVariables.parseTestResultId;

public class CheckMessageServlet extends HttpServlet
{
	private static final Logger log = Logger.getLogger(
			CheckMessageServlet.class.getName());

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		CheckMessage m = new CheckMessage(req, resp);

		m.userKey = null;
		HttpSession sess = req.getSession(false);
		if (sess != null) {
			m.contestId = (String) sess.getAttribute("contest");
			String username = (String) sess.getAttribute("username");
			if (m.contestId != null && username != null) {
				m.userKey = makeUserKey(m.contestId, username);
				if (m.checkMessageDismissal()) {
					return;
				}
				m.pokeUser();
			}
		}

		JsonParser json = new JsonFactory().
			createJsonParser(req.getReader());
		while (json.nextToken() != null) {
			if (json.getCurrentToken() != JsonToken.FIELD_NAME) { continue; }

			String field = json.getCurrentName();
			if (field.equals("assertions")) {
				if (json.nextToken() != JsonToken.START_ARRAY) {
					throw new IOException("invalid JSON request");
				}
				while (json.nextToken() != JsonToken.END_ARRAY) {
					m.addAssertion(json.getText());
				}
			}
		}
		json.close();

		if (m.checkMultiple()) {
			return;
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
		TemplateVariables tv;
		String contestId;
		Key userKey;
		DatastoreService ds;

		String timeout;
		String type;
		String after;

		ArrayList<Check> checks = new ArrayList<Check>();

		CheckMessage(HttpServletRequest req, HttpServletResponse resp)
		{
			this.req = req;
			this.resp = resp;
			this.timeout = req.getParameter("timeout");
			this.type = req.getParameter("type");
			this.after = req.getParameter("after");
			this.tv = new TemplateVariables(req);
			this.ds = DatastoreServiceFactory.getDatastoreService();
		}

		boolean checkMessageDismissal()
			throws IOException
		{
			String dismissMsgId = req.getParameter("dismiss_message");
			if (dismissMsgId == null) {
				return false;
			}

			dismissMessage(dismissMsgId);

			resp.setContentType("text/json;charset=UTF-8");
			JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
			out.writeStartObject();
			out.writeStringField("class", "dismissed_message");
			out.writeStringField("message_id", dismissMsgId);
			out.writeEndObject();
			out.close();
			return true;
		}

		void pokeUser()
		{
			MemcacheService syncCache = MemcacheServiceFactory.getMemcacheService();
			String k = "last_access["+userKey.getName()+"]";
			syncCache.put(k, new Date());
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

		void addAssertion(String assertion)
			throws IOException
		{
			Matcher m = Pattern.compile("^([^=]+)=(.*)$").matcher(assertion);
			if (!m.matches()) {
				throw new IOException("Invalid assertion");
			}

			String assertionType = m.group(1);
			String data = m.group(2);

			if (assertionType.equals("jobcompletion")) {
				addJobCompletionCheck(data);
			}
			else if (assertionType.equals("testresultcompletion")) {
				addTestResultCompletionCheck(data);
			}
			else if (assertionType.equals("testresultstatus")) {
				int sep = data.indexOf("//");
				if (sep == -1) { return; }
				String testResultId = data.substring(0, sep);
				String statusStr = data.substring(sep+2);
				addTestResultStatusCheck(testResultId, statusStr);
			}
			else if (assertionType.equals("useronline")) {
				int sep = data.indexOf(',');
				if (sep == -1) { return; }
				String username = data.substring(0, sep);
				String status = data.substring(sep+1);
				addUserStatusCheck(username, status);
			}
			else if (assertionType.equals("submissionslist")) {
				checks.add(new SubmissionsListCheck(tv, data));
			}
			else {
				log.warning("Unrecognized assertion type: " + assertionType);
			}
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

		void addUserStatusCheck(String username, String status)
		{
			if (contestId == null) { return; }
			checks.add(new UserStatusCheck(tv, contestId, username, status));
		}

		void dismissMessage(String messageId)
		{
			Transaction txn = ds.beginTransaction();
			try {

			if (messageId.startsWith("A:")) {
				Entity userEnt = ds.get(userKey);
				long announcementNumber = Long.parseLong(messageId.substring(2));
				Key annKey = KeyFactory.createKey(makeContestKey(contestId), "Announcement", announcementNumber);
				userEnt.setProperty("last_announcement_seen", annKey);
				ds.put(userEnt);
			}
			else {
				Key messageKey = KeyFactory.createKey(userKey, "Message", Long.parseLong(messageId));
				Entity mEnt = ds.get(messageKey);
				mEnt.setProperty("dismissed", Boolean.TRUE);
				ds.put(mEnt);
			}

			txn.commit();
			}
			catch (EntityNotFoundException e) {
				// shouldn't happen, but safe to ignore
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}

		boolean checkForMessage(HttpServletRequest req, HttpServletResponse resp)
			throws IOException
		{
			if (userKey == null) {
				return false;
			}

			MessageResult mr1 = fetchFirstMessage();
			MessageResult mr2 = fetchNextAnnouncement();

			if (mr1.ent != null && mr2.ent != null) {
				Date msgDate = (Date) mr1.ent.getProperty("created");
				Date annDate = (Date) mr2.ent.getProperty("created");
				if (msgDate.compareTo(annDate) < 0) {
					emitMessageDetails(mr1.ent, mr1.totalCount+mr2.totalCount);
				}
				else {
					emitMessageDetails(mr2.ent, mr1.totalCount+mr2.totalCount);
				}
				return true;
			}
			else if (mr1.ent != null) {
				emitMessageDetails(mr1.ent, mr1.totalCount+mr2.totalCount);
				return true;
			}
			else if (mr2.ent != null) {
				emitMessageDetails(mr2.ent, mr1.totalCount+mr2.totalCount);
				return true;
			}

			return false;
		}

		class MessageResult {
			Entity ent;
			int totalCount;

			void hit(Entity ent)	
			{
				if (this.ent == null) {
					this.ent = ent;
				}
				this.totalCount++;
			}
		}

		MessageResult fetchFirstMessage()
		{
			MessageResult mr = new MessageResult();

			Query q = new Query("Message");
			q.setAncestor(userKey);
			q.setFilter(
				Query.FilterOperator.EQUAL.of("dismissed", Boolean.FALSE)
				);
			q.addSort("created");

			PreparedQuery pq = ds.prepare(q);
			for (Entity ent : pq.asIterable()) {
				mr.hit(ent);
			}

			return mr;
		}

		MessageResult fetchNextAnnouncement()
		{
			MessageResult mr = new MessageResult();

			try {
			Entity userEnt = ds.get(userKey);
			Key lastAnnouncement = (Key) userEnt.getProperty("last_announcement_seen");
			long lastNumber = lastAnnouncement != null ? lastAnnouncement.getId() : 0;

			Query q = new Query("Announcement");
			q.setAncestor(makeContestKey(contestId));
			q.setFilter(
				Query.FilterOperator.GREATER_THAN.of("number", lastNumber)
				);
			q.addSort("number");

			PreparedQuery pq = ds.prepare(q);
			for (Entity ent : pq.asIterable()) {
				String rcpt = (String) ent.getProperty("recipient_group");
				if (rcpt != null && rcpt.equals("*")) {
					mr.hit(ent);
				}
			}

			}
			catch (EntityNotFoundException e) {}

			return mr;
		}

		void emitMessageDetails(Entity ent, int messageCount)
			throws IOException
		{
			String msgId;
			if (ent.getKind().equals("Announcement")) {
				msgId = "A:"+Long.toString(ent.getKey().getId());
			}
			else {
				msgId = Long.toString(ent.getKey().getId());
			}

			Date messageDate = (Date) ent.getProperty("created");
			SimpleDateFormat JSON_DATE_FMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			JSON_DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));


			resp.setContentType("text/json;charset=UTF-8");
			JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
			out.writeStartObject();
			out.writeStringField("class", "message");
			out.writeStringField("message_id", msgId);
			out.writeStringField("message", (String)ent.getProperty("message"));
			out.writeStringField("url", (String)ent.getProperty("url"));
			out.writeStringField("messagetype",
				ent.getKind().equals("Announcement") ? "A" : "N");
			out.writeNumberField("messagecount", messageCount);
			out.writeStringField("message_date",
				JSON_DATE_FMT.format(messageDate));
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

	static class SubmissionsListCheck extends Check
	{
		final TemplateVariables tv;
		String contestId;
		String filter;
		String [] items;

		SubmissionsListCheck(TemplateVariables tv, String data)
		{
			this.tv = tv;
			int comma = data.indexOf(',');
			if (comma != -1) {
				contestId = data.substring(0, comma);
				data = data.substring(comma + 1);
			}
			comma = data.indexOf(',');
			if (comma != -1) {
				filter = data.substring(0, comma);
				data = data.substring(comma + 1);
			}
			items = data.split(",");
		}

		@Override
		public boolean doCheck()
		{
			try {

			Contest c = tv.fetchContest(contestId);
			ArrayList<Submission> submissions = c.getSubmissionsFiltered(filter);
			if (submissions.size() != items.length) {
				return true;
			}
			for (int i = 0; i < items.length; i++) {
				if (!submissions.get(i).getHash().equals(items[i])) {
					return true;
				}
			}
			return false;

			} catch (EntityNotFoundException e) {
				log.warning(e.getMessage());
				return false;
			}
		}

		@Override
		public void emitMessage(JsonGenerator out) throws IOException
		{
			out.writeStartObject();
			out.writeStringField("class", "submissions_list_changed");
			out.writeEndObject();
		}
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

	static class UserStatusCheck extends Check
	{
		final TemplateVariables tv;
		final Key userKey;
		final String status;
		final String username;

		UserStatusCheck(TemplateVariables tv, String contestId, String username, String status)
		{
			this.tv = tv;
			this.userKey = makeUserKey(contestId, username);
			this.status = status;
			this.username = username;
		}

		@Override
		public boolean doCheck()
		{
			boolean isOnline = tv.checkUserOnline(userKey);
			if (isOnline && !status.equals("Y")) {
				return true;
			}
			else if (!isOnline && status.equals("Y")) {
				return true;
			}
			else {
				return false;
			}
		}

		@Override
		public void emitMessage(JsonGenerator out) throws IOException
		{
			out.writeStartObject();
			out.writeStringField("class", "online_status_change");
			out.writeStringField("user", username);
			out.writeEndObject();
		}
	}
}
