package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;

public class ProblemCoderServlet extends CoreServlet
{
	public String getTemplate() {
		return "problem_write.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws EntityNotFoundException
	{
		String contestId = tv.req.getParameter("contest");
		String problemId = tv.req.getParameter("problem");

		TemplateVariables.Problem p = tv.fetchProblem(contestId, problemId);
		ctx.put("problem", p);

		HashMap<String,Object> form = new HashMap<String,Object>();
		form.put("source_name", "Main.java");
		form.put("source_content", "Hello world.");
		ctx.put("f", form);
	}
}
