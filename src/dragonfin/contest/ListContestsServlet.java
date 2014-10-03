package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.contest.model.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.*;

public class ListContestsServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(ListContestsServlet.class.getName());

	void redirectSystemLoginScreen(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		UserService userService = UserServiceFactory.getUserService();
		String loginUrl = userService.createLoginURL(getMyUrl(req));
		resp.sendRedirect(loginUrl);
	}

	boolean requireAdmin(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		if (req.getUserPrincipal() == null) {
			log.info("no user principal");
			redirectSystemLoginScreen(req, resp);
			return false;
		}
		log.info("user principal is "+req.getUserPrincipal());

		UserService userService = UserServiceFactory.getUserService();
		if (!userService.isUserAdmin()) {
			resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			resp.setContentType("text/html;charset=UTF-8");
			PrintWriter out = resp.getWriter();
			out.println("<html><body>");
			out.println("This page requires administrative access.");
			out.println("<p>");
		String logoutUrl = userService.createLogoutURL(getMyUrl(req));
			out.println("<a href=\""+logoutUrl+"\">Logout</a>");
			out.println("</body></html>");
			out.close();
			return false;
		}
			
		return true;
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (!requireAdmin(req, resp)) {
			return;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		Query q = new Query("Contest");
		PreparedQuery pq = ds.prepare(q);

		ArrayList<ContestInfo> list = new ArrayList<ContestInfo>();
		for (Entity ent : pq.asIterable()) {
			String contestId = ent.getKey().getName();
			String creator = (String) ent.getProperty("created_by");
			ContestInfo c = new ContestInfo();
			c.id = contestId;
			c.created_by = creator;
			list.add(c);
		}

		HashMap<String,Object> args = new HashMap<String,Object>();
		args.put("all_contests", list);
		renderTemplate(req, resp, "admin/list_contest.tt", args);
	}
}
