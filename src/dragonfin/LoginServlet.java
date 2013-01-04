package dragonfin;

import java.io.*;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.sql.*;
import dragonfin.contest.model.*;

public class LoginServlet extends CoreServlet
{
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		if (req.getParameter("logout") != null)
		{
			HttpSession s = req.getSession(false);
			if (s != null)
			{
				s.invalidate();
			}
			resp.sendRedirect("login");
			return;
		}

		renderTemplate(req, resp, "login.vm");
	}

	private void sendUserOnTheirWay(HttpServletRequest req,
			HttpServletResponse resp)
		throws IOException
	{
		String u = req.getParameter("next");
		if (u == null)
		{
			u = ".";
		}
		resp.sendRedirect(u);
	}

	private boolean checkUserLogin(HttpServletRequest req, HttpServletResponse resp)
		throws Exception
	{
		Connection db = Database.getConnection();
		try
		{

		PreparedStatement stmt = db.prepareStatement(
			"SELECT team_number,is_contestant,is_judge,is_director"
			+" FROM team"
			+" WHERE contest=?"
			+" AND user=?"
			+" AND password=SHA1(?)"
			);
		stmt.setString(1, req.getParameter("contest"));
		stmt.setString(2, req.getParameter("username"));
		stmt.setString(3, req.getParameter("password"));
		ResultSet rs = stmt.executeQuery();
		if (rs.next())
		{
			// found team login
			HttpSession s = req.getSession(true);
			s.setAttribute("uidnumber", rs.getString(1));
			s.setAttribute("contest", req.getParameter("contest"));
			if (rs.getString(2).equals("Y"))
				s.setAttribute("is_contestant", Boolean.TRUE);
			else
				s.removeAttribute("is_contestant");
			if (rs.getString(3).equals("Y"))
				s.setAttribute("is_judge", Boolean.TRUE);
			else
				s.removeAttribute("is_judge");
			if (rs.getString(4).equals("Y"))
				s.setAttribute("is_director", Boolean.TRUE);
			else
				s.removeAttribute("is_director");
			s.setAttribute("uid", req.getParameter("contest")+"/"+req.getParameter("username"));
			s.setAttribute("username", req.getParameter("username"));
			return true;
		}
		return false;

		}
		finally
		{
			db.close();
		}
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		try
		{
			if (checkUserLogin(req, resp))
			{
				sendUserOnTheirWay(req, resp);
			}
			else
			{
				HashMap<String,Object> args = new HashMap<String,Object>();
				args.put("message", "Error: invalid username/password");
				renderTemplate(req, resp, "login.vm", args);
			}
		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}
}
