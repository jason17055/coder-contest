package dragonfin.contest;

import dragonfin.Differencer;
import dragonfin.contest.common.File;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.fasterxml.jackson.core.*;

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
		if (file1 == null && file2 == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String content1;
		String content2;

		try {
			content1 = file1 != null ? file1.getText_content() : "";
			content2 = file2 != null ? file2.getText_content() : "";
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
		out.println(".o.auth div { border-left: 3px solid white; }");
		out.println(".o div.xxx { border-left: 3px solid #f00; color: #c00;}");
		out.println(".o div.missing { border-left: 3px solid #fff; color: #f00; font-style: italic; font-family: serif; }");
		out.println(".o.auth div.missing { display: none; }");
		out.println(".o.auth div.missing.hilited { display: block; width: 100%; border: 2px solid #4f4; }");
		out.println(".o div.hilited { background-color: #dfd; }");
		out.println(".o div.xxx.hilited { background-color: #fdd; }");
		out.println(".o div.xxx.hilited span { background-color: #fbb; }");
		out.println(".o div.missing.hilited { background-color: #fdd; }");
		out.println("</style>");
		out.println("<script type=\"text/javascript\" src=\""+req.getContextPath()+"/diff.js\"></script>");
		out.println("</head>");
		out.println("<body>");

		if (file1 != null && file2 != null) {

		out.print("<div class='o'>");
		ArrayList<Differencer.DiffSegment> diffs = doDiffHelper(out, content1, content2);
		out.println("</div>");

		out.println("<script type=\"text/javascript\"><!--");
		out.print("var tmpDiffInfo = ");

		out.flush();

		JsonGenerator j_out = new JsonFactory().createJsonGenerator(resp.getWriter());
		j_out.writeStartArray();
		for (Differencer.DiffSegment seg : diffs) {
			j_out.writeStartObject();
			j_out.writeFieldName("type");
			j_out.writeString(String.format("%c", seg.type));
			j_out.writeFieldName("lhs_start");
			j_out.writeNumber(seg.offset1);
			j_out.writeFieldName("lhs_end");
			j_out.writeNumber(seg.offset1+seg.length1);
			j_out.writeFieldName("rhs_start");
			j_out.writeNumber(seg.offset2);
			j_out.writeFieldName("rhs_end");
			j_out.writeNumber(seg.offset2+seg.length2);
			j_out.writeEndObject();
		}
		j_out.writeEndArray();
		j_out.flush();

		out.println(";");
		out.println("setDiffInfo(tmpDiffInfo);");
		out.println("//--></script>");
		} //end if two files specified
		else {

		out.print("<div class='o auth'>");
		doDiffExpected(out, content2);
		out.println("</div>");

		}

		out.println("</body>");
		out.println("</html>");
		out.close();
	}

	static String [] splitLines(String rawText)
	{
		ArrayList<String> rv = new ArrayList<String>();
		int start = 0;
		for (int i = 0; i < rawText.length(); i++) {
			if (rawText.charAt(i) == '\n') {
				rv.add(rawText.substring(start, i));
				start = i + 1;
			}
		}
		if (start < rawText.length()) {
			rv.add(rawText.substring(start));
		}
		return rv.toArray(new String[0]);
	}

	void doDiffExpected(PrintWriter out, String content)
	{
		String [] lines = splitLines(content);
		for (int lineCount = 0; lineCount < lines.length; lineCount++) {

			out.print("<div class='missing' id='line"+lineCount+"m'\n></div>");
			out.print("<div id='line"+lineCount+"'\n><span>" + escapeHtml(lines[lineCount])+"</span>&nbsp;</div>");
		}
		out.println("<div class='missing' id='line"+lines.length+"m'></div>");
	}

	ArrayList<Differencer.DiffSegment> doDiffHelper(PrintWriter out, String content1, String content2)
	{
		String [] lines1 = splitLines(content1);
		String [] lines2 = splitLines(content2);

		Differencer diff = new Differencer(lines1, lines2);
		Differencer.DiffSegment seg;
		ArrayList<Differencer.DiffSegment> rv = new ArrayList<Differencer.DiffSegment>();
		int lineCount = 0;
		while ( (seg=diff.nextSegment()) != null ) {

			rv.add(seg);
			int segLen = seg.getLength();
			if (seg.type == '-') {
				out.print("<div class='missing' id='line"+lineCount+"m'\n>*** "+segLen+(segLen==1 ? " line" : " lines") + " missing here ***</div>");
			}
			else {
				for (int i = 0; i < segLen; i++) {
					if (seg.type == '=') {
						out.print("<div id='line"+lineCount+"'\n><span>" + escapeHtml(seg.getLine(i)) + "</span>&nbsp;</div>");
					}
					else {
						out.print("<div id='line"+lineCount+"' class='xxx'\n><span>" + escapeHtml(seg.getLine(i)) + "</span>&nbsp;</div>");
					}
					lineCount++;
				}
			}
		}

		return rv;
	}

	static String escapeHtml(String s)
	{
		return s;
	}
}
