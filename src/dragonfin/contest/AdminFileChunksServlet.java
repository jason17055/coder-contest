package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class AdminFileChunksServlet extends CoreServlet
{
	static DateFormat df = DateFormat.getDateTimeInstance();

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("FileChunk");
		if (req.getParameter("older_than") != null) {
			try {
			String tmpStr = req.getParameter("older_than");
			Date d = df.parse(tmpStr);
			q.setFilter(new Query.FilterPredicate(
				"last_touched",
				Query.FilterOperator.LESS_THAN,
				d));
			}
			catch (ParseException e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"older_than: " +e.getMessage());
				return;
			}
		}

		PrintWriter out = resp.getWriter();
		out.println("<html><body>");
		out.println("<h2>File Chunks</h2>");
		out.println("<form method='post' action='chunks'>");
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
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("<div>");
		out.println("<input type='text' name='chunk' size='40'>");
		out.println("<button type='submit' name='action:delete_chunk'>Delete Chunk</button>");
		out.println("</div>");
		out.println("<div>");
		out.println("Date filter: <input type='text' name='date_filter' size='40'");
		if (req.getParameter("older_than") != null) {
			out.println("value='"+req.getParameter("older_than")+"'");
		}
		out.println(">");
		out.println("<button type='submit' name='action:filter_chunks'>Preview</button>");
		out.println("<button type='submit' name='action:delete_old_chunks'>Delete</button>");
		out.println("</div>");

		out.println("<h2>Files</h2>");
		out.println("<table border='1'>");

		Query q1 = new Query("File") ;
		PreparedQuery pq1 = ds.prepare(q1);

		for (Entity ent : pq1.asIterable()) {
			String name = ent.getKey().getName();
			Date uploaded = (Date) ent.getProperty("uploaded");
			String fileName = (String)ent.getProperty("given_name");
			String contentType = (String)ent.getProperty("content_type");
			String url = req.getContextPath()+"/_f/"+name+"/"+fileName;
			Key head = (Key)ent.getProperty("head_chunk");

			out.println("<tr>");
			out.println("<td>"+name+"</td>");
			out.println("<td>"+df.format(uploaded)+"</td>");
			out.println("<td><a href='"+url+"'>"+fileName+"</a></td>");
			out.println("<td>"+contentType+"</td>");
			out.println("<td>"+head+"</td>");
			out.println("</tr>");
		}
		out.println("</table>");
		out.println("<div>");
		out.println("<input type='text' name='file_id' size='40'>");
		out.println("<button type='submit' name='action:delete_file'>Delete File</button>");
		out.println("</div>");

		out.println("</form>");
		out.println("</body></html>");
		out.close();
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("action:delete_chunk") != null) {

			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			String chunkName = req.getParameter("chunk");
			Key chunkKey = KeyFactory.createKey("FileChunk", chunkName);
			ds.delete(chunkKey);

			resp.sendRedirect("chunks");
		}
		else if (req.getParameter("action:delete_file") != null) {

			DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
			String fileName = req.getParameter("file_id");
			Key fileKey = KeyFactory.createKey("File", fileName);
			ds.delete(fileKey);

			resp.sendRedirect("chunks");
		}
		else if (req.getParameter("action:filter_chunks") != null) {
			String x = req.getParameter("date_filter");
			resp.sendRedirect("chunks?older_than="+x);
		}
		else if (req.getParameter("action:delete_old_chunks") != null) {
			String x = req.getParameter("date_filter");
			try {
			Date d = df.parse(x);
			doDeleteOldChunks(d);
			resp.sendRedirect("chunks");
			}
			catch (ParseException e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Date parse error: "+e.getMessage());
			}
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
	}

	void doDeleteOldChunks(Date date)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("FileChunk");
		q.setFilter(new Query.FilterPredicate(
			"last_touched",
			Query.FilterOperator.LESS_THAN,
			date));
		PreparedQuery pq = ds.prepare(q);

		for (Entity ent : pq.asIterable()) {
			ds.delete(ent.getKey());
		}
	}
}
