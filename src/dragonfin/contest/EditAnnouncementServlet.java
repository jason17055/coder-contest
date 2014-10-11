package dragonfin.contest;

import dragonfin.contest.common.*;

import java.io.*;
import java.util.*;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class EditAnnouncementServlet extends CoreServlet
{
	String getTemplate() {
		return "edit_announcement.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }
		renderTemplate(req, resp, getTemplate());
	}

	@Override
	void moreVars(TemplateVariables tv, SimpleBindings ctx)
	{
		HashMap<String,String> form = new HashMap<String,String>();
		form.put("recipient", "*");
		form.put("message", "");
		ctx.put("f", form);
	}
}
