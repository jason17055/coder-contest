package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class ListContestsServlet extends AdminPageServlet
{
	String getTemplate()
	{
		return "admin/list_contest.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireAdmin(req, resp)) {
			return;
		}

		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(final TemplateVariables tv, SimpleBindings ctx)
	{
		ctx.put("all_contests", new Callable< ArrayList<TemplateVariables.Contest> >() {
			public ArrayList<TemplateVariables.Contest> call() {
				return tv.getAll_contests();
			}});
	}
}
