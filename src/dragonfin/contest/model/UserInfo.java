package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class UserInfo implements java.io.Serializable
{
	String id;
	String name;
	String contestId;
	int ordinal;

	public String getId()
	{
		return id;
	}

	public String getName() { return name; }
	public String getContestId() { return contestId; }
	public int getOrdinal() { return ordinal; }

	public static UserInfo loadById(String id)
		throws SQLException, UserNotFound
	{
		Connection db = Database.getConnection();
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try
		{
			stmt = db.prepareStatement(
				"SELECT team_name,contest,ordinal"
				+" FROM team"
				+" WHERE team_number=?"
				);
			stmt.setString(1, id);
			rs = stmt.executeQuery();
			if (rs.next())
			{
				// found the user
				UserInfo rv = new UserInfo();
				rv.id = id;
				rv.name = rs.getString(1);
				rv.contestId = rs.getString(2);
				rv.ordinal = rs.getInt(3);
				return rv;
			}
			else
			{
				throw new UserNotFound(id);
			}
		}
		finally
		{
			if (rs != null) { rs.close(); }
			if (stmt != null) { stmt.close(); }
			db.close();
		}
	}

	public static class UserNotFound extends Exception
	{
		public UserNotFound(String id)
		{
			super("Invalid user: "+id);
		}
	}

}

