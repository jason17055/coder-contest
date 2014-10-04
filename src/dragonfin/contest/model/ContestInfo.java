package dragonfin.contest.model;

import java.sql.*;
import java.util.*;

public class ContestInfo implements java.io.Serializable
{
	public String id;
	public String title;
	public String created_by;
	public String url;
	public List<ProblemInfo> problems;

	public ContestInfo()
	{
	}

	public String getId()
	{
		return id;
	}

	public List<ProblemInfo> getProblems()
	{
		return problems;
	}

	public String getTitle()
	{
		return title;
	}

	public String getCurrent_phase_name()
	{
		return "CODING";
	}

	public String getCurrent_phase_timeleft()
	{
		return "foo";
	}

	public String [] getStatus_choices()
	{
		return new String[] {
			"Accepted",
			"Correct",
			"Wrong Answer",
			"Output Format Error",
			"Excessive Output",
			"Compilation Error",
			"Run-Time Error",
			"Time-Limit Exceeded"
			};
	}
}
