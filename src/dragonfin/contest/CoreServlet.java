package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;
import dragonfin.templates.*;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import com.google.appengine.api.datastore.*;

import static dragonfin.contest.common.CommonFunctions.escapeUrl;

public class CoreServlet extends HttpServlet
{
	TemplateToolkit engine;

	File checkFileUrl(File inFile)
	{
		if (inFile != null && inFile.id != null) {
			inFile.url = getServletContext().getContextPath()+"/_f/"+inFile.id+"/"+inFile.name;
		}
		return inFile;
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
		Callable<TemplateVariables.User> user;

		SessionAdapter(HttpSession session)
		{
			this.session = session;
		}

		public Object get(String key)
		{
			return session.getAttribute(key);
		}
	}

	String makeContestUrl(String contestId, String page)
	{
		return makeContestUrl(contestId, page, null);
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

		if (relUrl.startsWith("/_p/_problem/")) {
			// find the problem query arg
			String problemId = null;
			for (Iterator<String> it = queryArgs.iterator(); it.hasNext(); )
			{
				String arg = it.next();
				if (arg.startsWith("problem=")) {
					problemId = arg.substring(8);
					it.remove();
					queryArgsChanged = true;
				}
			}
			relUrl = "/_p/problem."+problemId+relUrl.substring(12);
		}

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
		ctx.put("images_url",req.getContextPath()+"/images");
		ctx.put("ajax_url",req.getContextPath()+"/_a");
		ctx.put("r", new RequestAdapter(req));
		ctx.put("g", new TemplateGlobals());

		final TemplateVariables tv = new TemplateVariables(req);
		ctx.put("contest", tv.getContest());

		Callable< ArrayList<TemplateVariables.Problem> > c1 = new Callable< ArrayList<TemplateVariables.Problem> >() {
			public ArrayList<TemplateVariables.Problem> call() throws Exception
			{
				return tv.getAll_problems();
			}
		};
		ctx.put("all_problems", c1);

		Callable< ArrayList<TemplateVariables.User> > c2 = new Callable< ArrayList<TemplateVariables.User> >() {
			public ArrayList<TemplateVariables.User> call() throws Exception
			{
				return tv.getAll_users();
			}
		};
		ctx.put("all_users", c2);

		Callable< ArrayList<TemplateVariables.User> > c3 = new Callable< ArrayList<TemplateVariables.User> >() {
			public ArrayList<TemplateVariables.User> call() throws Exception
			{
				return tv.getAll_contestants();
			}
		};
		ctx.put("all_contestants", c3);

		HttpSession s = req.getSession(false);
		if (s != null)
		{
			SessionAdapter sa = new SessionAdapter(s);
			ctx.put("session", sa);

			final String contestId = (String) s.getAttribute("contest");
			if (contestId != null) {
				makeContestVars(contestId, ctx);
			}

			final String userId = (String) s.getAttribute("username");
			Callable<TemplateVariables.User> userC = new Callable<TemplateVariables.User>() {
				public TemplateVariables.User call() throws Exception
				{
					return tv.fetchUser(contestId, userId);
				}
			};
			sa.user = userC;
		}

		moreVars(tv, ctx);

		if (args != null)
			ctx.putAll(args);
		return ctx;
	}

	void moreVars(TemplateVariables tv, SimpleBindings ctx)
		throws Exception
	{
	}

	void makeContestVars(final String contestId, Map<String,Object> ctx)
	{
		Map<String,String> links = new HashMap<String,String>();
		links.put("controller", makeContestUrl(contestId, "controller", null));
		links.put("submissions_list", makeContestUrl(contestId, "submissions", null));
		links.put("edit_self", makeContestUrl(contestId, "edit_self", null));
		links.put("scoreboard", makeContestUrl(contestId, "scoreboard", null));
		links.put("edit_contest", makeContestUrl(contestId, "rules", null));
		links.put("problems_list", makeContestUrl(contestId, "problems", null));
		links.put("users_list", makeContestUrl(contestId, "users", null));
		links.put("new_problem", makeContestUrl(contestId, "problem", null));
		links.put("new_user", makeContestUrl(contestId, "user", null));
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

	String getBaseUrl(HttpServletRequest req)
	{
		String scheme = req.getScheme();
		int port = req.getServerPort();
		int defaultPort = scheme.equals("https") ? 443 : 80;

		String myUrl = scheme + "://" + req.getServerName() + (port == defaultPort ? "" : ":"+port) + req.getContextPath();
		return myUrl;
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

	/** @return true if response has been sent. */
	boolean requireContest(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		String contestId = req.getParameter("contest");
		HttpSession s = req.getSession(false);
		if (s == null) {
			// not logged in
			showLoginPage(req, resp);
			return true;
		}

		String sesContestId = (String) s.getAttribute("contest");
		String username = (String) s.getAttribute("username");
		if (sesContestId == null || username == null) {
			// not logged in
			showLoginPage(req, resp);
			return true;
		}

		if (!sesContestId.equals(contestId)) {
			// session is for a different contest
			showLoginPage(req, resp);
			return true;
		}

		return false;
	}

	Key getLoggedInUserKey(HttpServletRequest req)
	{
		String contestId = req.getParameter("contest");
		HttpSession s = req.getSession(false);
		if (s == null) {
			throw new Error("Unexpected: should be logged in");
		}

		String sesContestId = (String) s.getAttribute("contest");
		String username = (String) s.getAttribute("username");
		if (sesContestId == null || username == null) {
			throw new Error("Unexpected: invalid session");
		}

		return KeyFactory.createKey("User", sesContestId + "/" + username);
	}

	/** @return true if response has been sent. */
	boolean requireDirector(HttpServletRequest req, HttpServletResponse resp)
		throws ServletException, IOException
	{
		String contestId = req.getParameter("contest");
		HttpSession s = req.getSession(false);
		if (s == null) {
			// not logged in
			showLoginPage(req, resp);
			return true;
		}

		String sesContestId = (String) s.getAttribute("contest");
		String username = (String) s.getAttribute("username");
		if (sesContestId == null || username == null) {
			// not logged in
			showLoginPage(req, resp);
			return true;
		}

		if (!sesContestId.equals(contestId)) {
			// session is for a different contest
			showLoginPage(req, resp);
			return true;
		}

		try {
		TemplateVariables tv = new TemplateVariables(req);
		TemplateVariables.User user = tv.fetchUser(contestId, username);
		if (!user.is_director) {

			// not a director
			resp.sendError(HttpServletResponse.SC_FORBIDDEN,
				"This page requires you to be contest director.");
			return true;
		}
		}
		catch (EntityNotFoundException e) {
		}

		return false;
	}

	void updateFromFormInt(Entity ent1, Map<String,String> POST, String propName)
	{
		if (POST.containsKey(propName)) {
			try {
			long x = Long.parseLong(POST.get(propName));
			ent1.setProperty(propName, new Long(x));
			}
			catch (NumberFormatException e) {
				ent1.removeProperty(propName);
			}
		}
	}

	void updateFromForm_file(Entity ent1, Map<String,String> POST, String propName)
	{
		String fileHash = POST.get(propName+"_upload");
		if (fileHash != null) {
			Key fileKey = KeyFactory.createKey("File", fileHash);
			ent1.setProperty(propName, fileKey);
			return;
		}

		if (POST.containsKey(propName+"_replace")) {
			// replace file with nothing
			ent1.removeProperty(propName);
		}
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
