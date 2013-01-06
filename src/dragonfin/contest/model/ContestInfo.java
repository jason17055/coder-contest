package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class ContestInfo implements java.io.Serializable
{
	String id;
	String title;

	public ContestInfo()
	{
	}

	public String getId()
	{
		return id;
	}

	public List<ProblemInfo> getProblems()
		throws SQLException
	{
		ArrayList<ProblemInfo> result = new ArrayList<ProblemInfo>();
		Connection db = Database.getConnection();
		PreparedStatement stmt = null;
		try
		{
			stmt = db.prepareStatement(
				"SELECT p.problem_number,problem_name,spec_file,spec_name,difficulty"
				+" FROM problem p"
				+" WHERE contest=?"
				+" ORDER BY problem_number"
				);
			stmt.setString(1, this.getId());
			ResultSet rs = stmt.executeQuery();
			while (rs.next())
			{
				ProblemInfo p = new ProblemInfo();
				p.contestId = this.getId();
				p.number = rs.getInt(1);
				p.name = rs.getString(2);
				p.specFile = rs.getString(3);
				p.specName = rs.getString(4);
				p.difficulty = rs.getInt(5);
				result.add(p);
			}
			return result;
		}
		finally
		{
			if (stmt != null)
				stmt.close();
			db.close();
		}
	}

	public String getTitle()
	{
		return title;
	}

	public static ContestInfo load(String id)
		throws SQLException, ContestNotFound
	{
		Connection db = Database.getConnection();
		PreparedStatement stmt = null;
		try
		{
			stmt = db.prepareStatement(
				"SELECT contest_id,title"
				+" FROM contest"
				+" WHERE contest_id=?"
				);
			stmt.setString(1, id);
			ResultSet rs = stmt.executeQuery();
			if (rs.next())
			{
				// found contest info
				ContestInfo rv = new ContestInfo();
				rv.id = rs.getString(1);
				rv.title = rs.getString(2);
				return rv;
			}
			else
			{
				throw new ContestNotFound(id);
			}
		}
		finally
		{
			if (stmt != null)
				stmt.close();
			db.close();
		}
	}

	public static class ContestNotFound extends Exception
	{
		public ContestNotFound(String contestId)
		{
			super("Invalid contest: "+contestId);
		}
	}
}
