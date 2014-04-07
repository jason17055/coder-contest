package dragonfin.contest.model;

public class ProblemInfo
{
	public String contestId;
	public String id;
	public String name;
	public String specFile;
	public String specName;
	public int difficulty;
	public int solutionTime;
	public int incorrectSubmissions;
	public String opened;

	public int getSolution_time()
	{
		return solutionTime;
	}

	public int getIncorrect_submissions()
	{
		return incorrectSubmissions;
	}

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
