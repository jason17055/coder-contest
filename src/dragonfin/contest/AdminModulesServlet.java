package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.text.DateFormat;
import java.text.ParseException;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.modules.*;

public class AdminModulesServlet extends AdminPageServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireAdmin(req, resp)) {
			return;
		}

		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<form method='post'>");
		out.println("<p>Instances:");
		out.println("<input type='text' name='instance_count'>");
		out.println("<button type='submit' name='action:set_instances'>Set Num Instances</button>");
		out.println("</p>");
		out.println("<p>");
		out.println("<button type='submit' name='action:start_module'>Start Module</button>");
		out.println("<button type='submit' name='action:stop_module'>Stop Module</button>");
		out.println("</p>");
		out.println("</form>");
		out.println("</body>");
		out.println("</html>");
		out.close();
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireAdmin(req, resp)) {
			return;
		}

		ModulesService modules = ModulesServiceFactory.getModulesService();
		if (req.getParameter("action:set_instances") != null) {

			int instances = Integer.parseInt(req.getParameter("instance_count"));
			modules.setNumInstances("job-broker", "1", instances);
		}
		else if (req.getParameter("action:start_module") != null) {

			modules.startVersion("job-broker", "1");
		}
		else if (req.getParameter("action:stop_module") != null) {

			modules.stopVersion("job-broker", "1");
		}
		else {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
			return;
		}

		doGet(req, resp);
	}
}
