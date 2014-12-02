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
	ModulesService modules = ModulesServiceFactory.getModulesService();

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

		String curInstanceCount = "";
		try {
			int instances = modules.getNumInstances("job-broker", "1");
			curInstanceCount = Integer.toString(instances);
			out.println("<p>Current NumInstances: "+curInstanceCount+"</p>");

			String brokerHost = modules.getVersionHostname("job-broker", "1");
			out.println("<p>Broker: "+(brokerHost != null ? brokerHost : "null!")+"</p>");

			String defVersion = modules.getDefaultVersion("job-broker");
			out.println("<p>Default version: " +defVersion + "</p>");

			out.println("<p>Available modules:</p><ul>");
			for (String sm : modules.getModules()) {
				out.println("<li>"+sm + " ( ");
				for (String sv : modules.getVersions(sm)) {
					out.println(sv);
				}
				out.println(")</li>");
			}
			out.println("</ul>");
		}
		catch (Throwable e) {
			out.println("<p>Error: "+e.toString()+"</p>");
		}
		out.println("<p>Instances:");
		out.println("<input type='text' name='instance_count' value='"+curInstanceCount+"'>");
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
