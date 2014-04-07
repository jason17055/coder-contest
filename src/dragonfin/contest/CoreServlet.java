package dragonfin.contest;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import dragonfin.templates.*;
import dragonfin.contest.model.*;

public class CoreServlet extends HttpServlet
{
	TemplateToolkit engine;

	public static String escapeUrl(String inStr)
	{
		try
		{
		return java.net.URLEncoder.encode(inStr, "UTF-8");
		}catch (UnsupportedEncodingException e)
		{
			throw new Error("unexpected: "+e.getMessage(), e);
		}
	}

	@Override
	public void init()
		throws ServletException
	{
		try
		{
			engine = new TemplateToolkit(
				new MyResourceLoader(getServletContext()));
		}
		catch (Exception e)
		{
			throw new ServletException("Error initializing Template Engine", e);
		}
	}

	public static class SessionAdapter
	{
		HttpSession session;
		SessionAdapter(HttpSession session)
		{
			this.session = session;
		}

		public Object get(String key)
		{
			return session.getAttribute(key);
		}
	}

	String makeContestUrl(String contestId, String page, String args)
	{
		return getServletContext().getContextPath() +
			"/" + contestId + "/" +
			page +
			(args != null && args.length() != 0 ? ("?"+args) : "");
	}

	static String fixUrl(HttpServletRequest req, String url)
	{
		String queryString;
		int qmark = url.indexOf('?');
		if (qmark != -1) {
			queryString = url.substring(qmark+1);
			url = url.substring(0, qmark);
		}
		else {
			queryString = "";
		}

		String servletPrefix = req.getContextPath();
		String relUrl;
		if (url.startsWith(servletPrefix)) {
			relUrl = url.substring(servletPrefix.length());
		}
		else {
			relUrl = url;
			servletPrefix = "";
		}

		ArrayList<String> queryArgs = new ArrayList<String>(
			Arrays.asList(queryString.split("&"))
			);
		boolean queryArgsChanged = false;

		if (relUrl.startsWith("/_p/")) {
			// find the contest query arg
			String contestId = null;
			for (Iterator<String> it = queryArgs.iterator(); it.hasNext(); )
			{
				String arg = it.next();
				if (arg.startsWith("contest=")) {
					contestId = arg.substring(8);
					it.remove();
					queryArgsChanged = true;
				}
			}
			relUrl = "/"+contestId+relUrl.substring(3);
		}

		if (queryArgsChanged) {
			StringBuilder sb = new StringBuilder();
			for (String arg : queryArgs) {
				if (sb.length() != 0) {
					sb.append("&");
				}
				sb.append(arg);
			}
			queryString = sb.toString();
		}

		return servletPrefix+relUrl+
			(queryString.length() != 0 ? ("?"+queryString) : "");
	}

	public static class RequestAdapter
	{
		HttpServletRequest request;
		RequestAdapter(HttpServletRequest request)
		{
			this.request = request;
		}

		public String getUri()
		{
			String s = request.getRequestURI();
			String q = request.getQueryString();
			String u = s + (q != null ? "?"+q : "");
			return fixUrl(request, u);
		}

		public String get(String param)
		{
			return request.getParameter(param);
		}
	}

	protected Bindings makeVars(HttpServletRequest req,
			Map<String,Object> args)
		throws Exception
	{
		SimpleBindings ctx = new SimpleBindings();
		ctx.put("resources_prefix",req.getContextPath());
		ctx.put("r", new RequestAdapter(req));
		ctx.put("g", new TemplateGlobals());

		HttpSession s = req.getSession(false);
		if (s != null)
		{
			ctx.put("s", new SessionAdapter(s));

			final String contestId = (String) s.getAttribute("contest");
			if (contestId != null) {
				makeContestVars(contestId, ctx);
			}

			final String userId = (String) s.getAttribute("username");
			Callable<UserInfo> userC = new Callable<UserInfo>()
			{
			public UserInfo call() throws Exception
			{
				return DataHelper.loadUser(contestId, userId);
			}
		};
		ctx.put("user", userC);

		}

		if (args != null)
			ctx.putAll(args);
		return ctx;
	}

	void makeContestVars(String contestId, Map<String,Object> ctx)
		throws DataHelper.NotFound
	{
		ContestInfo contest = DataHelper.loadContest(contestId);
		ctx.put("contest", contest);

		Map<String,String> links = new HashMap<String,String>();
		links.put("controller", makeContestUrl(contestId, "controller", null));
		links.put("submissions_list", makeContestUrl(contestId, "submissions", null));
		links.put("edit_self", makeContestUrl(contestId, "edit_self", null));
		links.put("scoreboard", makeContestUrl(contestId, "scoreboard", null));
		links.put("edit_contest", makeContestUrl(contestId, "edit_contest", null));
		links.put("problems_list", makeContestUrl(contestId, "problems", null));
		links.put("users_list", makeContestUrl(contestId, "users", null));
		links.put("new_problem", makeContestUrl(contestId, "new_problem", null));
		ctx.put("contest_links", links);
	}

	public void renderTemplate(HttpServletRequest req, HttpServletResponse resp,
			String templateName, Map<String,Object> args)
		throws ServletException, IOException
	{
		try
		{
		Bindings vars = makeVars(req, args);
		Writer out = resp.getWriter();
		engine.process(templateName, vars, out);
		out.close();
		}
		catch (IOException e) { throw e; }
		catch (SQLException e)
		{
			throw new ServletException("Database error", e);
		}
		catch (Exception e)
		{
			throw new ServletException("Template Engine error", e);
		}
	}

	public void renderTemplate(HttpServletRequest req, HttpServletResponse resp,
			String templateName)
		throws ServletException, IOException
	{
		renderTemplate(req, resp, templateName, null);
	}

	String getMyUrl(HttpServletRequest req)
	{
		String u = req.getRequestURI();
		String q = req.getQueryString();
		if (q != null) {
			u = u + "?" + q;
		}
		return fixUrl(req, u);
	}

	protected void showLoginPage(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String myUrl = getMyUrl(req);
		String newUrl = makeContestUrl(
			req.getParameter("contest"),
			"login",
			"next=" + escapeUrl(myUrl)
			);

		resp.sendRedirect(newUrl);
	}

	protected boolean notLoggedIn(HttpServletRequest req)
	{
		HttpSession s = req.getSession(false);
		return !(
			s != null &&
			s.getAttribute("contest") != null &&
			s.getAttribute("username") != null
			);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (notLoggedIn(req))
		{
			showLoginPage(req, resp);
			return;
		}

		String path = req.getPathInfo();
		renderTemplate(req, resp, "mytemplate.vm");
	}

	public static void main(String [] args)
		throws Exception
	{
	}
}

class MyResourceLoader
	implements ResourceLoader
{
	ServletContext servletContext;

	public MyResourceLoader(ServletContext ctx)
	{
		this.servletContext = ctx;
	}

	//implements ResourceLoader
	public InputStream getResourceStream(String source)
	{
		return servletContext.getResourceAsStream("/WEB-INF/templates/"+source);
	}
}