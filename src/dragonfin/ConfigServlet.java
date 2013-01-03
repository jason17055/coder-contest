package dragonfin;

import java.io.*;
import javax.servlet.http.*;

public class ConfigServlet extends HttpServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException
	{
		String uploadedFilesPath = getServletContext().getInitParameter("uploadedFilesPath");
		String databaseName = getServletContext().getInitParameter("databaseName");

		PrintWriter out = resp.getWriter();
		out.println("<html>");
		out.println("<body>");
		out.println("<table border=\"1\">");
		out.println("<tr><td>uploadedFilesPath</td>");
		out.println("<td>"+uploadedFilesPath+"</td></tr>");
		out.println("<tr><td>databaseName</td>");
		out.println("<td>"+databaseName+"</td></tr>");
		out.println("</table>");
		out.println("</body>");
		out.println("</html>");
	}
}
