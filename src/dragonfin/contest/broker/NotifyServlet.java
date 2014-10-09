package dragonfin.contest.broker;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.*;
import javax.servlet.http.*;

public class NotifyServlet extends HttpServlet
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String jobId = req.getParameter("job");
		if (jobId == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		JobQueue Q = (JobQueue) getServletContext().getAttribute("jobQueue");
		boolean isNew = Q.maybeNewJob(jobId);

		resp.setContentType("text/plain;charset=UTF-8");
		PrintWriter out = resp.getWriter();
		out.println("ok");
		out.println(isNew ? "thanks for the tip!" : "already known");
		out.close();
	}

}
