package dragonfin.contest.gc;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.CompositeFilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import dragonfin.contest.*;

import static com.google.appengine.api.datastore.Query.FilterOperator.*;

public class ChunkCleanupServlet extends AdminPageServlet
{
	static DateFormat df = DateFormat.getDateTimeInstance();

	void gatherFileReferences(DatastoreService ds, Key min, Key max, HashMap<Key,Key> seen)
	{
		Query q = new Query("File");
		q.setFilter(CompositeFilterOperator.and(
			new FilterPredicate("head_chunk", GREATER_THAN_OR_EQUAL, min),
			new FilterPredicate("head_chunk", LESS_THAN, max)));

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {
			Key chunkKey = (Key) ent.getProperty("head_chunk");
			seen.put(chunkKey, ent.getKey());
		}
	}

	void gatherChunkReferences(DatastoreService ds, Key min, Key max, HashMap<Key,Key> seen)
	{
		Query q = new Query("FileChunk")
			.setFilter(new FilterPredicate("parts", GREATER_THAN_OR_EQUAL, min))
			.setFilter(new FilterPredicate("parts", LESS_THAN, max));

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {
			@SuppressWarnings("unchecked")
			List<Key> parts = (List<Key>) ent.getProperty("parts");
			if (parts == null) {
				continue;
			}

			for (Key chunkKey : parts) {
				if (chunkKey.compareTo(min) >= 0 &&
					chunkKey.compareTo(max) < 0) {
					seen.put(chunkKey, ent.getKey());
				}
			}
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireAdmin(req, resp)) {
			return;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		String minDigest = req.getParameter("min") != null ? req.getParameter("min") : "!";
		Key minKey = KeyFactory.createKey("FileChunk", minDigest);
		String maxDigest = req.getParameter("max") != null ? req.getParameter("max") : "~";
		Key maxKey = KeyFactory.createKey("FileChunk", maxDigest);

		HashMap<Key,Key> seen = new HashMap<Key,Key>();
		gatherFileReferences(ds, minKey, maxKey, seen);
		gatherChunkReferences(ds, minKey, maxKey, seen);

		Query q = new Query("FileChunk");
		q.setFilter(CompositeFilterOperator.and(
			new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, GREATER_THAN_OR_EQUAL, minKey),
			new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, LESS_THAN, maxKey)));

		PrintWriter out = resp.getWriter();
		out.println("<html><body>");
		out.println("<h2>File Chunks</h2>");
		out.println("<table border='1'>");

		PreparedQuery pq = ds.prepare(q);

		for (Entity ent : pq.asIterable()) {
			String name = ent.getKey().getName();
			Date touched = (Date) ent.getProperty("last_touched");

			@SuppressWarnings("unchecked")
			List<Key> partsList = (List<Key>) ent.getProperty("parts");

			Blob bytes = (Blob) ent.getProperty("data");

			String content =
			partsList != null ? String.format("%d parts", partsList.size()) :
			bytes != null ? String.format("%d bytes", bytes.getBytes().length) : "";

			out.println("<tr>");
			out.println("<td>"+name+"</td>");
			out.println("<td>"+df.format(touched)+"</td>");
			out.println("<td>"+content+"</td>");
			out.print("<td>");
			if (seen.containsKey(ent.getKey())) {
				Key refKey = seen.get(ent.getKey());
				out.print("ref by " + refKey.getKind() + "&lt;" + refKey.getName() + "&gt;");
			} else {
				out.print("orphaned");
			}
			out.println("</td>");
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("</body></html>");
		out.close();
	}

}
