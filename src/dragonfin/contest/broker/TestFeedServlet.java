package dragonfin.contest.broker;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.fasterxml.jackson.core.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.modules.*;

import dragonfin.contest.common.FileUploadFormHelper;
import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class TestFeedServlet extends HttpServlet
{
	FileUploadFormHelper uploadForm = new FileUploadFormHelper();
	ModulesService modulesApi = ModulesServiceFactory.getModulesService();

	static final long DEFAULT_TIME_LIMIT = 10000;

	private static final Logger log = Logger.getLogger(TestFeedServlet.class.getName());

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

		long timeLimit = DEFAULT_TIME_LIMIT;
		String timeLimitS = POST.get("timeout");
		if (timeLimitS != null) {
			timeLimit = (long)Math.round(1000*Double.parseDouble(timeLimitS));
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		//
		// update worker status
		//
		Key contestKey = KeyFactory.createKey("Contest", contestId);
		Key workerKey = KeyFactory.createKey(contestKey, "Worker", Long.parseLong(workerId));
		try {
			String newWorkerStatus = POST.get("worker_status");
			postWorkerStatus(ds, workerKey, newWorkerStatus);
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

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

		Entity ent = Q.claim(contestId, languages, workerKey, timeLimit);
		if (ent == null) {

			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		sendJobDetails(resp, ds, ent);
	}

	void postWorkerStatus(DatastoreService ds, Key workerKey, String newWorkerStatus)
		throws EntityNotFoundException
	{
		Transaction txn = ds.beginTransaction();
		try {

			Entity ent = ds.get(workerKey);
			ent.setProperty("last_refreshed", new Date());
			ent.setProperty("worker_status", newWorkerStatus);
			ds.put(ent);

			txn.commit();
		}
		catch (ConcurrentModificationException e) {
			// ignore
			log.warning("postWorkerStatus failed, " + e.getMessage());
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}

	void sendJobDetails(HttpServletResponse resp, DatastoreService ds, Entity ent)
		throws IOException
	{
		Key jobKey = ent.getKey();

		String sourceUrl = null;
		String sourceId = null;
		try {
			Key sourceKey = (Key) ent.getProperty("source");
			sourceId = sourceKey.getName();
			Entity sourceEnt = ds.get(sourceKey);
			String sourceName = (String) sourceEnt.getProperty("given_name");
			sourceUrl = makeFileUrl(sourceId, sourceName);
		}
		catch (EntityNotFoundException e) {
		}

		Key inputKey = (Key) ent.getProperty("input");
		String inputUrl;
		if (inputKey != null) {
			String inputId = inputKey.getName();
			inputUrl = makeFileUrl(inputId, "input.txt");
		}
		else {
			inputUrl = null;
		}

		Key expectedKey = (Key) ent.getProperty("expected");
		String expectedUrl = expectedKey != null ?
			makeFileUrl(expectedKey.getName(), "expected.txt") : null;

		Key actualKey = (Key) ent.getProperty("actual");
		String actualUrl = actualKey != null ?
			makeFileUrl(actualKey.getName(), "actual.txt") : null;

		String jobType = (String) ent.getProperty("type");
		long runtimeLimit = ent.hasProperty("runtime_limit") ?
			((Long)ent.getProperty("runtime_limit")).longValue() : 0;

		resp.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = resp.getWriter();

		out.printf("id %s\n", Long.toString(jobKey.getId()));
		out.printf("post_result_to %s\n", makeResultUrl(jobKey));
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

	String makeFileUrl(String fileId, String fileName)
	{
		return getUrlPrefix() + "/_f/" + escapeUrl(fileId) +
			"/" + escapeUrl(fileName);
	}

	String makeResultUrl(Key jobKey)
	{
		String jobId = Long.toString(jobKey.getId());
		return getUrlPrefix() + "/_w/post_job_result?job="+escapeUrl(jobId);
	}

	String getUrlPrefix()
	{
		String v = modulesApi.getCurrentVersion();
		return "http://" + modulesApi.getVersionHostname("default", v);
	}
}
