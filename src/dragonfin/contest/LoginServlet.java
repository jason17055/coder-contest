package dragonfin.contest;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

public class LoginServlet extends CoreServlet
{
	private static final Logger log = Logger.getLogger(LoginServlet.class.getName());

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("logout") != null)
		{
			HttpSession s = req.getSession(false);
			if (s != null)
			{
				s.invalidate();
			}
			resp.sendRedirect("login");
			return;
		}

		renderTemplate(req, resp, "login.tt");
	}

	private void sendUserOnTheirWay(HttpServletRequest req,
			HttpServletResponse resp)
		throws IOException
	{
		String u = req.getParameter("next");
		if (u == null)
		{
			String contestId = req.getParameter("contest");
			u = makeContestUrl(contestId, "", null);
		}
		resp.sendRedirect(u);
	}

	private boolean checkUserLogin(HttpServletRequest req, HttpServletResponse resp)
		throws Exception
	{
		String contestId = req.getParameter("contest");
		String userId = req.getParameter("username");
		String password = req.getParameter("password");

		if (contestId == null || contestId.equals("")) {
			return false;
		}
		if (userId == null || userId.equals("")) {
			return false;
		}

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Key userKey = KeyFactory.createKey("User",
				contestId+"/"+userId);
		Entity ent;
		try {
			ent = ds.get(userKey);
		}
		catch (EntityNotFoundException e) {
			log.info("Login failure (User "+contestId+"/"+userId+" not found)");
			return false;
		}

		if (checkPassword(password, (String) ent.getProperty("password"))) {
			// login ok
			HttpSession s = req.getSession(true);
			s.setAttribute("contest", contestId);
			s.setAttribute("username", userId);

			s.setAttribute("is_director", ent.getProperty("is_director"));
			s.setAttribute("is_contestant", ent.getProperty("is_contestant"));
			s.setAttribute("is_judge", ent.getProperty("is_judge"));

			log.info("Login success (User "+contestId+"/"+userId+")");

			JobBroker.startBrokerIfNeeded();

			return true;
		}

		log.info("Login failure (User "+contestId+"/"+userId+" wrong password)");
		return false;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		try
		{
			if (checkUserLogin(req, resp))
			{
				sendUserOnTheirWay(req, resp);
			}
			else
			{
				HashMap<String,Object> args = new HashMap<String,Object>();
				args.put("message", "Error: invalid username/password");
				renderTemplate(req, resp, "login.tt", args);
			}
		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}

	static boolean checkPassword(String givenPassword, String actualPassword)
	{
		return givenPassword.equals(actualPassword);
	}
}
