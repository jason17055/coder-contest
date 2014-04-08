package dragonfin.contest.model;

import java.util.Date;

public class ProblemInfo
{
	public String contestId;
	public String id;
	public String name;
	public int ordinal;
	public File spec;
	public File solution;
	public int difficulty;
	public int incorrect_submissions;
	public String opened;
	public String scoreboard_image;
	public boolean allow_submissions;
	public boolean visible;
	public boolean score_by_access_time;
	public String judged_by;
	public int system_test_count;
	public String edit_url;
	public int allocated_minutes;
	public int runtime_limit;
	public Date start_time;

	public String getOpened()
	{
		return opened;
	}

	public int getDifficulty()
	{
		return difficulty;
	}

	public String getName()
	{
		return name;
	}
}
