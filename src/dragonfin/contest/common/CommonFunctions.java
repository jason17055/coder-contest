package dragonfin.contest.common;

import java.io.*;

public class CommonFunctions
{
	public static String escapeUrl(String inStr)
	{
		try
		{
		return java.net.URLEncoder.encode(inStr, "UTF-8");
		}catch (UnsupportedEncodingException e)
		{
			throw new Error("unexpected: "+e.getMessage(), e);
		}
	}

	public static String fileExtensionOf(String fileName)
	{
		if (fileName == null) {
			return null;
		}
		int period = fileName.lastIndexOf('.');
		if (period == -1) {
			return null;
		}

		return fileName.substring(period+1);
	}
}
