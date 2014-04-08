package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import dragonfin.contest.model.File;
import com.google.appengine.api.datastore.*;
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DefineProblemServlet extends CoreServlet
{
	static final String TEMPLATE = "define_problem.tt";

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("id");

		if (requireDirector(req, resp)) { return; }

		Map<String,Object> form = new HashMap<String,Object>();
		if (problemId != null) {
			try {
			ProblemInfo prb = DataHelper.loadProblem(contestId, problemId);
			form.put("name", prb.name);
			form.put("visible", prb.visible ? "1" : "");
			form.put("allow_submissions", prb.allow_submissions ? "1" : "");
			form.put("judged_by", prb.judged_by);
			form.put("difficulty", Integer.toString(prb.difficulty));
			form.put("allocated_minutes", Integer.toString(prb.allocated_minutes));
			form.put("runtime_limit", Integer.toString(prb.runtime_limit));
			form.put("spec", prb.spec);
			form.put("solution", prb.solution);
			form.put("scoreboard_image", prb.scoreboard_image);
			form.put("score_by_access_time", prb.score_by_access_time ? "1" : "");
			form.put("start_time", prb.start_time);

			} catch (DataHelper.NotFound e) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
		}

		Map<String,Object> args = new HashMap<String,Object>();
		args.put("f", form);
		renderTemplate(req, resp, TEMPLATE, args);
	}

	static final Charset UTF8 = Charset.forName("UTF-8");
	static String readStream(InputStream in)
		throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		int nread;
		byte[] buf = new byte[8192];
		while ( (nread = in.read(buf)) != -1) {
			bytes.write(buf, 0, nread);
		}

		return new String(bytes.toByteArray(), UTF8);
	}

	static String byteArray2Hex(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02x", bytes[i]));
		}
		return sb.toString();
	}

	static class UploadHelper
	{
		DatastoreService ds;
		InputStream stream;
		byte [] buf = new byte[16*1024];

		static MessageDigest createDigestObject()
		{
			try {
				return MessageDigest.getInstance("SHA-1");
			}
			catch (NoSuchAlgorithmException e) {
				throw new Error("Unexpected "+e.getMessage(), e);
			}
		}

		MessageDigest md = createDigestObject();

		FilePart nextChunk()
			throws IOException
		{
			int nread = stream.read(buf);
			if (nread == -1) {
				return null;
			}

			FilePart p = new FilePart();
			p.data = Arrays.copyOfRange(buf, 0, nread);
			makeDigest(p);
			return p;
		}

		void makeDigest(FilePart p)
		{
			md.update(p.data);
			byte [] digestBytes = md.digest();
			p.digestHex = byteArray2Hex(digestBytes);
		}

		Key[] processStream(InputStream stream)
			throws IOException
		{
			this.stream = stream;

			ArrayList<Key> chunks = new ArrayList<Key>();

			FilePart p;
			while ( (p = nextChunk()) != null )
			{
				Key chunkKey = KeyFactory.createKey("FileChunk", p.digestHex);
				Entity ent = new Entity(chunkKey);
				ent.setProperty("data", p.data);
				ds.put(ent);

				chunks.add(chunkKey);
			}

			return chunks.toArray(new Key[0]);
		}
	}

	static class FilePart
	{
		byte [] data;
		String digestHex;
	}

	File handleFileUpload(FileItemStream item)
		throws ServletException, IOException
	{
		String fileName = item.getName();
		String contentType = item.getContentType();

		UploadHelper helper = new UploadHelper();
		helper.ds = DatastoreServiceFactory.getDatastoreService();

		InputStream stream = item.openStream();
		Key [] chunks = helper.processStream(stream);

		helper.md.reset();
		helper.md.update(fileName.getBytes(UTF8));
		helper.md.update((byte)0);
		helper.md.update(contentType.getBytes(UTF8));
		helper.md.update((byte)0);
		for (Key chunkKey : chunks) {
			String name = chunkKey.getName();
			helper.md.update(name.getBytes(UTF8));
		}

		byte[] digestBytes = helper.md.digest();
		String digestHex = byteArray2Hex(digestBytes);

		Key fileKey = KeyFactory.createKey("File", digestHex);
		Entity ent = new Entity(fileKey);

		ent.setProperty("uploaded", new Date());
		ent.setProperty("given_name", fileName);
		ent.setProperty("content_type", contentType);

		ent.setProperty("chunks", chunks);
		
		helper.ds.put(ent);

		File f = new File();
		f.id = digestHex;
		f.name = fileName;
		return f;
	}

	Map<String,String> processMultipartForm(HttpServletRequest req)
		throws ServletException, IOException
	{
		try {

		Map<String,String> formFields = new HashMap<String,String>();

		ServletFileUpload upload = new ServletFileUpload();
		FileItemIterator it = upload.getItemIterator(req);
		while (it.hasNext()) {
			FileItemStream item = it.next();

			if (item.isFormField()) {
				String name = item.getFieldName();
				String value = readStream(item.openStream());
				formFields.put(name, value);
			}
			else {
				String name = item.getFieldName();
				String fileName = item.getName();
				File f = handleFileUpload(item);
				formFields.put(name, f.name);
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
			doUpdateProblem(req, resp);
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
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");
		String problemName = POST.get("name");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Entity contestEnt = ds.get(contestKey);

			long problemId = contestEnt.hasProperty("last_problem_id") ?
				((Long)contestEnt.getProperty("last_problem_id")).longValue() : 0;
			problemId++;
			contestEnt.setProperty("last_problem_id", new Long(problemId));
			ds.put(contestEnt);

			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", problemId);
			Entity ent1 = new Entity(prbKey);
			ent1.setProperty("created", new Date());
			updateFromForm(ent1, POST);
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

	void updateFromFormInt(Entity ent1, Map<String,String> POST, String propName)
	{
		if (POST.containsKey(propName)) {
			try {
			long x = Long.parseLong(POST.get(propName));
			ent1.setProperty(propName, new Long(x));
			}
			catch (NumberFormatException e) {
				ent1.removeProperty(propName);
			}
		}
	}

	void updateFromForm(Entity ent1, Map<String,String> POST)
	{
		ent1.setProperty("name", POST.get("name"));
		ent1.setProperty("visible", POST.containsKey("visible") ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("allow_submissions", POST.containsKey("allow_submissions") ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("score_by_access_time", "Y".equals(POST.get("score_by_access_time")) ? Boolean.TRUE : Boolean.FALSE);
		ent1.setProperty("judged_by", POST.get("judged_by"));

		updateFromFormInt(ent1, POST, "difficulty");
		updateFromFormInt(ent1, POST, "allocated_minutes");
		updateFromFormInt(ent1, POST, "runtime_limit");
	}

	void doUpdateProblem(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		String contestId = req.getParameter("contest");
		String problemId = req.getParameter("id");
		if (requireDirector(req, resp)) { return; }

		@SuppressWarnings("unchecked")
		Map<String,String> POST = (Map<String,String>) req.getAttribute("POST");
		String problemName = POST.get("name");

		// TODO- check parameters

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = ds.beginTransaction();

		try {

			Key contestKey = KeyFactory.createKey("Contest", contestId);
			Key prbKey = KeyFactory.createKey(contestKey,
				"Problem", Long.parseLong(problemId));
			Entity ent1 = ds.get(prbKey);
			updateFromForm(ent1, POST);
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
}
