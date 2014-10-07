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
		if (Q != null) {
			out.println("pending jobs: "+Q.pendingJobs.size());
			out.println("active jobs: "+Q.activeJobs.size());
		}
		out.close();
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = uploadForm.processMultipartForm(req);
		if (POST.containsKey("action:claim")) {

			doClaim(POST, req, resp);
		}
		else {
			resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED);
		}
	}

	void doClaim(Map<String,String> POST, HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		//options expected to be in the URL
		String contestId = req.getParameter("contest");
		String workerId = req.getParameter("worker");
		if (contestId == null || workerId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		// options expected in the POST body
		String languagesS = POST.get("languages");
		if (languagesS == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		//
		// update worker status
		//
		String newWorkerStatus = POST.get("worker_status");

		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key workerKey = KeyFactory.createKey(contestKey, "Worker", Long.parseLong(workerId));
		Key workerStatusKey = KeyFactory.createKey(workerKey, "WorkerStatus", 1);
		Entity statusEnt = new Entity(workerStatusKey);
		statusEnt.setProperty("last_refreshed", new Date());
		statusEnt.setProperty("worker_status", newWorkerStatus);
		ds.put(statusEnt);

		//
		// look for (and possibly wait for) a job
		//
		JobQueue Q = (JobQueue) getServletContext().getAttribute("jobQueue");
		if (Q == null) {
			throw new ServletException("jobQueue not initialized");
		}

		HashSet<String> languages = new HashSet<String>();
		String [] languagesArr = languagesS.split(",");
		for (String tmp : languagesArr) {
			languages.add(tmp);
		}

		Entity ent = Q.claim(contestId, languages, workerKey, 10000);
		if (ent == null) {

			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Key jobKey = ent.getKey();

		String sourceUrl = null;
		String sourceId = null;
		try {
			Key sourceKey = (Key) ent.getProperty("source");
			sourceId = sourceKey.getName();
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

		resp.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		out.printf("id %s\n", Long.toString(jobKey.getId()));
		out.printf("type %s\n", jobType);
		if (sourceId != null) {
			out.printf("hash %s\n", sourceId);
		}
		if (sourceUrl != null) {
			out.printf("source_file %s\n", sourceUrl);
		}
		if (inputUrl != null) {
			out.printf("input_file %s\n", inputUrl);
		}
		if (expectedUrl != null) {
			out.printf("expected_file %s\n", expectedUrl);
		}
		if (actualUrl != null) {
			out.printf("actual_file %s\n", actualUrl);
		}
		if (runtimeLimit != 0) {
			out.printf("timeout %d\n", runtimeLimit);
		}
		out.close();
	}
}
