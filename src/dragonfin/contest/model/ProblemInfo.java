package dragonfin.contest.model;

public class ProblemInfo
{
	String contestId;
	int number;
	String name;
	String specFile;
	String specName;
	int difficulty;
	int solutionTime;
	int incorrectSubmissions;
	String opened;

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

	public int getNumber()
	{
		return number;
	}
}
