package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class UserInfo implements java.io.Serializable
{
	public String id;
	public String name;
	public String contestId;
	public int ordinal;

	public String getId()
	{
		return id;
	}

	public String getName() { return name; }
	public String getContestId() { return contestId; }
	public int getOrdinal() { return ordinal; }
}

