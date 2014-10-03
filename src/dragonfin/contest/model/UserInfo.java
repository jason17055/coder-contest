package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class UserInfo implements java.io.Serializable
{
	public String contest;
	public String username;

	public String name;
	public String display_name;
	public String description;
	public String contestId;
	public int ordinal;
	public boolean is_director;
	public boolean is_contestant;
	public boolean is_judge;
	public String edit_url;
	public boolean online;
	public String url;
	public boolean visible;
	public String score_html;
	public Map<String,ResultInfo> result_by_problem = new HashMap<String,ResultInfo>();

	public String getId()
	{
		return contest+"/"+username;
	}

	public String getName() { return name; }
	public String getContestId() { return contest; }
	public int getOrdinal() { return ordinal; }
	public boolean isDirector() { return is_director; }
	public boolean isJudge() { return is_judge; }
	public boolean isContestant() { return is_contestant; }
}

