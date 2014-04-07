package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.charset.Charset;

public class DefineProblemServlet extends CoreServlet
{
	static final String TEMPLATE = "define_problem.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		renderTemplate(req, resp, TEMPLATE);
	}

	static final Charset UTF8 = Charset.forName("UTF-8");

	Map<String,String> processMultipartForm(HttpServletRequest req)
		throws ServletException, IOException
	{
		try {

		Map<String,String> formFields = new HashMap<String,String>();

		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator it = upload.getItemIterator(req);
		while (it.hasNext()) {
			FileItemStream item = it.next();
			InputStream stream = item.openStream();

			if (item.isFormField()) {
				String name = item.getFieldName();
				ByteArrayOutputStream bytes = new ByteArrayOutputStream();
				int nread;
				byte[] buf = new byte[8192];
				while ( (nread = stream.read(buf)) != -1) {
					bytes.write(buf, 0, nread);
				}

				String value = new String(bytes.toByteArray(), UTF8);
				formFields.put(name, value);
			}
			else {
				//TODO
				throw new ServletException("Unable to handle file upload at this time.");
			}
		}

		req.setAttribute("POST", formFields);
		return formFields;

		} catch (FileUploadException e) {
			throw new ServletException(e);
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Map<String,String> POST = processMultipartForm(req);

		if (POST.containsKey("action:cancel")) {
			doCancel(req, resp);
		}
		else if (POST.containsKey("action:create_problem")) {
			doCreateProblem(req, resp);
		}
		else {
			throw new ServletException("invalid POST");
		}
	}

	void doCancel(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String u = req.getParameter("next");
		if (u == null) {
			u = makeContestUrl(req.getParameter("contest"), "problems", null);
		}
		resp.sendRedirect(u);
	}

	void doCreateProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		requireDirector(req, resp);

		String problemName = req.getParameter("problem_name");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity contestEnt = ds.get(contestKey);

			int problemId = contestEnt.hasProperty("last_problem_id") ?
				((Integer)contestEnt.getProperty("last_problem_id")).intValue() : 0;
			problemId++;
			contestEnt.setProperty("last_problem_id", new Integer(problemId));
			ds.put(contestEnt);

			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", problemId);
			Entity ent1 = new Entity(prbKey);
			ent1.setProperty("name", problemName);
			ent1.setProperty("created", new Date());
			ds.put(ent1);

			txn.commit();
		}
		catch (EntityNotFoundException e) {
			throw new ServletException("Invalid contest", e);
		}
		finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

		doCancel(req, resp);
	}

	void requireDirector(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException
	{
		String contestId = req.getParameter("contest");
		HttpSession s = req.getSession(false);
		if (s == null) {
			throw new ServletException("Not logged in");
		}

		String sesContestId = (String) s.getAttribute("contest");
		String username = (String) s.getAttribute("username");
		if (sesContestId == null || username == null) {
			throw new ServletException("Not logged in");
		}

		if (!sesContestId.equals(contestId)) {
			throw new ServletException("Wrong contest");
		}

		try {
		UserInfo user = DataHelper.loadUser(contestId, username);
		if (!user.isDirector()) {
			throw new ServletException("Not a director");
		}
		}
		catch (DataHelper.NotFound e) {
			throw new ServletException("Not a director");
		}
	}
}
