package dragonfin.contest.model;

public class ProblemInfo
{
	public String contestId;
	public String id;
	public String name;
	public int ordinal;
	public String spec_file;
	public String spec_name;
	public int difficulty;
	public int incorrect_submissions;
	public String opened;
	public String balloon_image;
	public boolean allow_submissions;
	public boolean visible;
	public String judged_by;
	public String solution_file;
	public int system_test_count;
	public String edit_url;
	public int allocated_minutes;
	public int runtime_limit;

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
