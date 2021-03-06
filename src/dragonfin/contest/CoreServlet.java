package dragonfin.contest;

import dragonfin.contest.common.*;
import dragonfin.contest.common.File;
import dragonfin.templates.*;

import java.io.*;
import java.text.SimpleDateFormat;
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
		public Callable<TemplateVariables.User> user;

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
			return getMyUrl(request);
		}

		public String getBase_url()
		{
			return getBaseUrl(request);
		}

		public String get(String param)
		{
			return request.getParameter(param);
		}
	}

	static final String ATTR_TEMPLATE = "dragonfin.contest.TemplateVariables";
	TemplateVariables makeTemplateVariables(HttpServletRequest req)
	{
		TemplateVariables tv = (TemplateVariables) req.getAttribute(ATTR_TEMPLATE);
		if (tv == null) {
			tv = new TemplateVariables(req);
			req.setAttribute(ATTR_TEMPLATE, tv);
		}
		return tv;
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

		final TemplateVariables tv = makeTemplateVariables(req);
		ctx.put("contest", tv.getContest());
		ctx.put("call_url", new Function() {
			public Object invoke(Bindings args) throws Exception
			{
				Object arg1 = args.get("1");
				if (arg1 != null) {
					return tv.makeCallUrl(arg1.toString());
				}
				else {
					return null;
				}
			}});
		ctx.put("format_time", new FormatTimeFunction(tv.getContest()));
		ctx.put("sort_for_scoreboard", new Function() {
			public Object invoke(Bindings args) throws Exception
			{
				Object arg1 = args.get("1");
				if (arg1 instanceof Collection) {
					@SuppressWarnings("unchecked")
					Collection<TemplateVariables.User> userList = (Collection<TemplateVariables.User>)arg1;
					return tv.sortForScoreboard(userList);
				}
				else {
					return null;
				}
			}});

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

		Callable< List<BalloonInfo> > balloonsC = new Callable< List<BalloonInfo> >() {
			public List<BalloonInfo> call() throws Exception
			{
				return enumBalloons();
			}
		};
		ctx.put("all_balloon_images", balloonsC);

		moreVars(tv, ctx);

		if (args != null)
			ctx.putAll(args);
		return ctx;
	}

	static class FormatTimeFunction implements Function
	{
		TimeZone tz;
		SimpleDateFormat df_sameDay;
		SimpleDateFormat df_sameWeek;
		SimpleDateFormat df_other;

		FormatTimeFunction(TemplateVariables.Contest contest)
		{
			df_sameDay = new SimpleDateFormat("h:mma");
			df_sameWeek = new SimpleDateFormat("h:mma E");
			df_other = new SimpleDateFormat("h:mma E d MMM");

			if (contest != null && contest.time_zone != null) {
				this.tz = TimeZone.getTimeZone(contest.time_zone);
				df_sameDay.setTimeZone(this.tz);
				df_sameWeek.setTimeZone(this.tz);
				df_other.setTimeZone(this.tz);
			}
		}

		public Object invoke(Bindings args) throws Exception
		{
			Object arg1 = args.get("1");
			if (arg1 instanceof Date) {
				return doFormatTime((Date)arg1);
			}
			else {
				return null;
			}
		}

		String doFormatTime(Date d)
		{
			Date curTime = new Date();
			long ageSeconds = Math.abs(curTime.getTime() - d.getTime()) / 1000;
			if (ageSeconds < 8*60*60) {
				//less than eight hours
				return df_sameDay.format(d);
			}
			else if (ageSeconds < 3*86400) {
				//less than three days
				return df_sameWeek.format(d);
			}
			else {
				return df_other.format(d);
			}
		}
	}

	public static class BalloonInfo
	{
		public String id;
		public String name;
	}

	List<BalloonInfo> enumBalloons()
		throws IOException
	{
		BufferedReader in = new BufferedReader(
			new InputStreamReader(
			getServletContext().getResourceAsStream("/images/scoreboard/balloons.txt")
			));
		try {

			ArrayList<BalloonInfo> list = new ArrayList<BalloonInfo>();
			String tmp;
			while ( (tmp = in.readLine()) != null ) {
				BalloonInfo b = new BalloonInfo();
				int eq = tmp.indexOf('=');
				if (eq != -1) {
					b.id = tmp.substring(0, eq);
					b.name = tmp.substring(eq+1);
				}
				else {
					b.id = tmp;
					b.name = tmp;
				}
				list.add(b);
			}
			return list;
		}
		finally {

			in.close();
		}
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
		links.put("new_announcement", makeContestUrl(contestId, "announcement", null));
		links.put("issue_credentials", makeContestUrl(contestId, "issue_credentials", null));
		links.put("download_all_problems", makeContestUrl(contestId, "download_all_problems", null));
		links.put("login", makeContestUrl(contestId, "login"));
		links.put("logout", makeContestUrl(contestId, "login?logout=1"));
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

	public static String getMyUrl(HttpServletRequest req)
	{
		String u = req.getRequestURI();
		String q = req.getQueryString();
		if (q != null) {
			u = u + "?" + q;
		}
		return fixUrl(req, u);
	}

	static String getBaseUrl(HttpServletRequest req)
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
		TemplateVariables tv = makeTemplateVariables(req);
		TemplateVariables.Contest c;
		try {
			c = tv.getContest();
		}
		catch (EntityNotFoundException e) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (c.auth_external != null && c.auth_external.startsWith("cas:")) {
			//TODO- save myUrl for later redirection
			String myUrl = getMyUrl(req);
			String appUrl = String.format("%s/%s/login",
				getBaseUrl(req),
				escapeUrl(c.id)
				);
			String newUrl = String.format("%slogin?service=%s",
				c.auth_external.substring(4),
				escapeUrl(appUrl));
			resp.sendRedirect(newUrl);
			return;
		}

		String myUrl = getMyUrl(req);
		String newUrl = makeContestUrl(
			req.getParameter("contest"),
			"login",
			"next=" + escapeUrl(myUrl)
			);

		resp.sendRedirect(newUrl);
	}

	/**
	 * Check that the user is logged into the requested contest.
	 */
	protected boolean isLoggedIn(HttpServletRequest req)
	{
		HttpSession s = req.getSession(false);
		if (!(
			s != null &&
			s.getAttribute("contest") != null &&
			s.getAttribute("username") != null
			))
		{
			return false;
		}

		String contestId = req.getParameter("contest");
		return (contestId != null && contestId.equals(s.getAttribute("contest")));
	}

	/**
	 * Opposite of isLoggedIn.
	 */
	protected boolean notLoggedIn(HttpServletRequest req)
	{
		return !isLoggedIn(req);
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

	static Key getLoggedInUserKey(HttpServletRequest req)
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

	boolean isDirector(HttpServletRequest req, HttpServletResponse resp)
	{
		Key userKey = getLoggedInUserKey(req);

		try {
			TemplateVariables tv = new TemplateVariables(req);
			TemplateVariables.User user = tv.fetchUser(userKey);
			return user.is_director;
		}
		catch (EntityNotFoundException e) {
			return false;
		}
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

		TemplateVariables tv = makeTemplateVariables(req);
		try {
			TemplateVariables.User user = tv.fetchUser(contestId, username);
			if (user.is_director) {

				// ok, user is a director
				return false;
			}
		}
		catch (EntityNotFoundException e) {
		}

		// not a director
		resp.sendError(HttpServletResponse.SC_FORBIDDEN,
			"This page requires you to be contest director.");
		return true;
	}

	boolean requireJudge(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (requireContest(req, resp)) { return true; }

		TemplateVariables tv = makeTemplateVariables(req);
		try {
			TemplateVariables.User u = tv.fetchUser(getLoggedInUserKey(req));
			if (u.is_director || u.is_judge) {
				return false;
			}
		}
		catch (EntityNotFoundException e) {
		}

		// not a judge or director
		resp.sendError(HttpServletResponse.SC_FORBIDDEN,
			"This page requires director or judge access.");
		return true;
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
