package dragonfin.contest;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class ListSubmissionsServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireDirector(req, resp)) { return; }

		renderTemplate(req, resp, "list_submissions.tt");
	}
}
