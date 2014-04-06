package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class UserInfo implements java.io.Serializable
{
	public String id;
	public String name;
	public String contestId;
	public int ordinal;
	public boolean isDirector;
	public boolean isContestant;
	public boolean isJudge;

	public String getId()
	{
		return id;
	}

	public String getName() { return name; }
	public String getContestId() { return contestId; }
	public int getOrdinal() { return ordinal; }
	public boolean getIsDirector() { return isDirector; }
	public boolean getIsJudge() { return isJudge; }
	public boolean getIsContestant() { return isContestant; }
}

