package dragonfin.contest.broker;

import javax.servlet.*;

public class ContextListener implements ServletContextListener
{
	//implements ServletContextListener
	public void contextInitialized(ServletContextEvent evt)
	{
		ServletContext ctx = evt.getServletContext();
		ctx.setAttribute("jobQueue", new JobQueue());
	}

	//implements ServletContextListener
	public void contextDestroyed(ServletContextEvent evt)
	{
	}
}
