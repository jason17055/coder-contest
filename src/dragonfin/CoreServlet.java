package dragonfin;

import org.apache.velocity.*;
import org.apache.velocity.runtime.resource.*;
import org.apache.velocity.runtime.resource.loader.*;
import org.apache.velocity.app.VelocityEngine;
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class CoreServlet extends HttpServlet
{
	VelocityEngine engine;

	@Override
	public void init()
		throws ServletException
	{
		try
		{
			engine = new VelocityEngine();
			engine.setProperty("resource.loader", "mine");
			engine.setProperty("mine.resource.loader.instance", new MyResourceLoader(getServletContext()));
			engine.init();
		}
		catch (Exception e)
		{
			throw new ServletException("Error initializing Velocity Engine", e);
		}
	}

	public void renderTemplate(HttpServletRequest req, HttpServletResponse resp,
			String templateName)
		throws ServletException, IOException
	{
		VelocityContext ctx = new VelocityContext();
		ctx.put("name", "Jason");

		try
		{
		Template tmpl = engine.getTemplate(templateName);
		Writer out = resp.getWriter();
		tmpl.merge(ctx, out);
		out.close();
		}
		catch (IOException e) { throw e; }
		catch (Exception e)
		{
			throw new ServletException("Velocity Engine error", e);
		}
	}

	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		renderTemplate(req, resp, "mytemplate.vm");
	}

	public static void main(String [] args)
		throws Exception
	{
	}
}

class MyResourceLoader extends ResourceLoader
{
	ServletContext servletContext;

	public MyResourceLoader(ServletContext ctx)
	{
		this.servletContext = ctx;
	}

	@Override
	public void init(org.apache.commons.collections.ExtendedProperties configuration)
	{
	}

	@Override
	public InputStream getResourceStream(String source)
	{
		return servletContext.getResourceAsStream("/WEB-INF/templates/"+source);
	}

	@Override
	public boolean isSourceModified(Resource rsrc)
	{
		return false;
	}

	@Override
	public long getLastModified(Resource rsrc)
	{
		return 0;
	}
}
