package dragonfin.contest;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.zip.*;
import javax.servlet.*;
import javax.servlet.http.*;
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
		ZipOutputStream out = new ZipOutputStream(resp.getOutputStream());

		for (Problem p : problems) {
			ZipEntry zipEntry = new ZipEntry(p.name + "/");
			out.putNextEntry(zipEntry);
			out.closeEntry();
			if (p.hasSpecFile()) {
				writeSpecFile(out, p);
			}
			if (p.hasSolutionFile()) {
				writeSolutionFile(out, p);
			}
			for (SystemTest st : p.getSystem_tests()) {
				writeSystemTest(out, p, st);
			}
		}

		out.close();
		log.info("finished zip file for " + tv.getContestId());
	}

	void writeFileContents(ZipOutputStream out, File f)
		throws EntityNotFoundException, IOException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Entity fileEnt = ds.get(f.getKey());
		outputChunk(out, ds, (Key) fileEnt.getProperty("head_chunk"));
	}

	void writeSpecFile(ZipOutputStream out, Problem p)
		throws IOException
	{
		try {
			File f = p.getSpec();
			ZipEntry zipEntry = new ZipEntry(String.format("%s/%s", p.name, f.name));
			out.putNextEntry(zipEntry);
			writeFileContents(out, f);
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s/spec: %s", p.name, e.getMessage()));
		}
	}

	void writeSolutionFile(ZipOutputStream out, Problem p)
		throws IOException
	{
		try {
			File f = p.getSolution();
			ZipEntry zipEntry = new ZipEntry(String.format("%s/%s", p.name, f.name));
			out.putNextEntry(zipEntry);
			writeFileContents(out, f);
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s/solution: %s", p.name, e.getMessage()));
		}
	}

	void writeSystemTest(ZipOutputStream out, Problem p, SystemTest st)
		throws IOException
	{
		if (st.hasInputFile()) {
			writeSystemTestInputFile(out, p, st);
		}
		if (st.hasExpectedFile()) {
			writeSystemTestExpectedFile(out, p, st);
		}
	}

	void writeSystemTestInputFile(ZipOutputStream out, Problem p, SystemTest st)
		throws IOException
	{
		String fileName = String.format("%s/in_%d.txt", p.name, st.getTest_number());
		try {
			File f = st.getInput();
			ZipEntry zipEntry = new ZipEntry(fileName);
			out.putNextEntry(zipEntry);
			writeFileContents(out, f);
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s: %s", fileName, e.getMessage()));
		}
	}

	void writeSystemTestExpectedFile(ZipOutputStream out, Problem p, SystemTest st)
		throws IOException
	{
		String fileName = String.format("%s/out_%d.txt", p.name, st.getTest_number());
		try {
			File f = st.getExpected();
			ZipEntry zipEntry = new ZipEntry(fileName);
			out.putNextEntry(zipEntry);
			writeFileContents(out, f);
		}
		catch (EntityNotFoundException e) {
			// ignore!?
			log.warning(String.format("error zipping %s: %s", fileName, e.getMessage()));
		}
	}
}
