package dragonfin;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
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

	public static class RequestAdapter
	{
		HttpServletRequest request;
		RequestAdapter(HttpServletRequest request)
		{
			this.request = request;
		}

		public String get(String param)
		{
			return request.getParameter(param);
		}
	}

	protected Map<String,Object> makeVars(HttpServletRequest req,
			Map<String,Object> args)
	{
		HashMap<String,Object> ctx = new HashMap<String,Object>();
		ctx.put("resources_prefix",req.getContextPath());
		ctx.put("s", new SessionAdapter(req.getSession(false)));
		ctx.put("r", new RequestAdapter(req));
		ctx.put("g", new TemplateGlobals());
		if (args != null)
			ctx.putAll(args);
		return ctx;
	}

	public void renderTemplate(HttpServletRequest req, HttpServletResponse resp,
			String templateName, Map<String,Object> args)
		throws ServletException, IOException
	{
		Map<String, Object> vars = makeVars(req, args);

		try
		{
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

	void showLoginPage(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String newUrl = req.getContextPath()+"/login?next=" + escapeUrl(req.getRequestURI());
		resp.sendRedirect(newUrl);
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		HttpSession s = req.getSession(false);
		if (s == null || s.getAttribute("uid") == null)
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
