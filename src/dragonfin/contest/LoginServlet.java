package dragonfin.contest;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

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

			String contestId = req.getParameter("contest");
			String url = makeContestUrl(contestId, "login");
			resp.sendRedirect(url);
			return;
		}

		if (req.getParameter("ticket") != null)
		{
			doCasLogin(req, resp);
			return;
		}

		renderTemplate(req, resp, "login.tt");
	}

	private void doCasLogin(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Contest c;
		try {
			c = tv.getContest();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (c.auth_external == null || !c.auth_external.startsWith("cas:")) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		String casUrl = c.auth_external.substring(4);
		String appUrl = String.format("%s/%s/login",
			getBaseUrl(req),
			escapeUrl(c.id)
			);
		String ticketStr = req.getParameter("ticket");

		String vUrl = String.format("%svalidate?service=%s&ticket=%s",
			casUrl,
			appUrl,
			ticketStr);
		String line1 = null;
		String line2 = null;

		try {
			InputStream inStream = new URL(vUrl).openStream();
			BufferedReader br = new BufferedReader(
				new InputStreamReader(inStream, HBase64.UTF8)
				);
			line1 = br.readLine();
			line2 = br.readLine();
			br.close();
		}
		catch (Exception e) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		PrintWriter out = resp.getWriter();
		out.println("got validation response");
		out.println("line 1 :: "+line1);
		out.println("line 2 :: "+line2);
		out.close();
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
