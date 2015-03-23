package dragonfin.contest.common;

import java.io.*;

public class CommonFunctions
{
	public static String byteArray2Hex(byte[] bytes)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bytes.length; i++) {
			sb.append(String.format("%02x", bytes[i]));
		}
		return sb.toString();
	}

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

	public static int compareUsernames(String u1, String u2)
	{
		if (u1 == null) { u1 = ""; }
		return u1.compareTo(u2);
	}
}
