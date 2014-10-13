package dragonfin.contest;

import java.io.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.users.*;

public abstract class AdminPageServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(AdminPageServlet.class.getName());

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
			return true;
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
			return true;
		}
			
		return false;
	}
}
