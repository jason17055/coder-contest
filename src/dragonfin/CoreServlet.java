package dragonfin;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import dragonfin.templates.*;

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

	public void renderTemplate(HttpServletRequest req, HttpServletResponse resp,
			String templateName)
		throws ServletException, IOException
	{
		HashMap<String,Object> ctx = new HashMap<String,Object>();
		ctx.put("name", "Jason");
		ctx.put("resources_prefix",req.getContextPath());

		try
		{
		Writer out = resp.getWriter();
		engine.process(templateName, ctx, out);
		out.close();
		}
		catch (IOException e) { throw e; }
		catch (Exception e)
		{
			throw new ServletException("Template Engine error", e);
		}
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
