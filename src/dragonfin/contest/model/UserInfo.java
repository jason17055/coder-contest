package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class UserInfo implements java.io.Serializable
{
	public String id;
	public String name;
	public String contestId;
	public int ordinal;
	public boolean is_director;
	public boolean is_contestant;
	public boolean is_judge;

	public String getId()
	{
		return id;
	}

	public String getName() { return name; }
	public String getContestId() { return contestId; }
	public int getOrdinal() { return ordinal; }
	public boolean getIsDirector() { return is_director; }
	public boolean getIsJudge() { return is_judge; }
	public boolean getIsContestant() { return is_contestant; }
}

