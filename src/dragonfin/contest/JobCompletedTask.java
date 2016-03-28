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

import dragonfin.Differencer;
import dragonfin.contest.common.File;
import static dragonfin.contest.GetFileServlet.splitLines;
import static dragonfin.contest.TemplateVariables.defaultResultEntity;
import static dragonfin.contest.TemplateVariables.getContestFromUserKey;

public class JobCompletedTask extends HttpServlet
{
	private static final Logger log = Logger.getLogger(JobCompletedTask.class.getName());

	static void enqueueTask(long jobId)
	{
		Queue taskQueue = QueueFactory.getDefaultQueue();
		taskQueue.add(TaskOptions.Builder.withUrl("/_task/job_completed")
			.param("job", Long.toString(jobId))
			);
	}

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		MyHandler h = new MyHandler(req, resp);
		h.doPost();
	}

	class MyHandler
	{
		final HttpServletRequest req;
		final HttpServletResponse resp;
		DatastoreService ds;
		Entity ent; //job entity

		Key problemKey;
		Key testResultKey;
		Entity problemEnt;
		Entity systemTestEnt;

		MyHandler(HttpServletRequest req, HttpServletResponse resp) {
			this.req = req;
			this.resp = resp;
			this.ds = DatastoreServiceFactory.getDatastoreService();
		}

		void gatherEntities()
			throws EntityNotFoundException
		{
			String jobId_s = req.getParameter("job");
			long jobId = Long.parseLong(jobId_s);

			Key jobKey = KeyFactory.createKey("TestJob", jobId);
			this.ent = ds.get(jobKey);

			this.problemKey = (Key)ent.getProperty("problem");
			if (problemKey != null) {
				this.problemEnt = ds.get(problemKey);
			}

			this.testResultKey = (Key)ent.getProperty("test_result");
			if (testResultKey != null) {
				long systemTestNumber = testResultKey.getId();
				Key systemTestKey = KeyFactory.createKey(problemKey, "SystemTest", systemTestNumber);
				this.systemTestEnt = ds.get(systemTestKey);
			}
		}

		void doPost()
			throws IOException
		{
			try {
				gatherEntities();
			}
			catch (EntityNotFoundException e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}

			String jobType = (String)ent.getProperty("type");
			if ("S".equals(jobType)) {

				systemTest_step1();
			}
		}		

		boolean treatOutputAsText()
		{
			return problemEnt != null &&
				problemEnt.hasProperty("output_is_text") &&
				((Boolean) problemEnt.getProperty("output_is_text")).booleanValue();
		}

		void systemTest_step1()
			throws IOException
		{
			if (this.testResultKey == null) {
				return;
			}

			String resultStatus = (String)ent.getProperty("result_status");
			if (!"No Error".equals(resultStatus)) {
				// user program generated an error result
				resolveSystemTest(testResultKey, resultStatus);
				return;
			}

			// compare output with expected output
			Key outputFileKey = (Key)ent.getProperty("output");
			Key expectedFileKey = (Key)systemTestEnt.getProperty("expected");

			String status;
			try {
				File outputFile = fetchFile(outputFileKey);
				File expectedFile = fetchFile(expectedFileKey);

				if (treatOutputAsText()) {
 					status = compareTextFiles(expectedFile, outputFile);
				} else {
 					status = compareBinaryFiles(expectedFile, outputFile);
				}
			}
			catch (EntityNotFoundException e) {
				status = UNKNOWN;
			}
			resolveSystemTest(testResultKey, status);
		}

		String compareBinaryFiles(File file1, File file2)
		{
			if (file1 == null || file2 == null) {
				if (file1 == file2) {
					return EXACT_MATCH;
				} else {
					return DIFFERENT;
				}
			}

			if (file1.hash.equals(file2.hash)) {
				return EXACT_MATCH;
			} else {
				return DIFFERENT;
			}
		}

		String compareTextFiles(File file1, File file2)
			throws EntityNotFoundException, IOException
		{
			String content1 = file1 != null ? file1.getText_content() : "";
			String content2 = file2 != null ? file2.getText_content() : "";

			String [] lines1 = splitLines(content1);
			String [] lines2 = splitLines(content2);
			Differencer diff = new Differencer(lines1, lines2);
			Differencer.DiffSegment seg;

			boolean foundWhitespaceDifferences = false;
			int linesExtra = 0;
			int linesMissing = 0;
			int linesDifferent = 0;
			int lineCount = 0;
			while ( (seg = diff.nextSegment()) != null ) {

				lineCount += seg.length1;
				if (seg.type == '-') {
					linesMissing += seg.length1;
				}
				else if (seg.type == '+') {
					linesExtra += seg.length2;
				}
				else if (seg.type == '~') {
					foundWhitespaceDifferences = true;
				}
				else if (seg.type == '!') {
					linesDifferent += seg.length1;
				}
			}

			int typeCount = (linesExtra > 0 ? 1 : 0) +
					(linesMissing > 0 ? 1 : 0) +
					(linesDifferent > 0 ? 1 : 0);
			if (typeCount > 1) {
				// More than one kind of difference found, not
				// easy to summarize... just say "Different".
				return DIFFERENT;
			}
			else if (linesDifferent > 0) {
				return String.format("%d of %d %s differ",
					linesDifferent, lineCount,
					lineCount == 1 ? "line" : "lines");
			}
			else if (linesExtra > 0) {
				return String.format("%d extra %s", linesExtra,
					linesExtra == 1 ? "line" : "lines");
			}
			else if (linesMissing > 0) {
				return String.format("Missing %d of %d %s",
					linesMissing, lineCount,
					lineCount == 1 ? "line" : "lines");
			}

			// On reaching here, either the file is exact match or
			// the only differences are in whitespace.
			if (foundWhitespaceDifferences) {
				return "Whitespace Differences";
			}
			else {
				return EXACT_MATCH;
			}
		}

		File fetchFile(Key key)
			throws EntityNotFoundException
		{
			if (key == null) {
				return null;
			}

			Entity ent = ds.get(key);
			File f = new File(req);
			f.id = key.getName();
			f.name = (String) ent.getProperty("given_name");
			f.hash = ((Key) ent.getProperty("head_chunk")).getName();
			return f;
		}

		void resolveSystemTest(Key testResultKey, String status)
			throws IOException
		{
			Key inputFileKey = (Key) ent.getProperty("input");
			Key outputFileKey = (Key) ent.getProperty("output");
			Key errorOutputFileKey = (Key) ent.getProperty("result_detail");
			Key expectedFileKey = (Key) systemTestEnt.getProperty("expected");

			Transaction txn = ds.beginTransaction();
			try {

				Entity resultEnt = ds.get(testResultKey);
				resultEnt.setProperty("job", ent.getKey());
				resultEnt.setProperty("result_status", status);
				resultEnt.setProperty("input", inputFileKey);
				resultEnt.setProperty("output", outputFileKey);
				resultEnt.setProperty("error_output", errorOutputFileKey);
				resultEnt.setProperty("expected", expectedFileKey);
				ds.put(resultEnt);
				txn.commit();

			}
			catch (EntityNotFoundException e) {

				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			finally {
				if (txn.isActive()) {
					txn.rollback();
				}
			}
		}
	}

	static final String EXACT_MATCH = "Exact Match";
	static final String DIFFERENT = "Different";
	static final String UNKNOWN = "Unknown";
}
