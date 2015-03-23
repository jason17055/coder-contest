package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.fasterxml.jackson.core.*;
import com.google.appengine.api.datastore.*;

public class ListSubmissionsServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if ("json".equals(req.getParameter("format"))) {
			doGetJson(req, resp);
			return;
		}

		if (requireJudge(req, resp)) { return; }

		renderTemplate(req, resp, "list_submissions.tt");
	}

	void doGetJson(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireJudge(req, resp)) { return; }

		Collection<TemplateVariables.Submission> submissionsList;
		try {
			TemplateVariables tv = makeTemplateVariables(req);
			submissionsList = tv.getContest().getSubmissions();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		resp.setContentType("text/json;charset=UTF-8");
		JsonGenerator out = new JsonFactory().createJsonGenerator(resp.getWriter());
		out.writeStartArray();

		for (TemplateVariables.Submission s : submissionsList) {
			out.writeStartObject();
			out.writeStringField("id", s.id);
			out.writeStringField("type", s.type);
			out.writeStringField("hash", s.getHash());
			out.writeEndObject();
		}

		out.writeEndArray();
		out.close();
	}
}
