package dragonfin.contest.broker;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;
import com.fasterxml.jackson.core.*;
import com.google.appengine.api.datastore.*;

import dragonfin.contest.FileUploadFormHelper;
import static dragonfin.contest.TemplateVariables.makeFileUrl;

public class TestFeedServlet extends HttpServlet
{
	FileUploadFormHelper uploadForm = new FileUploadFormHelper();

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		resp.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		JobQueue Q = (JobQueue) getServletContext().getAttribute("jobQueue");
		out.println("job queue: "+(Q != null ? "exists" : "does not exist"));
		out.close();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);
		if (POST.containsKey("action:register")) {

			doRegister(POST, req, resp);
		}
		else if (POST.containsKey("action:claim")) {

			doClaim(POST, req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
	}

	void doRegister(Map<String,String> POST, HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String contestId = req.getParameter("contest");

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		String workerId;
		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity ent = new Entity("Worker", contestKey);
			ent.setProperty("created", new Date());
			ent.setProperty("accepted_languages", POST.get("languages"));
			ent.setProperty("description", POST.get("description"));
			ent.setProperty("system", POST.get("system"));

			Key workerKey = ds.put(ent);
			workerId = Long.toString(workerKey.getId());

			Key statusKey = KeyFactory.createKey(workerKey, "WorkerStatus", 0);
			Entity statusEnt = new Entity(statusKey);
			statusEnt.setProperty("last_refreshed", new Date());
			ds.put(statusEnt);

			txn.commit();
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		resp.setContentType("text/json;charset=UTF-8");
		JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
		out.writeStartObject();
		out.writeStringField("worker_id", workerId);
		out.writeEndObject();
		out.close();
	}

	void doClaim(Map<String,String> POST, HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		String contestId = req.getParameter("contest");
		String workerId = POST.get("worker");
		if (contestId == null || workerId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key workerKey = KeyFactory.createKey(contestKey, "Worker", Long.parseLong(workerId));
		Key workerStatusKey = KeyFactory.createKey(workerKey, "WorkerStatus", 0);
		Entity statusEnt = new Entity(workerStatusKey);
		statusEnt.setProperty("last_refreshed", new Date());
		statusEnt.setProperty("worker_status", POST.get("worker_status"));
		ds.put(statusEnt);

		Query q = new Query("TestJob");
		q.setFilter(Query.CompositeFilterOperator.and(
			Query.FilterOperator.EQUAL.of("contest", contestId),
			Query.FilterOperator.EQUAL.of("claimed", Boolean.FALSE)
			));
		q.addSort("created");

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {
			if (tryClaim(ds, req, resp, workerKey, ent.getKey())) {
				return;
			}
		}

		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}

	boolean tryClaim(DatastoreService ds, HttpServletRequest req, HttpServletResponse resp, Key workerKey, Key jobKey)
		throws IOException
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

			String sourceUrl = null;
			try {
				Key sourceKey = (Key) ent.getProperty("source");
				String sourceId = sourceKey.getName();
				Entity sourceEnt = ds.get(sourceKey);
				String sourceName = (String) sourceEnt.getProperty("given_name");
				sourceUrl = makeFileUrl(req, sourceId, sourceName);
			}
			catch (EntityNotFoundException e) {
			}

			Key inputKey = (Key) ent.getProperty("input");
			String inputId = inputKey.getName();
			String inputUrl = makeFileUrl(req, inputId, "input.txt");

			Key expectedKey = (Key) ent.getProperty("expected");
			String expectedUrl = expectedKey != null ?
				makeFileUrl(req, expectedKey.getName(), "expected.txt") : null;

			Key actualKey = (Key) ent.getProperty("actual");
			String actualUrl = actualKey != null ?
				makeFileUrl(req, actualKey.getName(), "actual.txt") : null;

			String jobType = (String) ent.getProperty("type");
			long runtimeLimit = ent.hasProperty("runtime_limit") ?
				((Long)ent.getProperty("runtime_limit")).longValue() : 0;

			resp.setContentType("text/json;charset=UTF-8");
			JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
			out.writeStartObject();
			out.writeStringField("job_id", Long.toString(jobKey.getId()));
			out.writeStringField("type", jobType);
			out.writeStringField("source_file", sourceUrl);
			out.writeStringField("input_file", inputUrl);
			if (expectedUrl != null) {
				out.writeStringField("expected_file", expectedUrl);
			}
			if (actualUrl != null) {
				out.writeStringField("actual_file", actualUrl);
			}
			if (runtimeLimit != 0) {
				out.writeFieldName("timeout");
				out.writeNumber(runtimeLimit);
			}
			out.writeEndObject();
			out.close();

			return true;
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}
}
