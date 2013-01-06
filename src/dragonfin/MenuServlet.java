package dragonfin;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;

public class MenuServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (notLoggedIn(req))
		{
			showLoginPage(req, resp);
			return;
		}

		renderTemplate(req, resp, "menu.tt");
	}
}
