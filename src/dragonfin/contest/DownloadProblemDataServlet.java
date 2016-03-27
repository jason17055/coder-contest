package dragonfin.contest;

import java.io.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.ThreadManager;
import com.google.appengine.api.datastore.*;

import dragonfin.contest.TemplateVariables.Problem;
import dragonfin.contest.TemplateVariables.SystemTest;
import dragonfin.contest.common.File;

import static dragonfin.contest.common.File.outputChunk;

public class DownloadProblemDataServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(DownloadProblemDataServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		TemplateVariables tv = makeTemplateVariables(req);
		ArrayList<Problem> problems;
		try {
			problems = tv.getContest().getAll_problems();
		} catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		log.info("building a zip file for " + tv.getContestId());

		resp.setHeader("Content-Type", "application/zip");
		resp.setHeader("Content-Disposition",
			String.format("attachment; filename=%s.zip",
				tv.getContestId()));

		ZipOutputStream out = new ZipOutputStream(resp.getOutputStream());

		ArrayDeque<FetchFileTask> files = new ArrayDeque<FetchFileTask>();
		for (Problem p : problems) {
			ZipEntry zipEntry = new ZipEntry(p.name + "/");
			out.putNextEntry(zipEntry);
			out.closeEntry();
			if (p.hasSpecFile()) {
				writeSpecFile(files, p);
			}
			if (p.hasSolutionFile()) {
				writeSolutionFile(files, p);
			}
			for (SystemTest st : p.getSystem_tests()) {
				writeSystemTest(files, p, st);
			}
		}

		log.info(String.format("queued up %d file fetches for %s.zip",
			files.size(), tv.getContestId()));
		ExecutorService executor = Executors.newFixedThreadPool(
			2, ThreadManager.currentRequestThreadFactory());

		try {
			FetchFileTask nextTask = null;
			while (!files.isEmpty() || nextTask != null) {
				FetchFileTask curTask = nextTask;
				if (!files.isEmpty()) {
					nextTask = files.remove();
					executor.execute(nextTask);
				} else {
					nextTask = null;
				}
				if (curTask != null) {
					out.putNextEntry(curTask.zipEntry);
					out.write(curTask.get());
				}
			}
		}
		catch (Exception e) {
			log.warning("error while making zip file: " + e.getMessage());
		}

		out.close();
		log.info("finished zip file for " + tv.getContestId() + ".zip");
	}

	static class FetchFileCallable implements Callable<byte[]>
	{
		File file;

		FetchFileCallable(File f)
		{
			this.file = f;
		}

		public byte[] call()
			throws EntityNotFoundException, IOException
		{
			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			Entity fileEnt = ds.get(this.file.getKey());
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			outputChunk(bytes, ds, (Key) fileEnt.getProperty("head_chunk"));
			return bytes.toByteArray();
		}
	}

	static class FetchFileTask extends FutureTask<byte[]>
	{
		ZipEntry zipEntry;

		FetchFileTask(ZipEntry zipEntry, File file)
		{
			super(new FetchFileCallable(file));
			this.zipEntry = zipEntry;
		}
	}

	void writeSpecFile(Queue<FetchFileTask> files, Problem p)
		throws IOException
	{
		try {
			File f = p.getSpec();
			ZipEntry zipEntry = new ZipEntry(String.format("%s/%s", p.name, f.name));
			files.add(new FetchFileTask(zipEntry, f));
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s/spec: %s", p.name, e.getMessage()));
		}
	}

	void writeSolutionFile(Queue<FetchFileTask> files, Problem p)
		throws IOException
	{
		try {
			File f = p.getSolution();
			ZipEntry zipEntry = new ZipEntry(String.format("%s/%s", p.name, f.name));
			files.add(new FetchFileTask(zipEntry, f));
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s/solution: %s", p.name, e.getMessage()));
		}
	}

	void writeSystemTest(Queue<FetchFileTask> files, Problem p, SystemTest st)
		throws IOException
	{
		if (st.hasInputFile()) {
			writeSystemTestInputFile(files, p, st);
		}
		if (st.hasExpectedFile()) {
			writeSystemTestExpectedFile(files, p, st);
		}
	}

	void writeSystemTestInputFile(Queue<FetchFileTask> files, Problem p, SystemTest st)
		throws IOException
	{
		String fileName = String.format("%s/in_%d.txt", p.name, st.getTest_number());
		try {
			File f = st.getInput();
			ZipEntry zipEntry = new ZipEntry(fileName);
			files.add(new FetchFileTask(zipEntry, f));
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s: %s", fileName, e.getMessage()));
		}
	}

	void writeSystemTestExpectedFile(Queue<FetchFileTask> files, Problem p, SystemTest st)
		throws IOException
	{
		String fileName = String.format("%s/out_%d.txt", p.name, st.getTest_number());
		try {
			File f = st.getExpected();
			ZipEntry zipEntry = new ZipEntry(fileName);
			files.add(new FetchFileTask(zipEntry, f));
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s: %s", fileName, e.getMessage()));
		}
	}
}
