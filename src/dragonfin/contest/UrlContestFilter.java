package dragonfin.contest;

import java.io.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class UrlContestFilter implements Filter
{
	@Override
	public void init(FilterConfig config)
		throws ServletException
	{
	}

	static Pattern CONTEST_PROBLEM_URL = Pattern.compile("^/([a-z0-9]{2,})/problem\\.([a-z0-9]{1,})/(.*)");
	static Pattern CONTEST_URL = Pattern.compile("^/([a-z0-9]{2,})/(.*)");

	@Override
	public void doFilter(ServletRequest sreq, ServletResponse resp, FilterChain chain)
		throws ServletException, IOException
	{
		HttpServletRequest req = (HttpServletRequest) sreq;
		String requestURI = req.getRequestURI();
		String localPath;
		if (requestURI.startsWith(req.getContextPath())) {
			localPath = requestURI.substring(req.getContextPath().length());
		}
		else {
			throw new ServletException("invalid request ("+requestURI+") for context ("+req.getContextPath()+")");
		}

		Matcher m;
		m = CONTEST_PROBLEM_URL.matcher(localPath);
		if (m.matches()) {
			boolean hasQueryString = req.getQueryString() != null;
			String newURI = "/_p/_problem/"+m.group(3)
				+ "?" + (hasQueryString ? (req.getQueryString()+"&") : "")
				+ "contest=" + m.group(1)
				+ "problem=" + m.group(2);
			req.getRequestDispatcher(newURI).forward(sreq, resp);
			return;
		}

		m = CONTEST_URL.matcher(localPath);
		if (m.matches()) {
			boolean hasQueryString = req.getQueryString() != null;
			String newURI = "/_p/"+m.group(2)
				+ "?" + (hasQueryString ? (req.getQueryString()+"&") : "")
				+ "contest=" + m.group(1);
			req.getRequestDispatcher(newURI).forward(sreq, resp);
		}
		else {
			chain.doFilter(sreq, resp);
		}
	}

	@Override
	public void destroy()
	{
	}
}
