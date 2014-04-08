package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class GetFileServlet extends CoreServlet
{
	static Pattern PATTERN = Pattern.compile("^/([^/]+)/([^/]+)$");
	private static final Logger log = Logger.getLogger(
			GetFileServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		Matcher m = PATTERN.matcher(req.getPathInfo());
		if (!m.matches()) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String fileHash = m.group(1);
		String fileName = m.group(2);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key fileKey = KeyFactory.createKey("File", fileHash);
		Entity fileEnt;
		try {
			fileEnt = ds.get(fileKey);
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND, "File entity not found");
			return;
		}

		String contentType = (String)fileEnt.getProperty("content_type");
		resp.setHeader("Content-Type", contentType);

		OutputStream out = resp.getOutputStream();
		outputChunk(out, ds, (Key) fileEnt.getProperty("head_chunk"));
		out.close();
	}

	void outputChunk(OutputStream out, DatastoreService ds, Key chunkKey)
		throws IOException
	{
		Entity ent;
		try {
			ent = ds.get(chunkKey);
		}
		catch (EntityNotFoundException e) {
			log.warning("Chunk "+chunkKey+" not found");
			return;
		}

		@SuppressWarnings("unchecked")
		List<Key> partsList = (List<Key>) ent.getProperty("parts");
		if (partsList != null && !partsList.isEmpty()) {

			for (Key k : partsList) {
				outputChunk(out, ds, k);
			}
		}

		Blob b = (Blob) ent.getProperty("data");
		if (b != null) {

			out.write(b.getBytes());
		}
	}
}
