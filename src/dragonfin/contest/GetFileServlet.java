package dragonfin.contest;

import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.File.outputChunk;

public class GetFileServlet extends CoreServlet
{
	static Pattern PATTERN = Pattern.compile("^/([^/]+)/([^/]+)$");
	private static final Logger log = Logger.getLogger(
			GetFileServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if ("/diff".equals(req.getPathInfo())) {
			doDiff(req, resp);
			return;
		}

		Matcher m = PATTERN.matcher(req.getPathInfo());
		if (!m.matches()) {
			log.info("bad request path-info \""+req.getPathInfo()+"\"");
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
		if ("text".equals(req.getParameter("type"))) {
			contentType = "text/plain";
		}
		resp.setHeader("Content-Type", contentType);

		OutputStream out = resp.getOutputStream();
		outputChunk(out, ds, (Key) fileEnt.getProperty("head_chunk"));
		out.close();
	}

	public void doDiff(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		File file1 = File.byId(req, req.getParameter("a"));
		File file2 = File.byId(req, req.getParameter("b"));

		String content1;
		String content2;
		try {
			content1 = file1.getText_content();
			content2 = file2.getText_content();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resp.setContentType("text/html;charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.println("<!DOCTYPE HTML>");
		out.println("<html>");
		out.println("<head>");
		out.println("<style type=\"text/css\">");
		out.println(".o div { border-left: 3px solid #4f4; padding-left: 6px; white-space: pre; font-family: monospace; }");
		out.println(".o div.xxx { border-left: 3px solid #f00; color: #c00;}");
		out.println(".o div.missing { border-left: 3px solid #fff; color: #f00; font-style: italic; font-family: serif; }");
		out.println(".o div.hilited { background-color: #dfd; }");
		out.println(".o div.xxx.hilited { background-color: #fdd; }");
		out.println(".o div.xxx.hilited span { background-color: #fbb; }");
		out.println(".o div.missing.hilited { background-color: #fdd; }");
		out.println("</style>");
		out.println("</head>");
		out.println("<body>");
		out.print("<div class='o'>");

		out.print(content2);
		out.println("</div>");
		out.println("</body>");
		out.println("</html>");
		out.close();
	}
}
