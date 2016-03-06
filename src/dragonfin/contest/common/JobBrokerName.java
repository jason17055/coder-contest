package dragonfin.contest.common;

public class JobBrokerName
{
	private JobBrokerName() {} // prevent construction

	/// Default module instance id.
	public static final String DEFAULT = "0";

	public static String getJobBrokerName(String moduleInstanceId)
	{
		return moduleInstanceId;
	}
}
