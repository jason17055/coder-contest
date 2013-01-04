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

	private boolean checkDirectorLogin(HttpServletRequest req,
			HttpServletResponse resp, Connection db)
		throws Exception
	{
		PreparedStatement stmt = db.prepareStatement(
			"SELECT contest_id"
			+" FROM contest"
			+" WHERE contest_id=?"
			+" AND director=?"
			+" AND director_password=SHA1(?)"
			);
		stmt.setString(1, req.getParameter("contest"));
		stmt.setString(2, req.getParameter("username"));
		stmt.setString(3, req.getParameter("password"));
		ResultSet rs = stmt.executeQuery();
		if (rs.next())
		{
			// found director login
			HttpSession s = req.getSession(true);
			s.removeAttribute("is_team");
			s.removeAttribute("is_judge");
			s.removeAttribute("is_sysadmin");
			s.setAttribute("is_director", rs.getString(1));
			s.setAttribute("uid", req.getParameter("contest")+"/"+req.getParameter("username"));
			s.setAttribute("username", req.getParameter("username"));
			return true;
		}
		return false;
	}

	private boolean checkJudgeLogin(HttpServletRequest req,
			HttpServletResponse resp, Connection db)
		throws Exception
	{
		PreparedStatement stmt = db.prepareStatement(
			"SELECT judge_id"
			+" FROM judge"
			+" WHERE contest=?"
			+" AND judge_user=?"
			+" AND judge_password=SHA1(?)"
			);
		stmt.setString(1, req.getParameter("contest"));
		stmt.setString(2, req.getParameter("username"));
		stmt.setString(3, req.getParameter("password"));
		ResultSet rs = stmt.executeQuery();
		if (rs.next())
		{
			// found judge login
			HttpSession s = req.getSession(true);
			s.setAttribute("is_judge", rs.getString(1));
			s.removeAttribute("is_team");
			s.removeAttribute("is_director");
			s.removeAttribute("is_sysadmin");
			s.setAttribute("uid", req.getParameter("contest")+"/"+req.getParameter("username"));
			s.setAttribute("username", req.getParameter("username"));
			return true;
		}
		return false;
	}

	private boolean checkTeamLogin(HttpServletRequest req,
			HttpServletResponse resp, Connection db)
		throws Exception
	{
		PreparedStatement stmt = db.prepareStatement(
			"SELECT team_number"
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
			s.setAttribute("is_team", rs.getString(1));
			s.removeAttribute("is_judge");
			s.removeAttribute("is_director");
			s.removeAttribute("is_sysadmin");
			s.setAttribute("uid", req.getParameter("contest")+"/"+req.getParameter("username"));
			s.setAttribute("username", req.getParameter("username"));
			return true;
		}
		return false;
	}

	public void doPost(HttpServletRequest req, HttpServletResponse resp)
		throws IOException, ServletException
	{
		try {

		Connection db = Database.getConnection();
		try
		{
			if(
			checkTeamLogin(req, resp, db)
			||
			checkJudgeLogin(req, resp, db)
			||
			checkDirectorLogin(req, resp, db)
			)
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
		finally
		{
			db.close();
		}

		}
		catch (Exception e)
		{
			throw new ServletException(e);
		}
	}
}
