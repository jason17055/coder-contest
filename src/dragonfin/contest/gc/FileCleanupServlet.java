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

public class FileCleanupServlet extends AdminPageServlet
{
	static DateFormat df = DateFormat.getDateTimeInstance();

	void gatherReferences(DatastoreService ds, Key min, Key max, HashMap<Key,String> seen, String kind, String field)
	{
		Query q = new Query(kind);
		q.setFilter(CompositeFilterOperator.and(
			new FilterPredicate(field, GREATER_THAN_OR_EQUAL, min),
			new FilterPredicate(field, LESS_THAN, max)));

		PreparedQuery pq = ds.prepare(q);
		for (Entity ent : pq.asIterable()) {
			Key fileKey = (Key) ent.getProperty(field);
			seen.put(fileKey, String.format("%s.%s",
					ent.getKey().toString(),
					field));
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
		Key minKey = KeyFactory.createKey("File", minDigest);
		String maxDigest = req.getParameter("max") != null ? req.getParameter("max") : "~";
		Key maxKey = KeyFactory.createKey("File", maxDigest);

		HashMap<Key,String> seen = new HashMap<Key,String>();
		gatherReferences(ds, minKey, maxKey, seen, "Problem", "spec");
		gatherReferences(ds, minKey, maxKey, seen, "Problem", "solution");
		gatherReferences(ds, minKey, maxKey, seen, "Problem", "input_validator");
		gatherReferences(ds, minKey, maxKey, seen, "Problem", "output_validator");
		gatherReferences(ds, minKey, maxKey, seen, "SystemTest", "input");
		gatherReferences(ds, minKey, maxKey, seen, "SystemTest", "expected");
		gatherReferences(ds, minKey, maxKey, seen, "Result", "source");
		gatherReferences(ds, minKey, maxKey, seen, "Submission", "source");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "source");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "input");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "expected");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "actual");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "result_detail");
		gatherReferences(ds, minKey, maxKey, seen, "TestJob", "output");
		gatherReferences(ds, minKey, maxKey, seen, "TestResult", "input");
		gatherReferences(ds, minKey, maxKey, seen, "TestResult", "expected");
		gatherReferences(ds, minKey, maxKey, seen, "TestResult", "output");
		gatherReferences(ds, minKey, maxKey, seen, "TestResult", "error_output");

		Query q = new Query("File");
		q.setFilter(CompositeFilterOperator.and(
			new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, GREATER_THAN_OR_EQUAL, minKey),
			new FilterPredicate(Entity.KEY_RESERVED_PROPERTY, LESS_THAN, maxKey)));

		PrintWriter out = resp.getWriter();
		out.println("<html><body>");
		out.println("<h2>Files</h2>");
		out.println("<table border='1'>");

		PreparedQuery pq = ds.prepare(q);

		for (Entity ent : pq.asIterable()) {
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
			out.println("<td>"+head+"</td>");
			out.print("<td>");
			if (seen.containsKey(ent.getKey())) {
				String refInfo = seen.get(ent.getKey());
				out.print("ref by " + refInfo);
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
