package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;
import javax.script.SimpleBindings;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.users.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

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

	String getTemplate()
	{
		return "admin/list_contest.tt";
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (!requireAdmin(req, resp)) {
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
