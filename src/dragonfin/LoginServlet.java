package dragonfin;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class LoginServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		renderTemplate(req, resp, "login.vm");
	}
}
