package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class TemplateGlobals
{
	public List<ContestInfo> getContests()
		throws SQLException
	{
		Connection db = Database.getConnection();
		Statement stmt = null;
		try
		{
			stmt = db.createStatement();
			ResultSet rs = stmt.executeQuery(
				"SELECT contest_id,title FROM contest WHERE enabled='Y' ORDER BY contest_id"
				);
			ArrayList<ContestInfo> result = new ArrayList<ContestInfo>();
			while (rs.next())
			{
				ContestInfo c = new ContestInfo();
				c.id = rs.getString(1);
				c.title = rs.getString(2);
				result.add(c);
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
}
